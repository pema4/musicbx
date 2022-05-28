use crate::model::configuration::{DeviceConfiguration, SampleRateConfiguration};
use crate::nodes::Node;
use anyhow::anyhow;
use cpal::{
    traits::{DeviceTrait, HostTrait, StreamTrait},
    Device, OutputCallbackInfo, Sample, SampleFormat, SampleRate, Stream, StreamConfig,
    StreamError, SupportedStreamConfig,
};
use glicol_synth::{AudioContext, AudioContextBuilder};
use std::sync::{Arc, Mutex};

pub trait AppDelegate {
    fn add_node(&self, node: &mut dyn Node);
    fn connect_nodes(&self, from: &dyn Node, from_output: &str, to: &dyn Node, to_input: &str);
    fn set_parameter(&self, node: &dyn Node, param_idx: u8, param_value: f32);
    fn reset(&self);
    fn output_configuration(&self) -> DeviceConfiguration;
}

impl Default for Box<dyn AppDelegate> {
    fn default() -> Self {
        Box::new(NoopAppDelegate)
    }
}

#[derive(Default)]
pub struct NoopAppDelegate;

impl AppDelegate for NoopAppDelegate {
    fn add_node(&self, _: &mut dyn Node) {}
    fn connect_nodes(&self, _: &dyn Node, _: &str, _: &dyn Node, _: &str) {}
    fn set_parameter(&self, _: &dyn Node, _: u8, _: f32) {}
    fn reset(&self) {}
    fn output_configuration(&self) -> DeviceConfiguration {
        DeviceConfiguration {
            current: None,
            available: vec![],
            sample_rate: None,
        }
    }
}

pub struct CpalAppDelegate {
    context: Arc<Mutex<AudioContext<1>>>,
    _audio_stream: Mutex<Option<Stream>>,
    output_name: String,
    sample_rate: u32,
}

impl CpalAppDelegate {
    pub fn builder() -> CpalAppDelegateBuilder {
        CpalAppDelegateBuilder::default()
    }
}

pub struct CpalAppDelegateBuilder {
    output_name: Option<String>,
    sr: Option<SampleRate>,
    err_fn: Box<dyn FnMut(StreamError) + Send + 'static>,
}

impl Default for CpalAppDelegateBuilder {
    fn default() -> Self {
        Self {
            output_name: None,
            sr: None,
            err_fn: Box::new(|_| {}),
        }
    }
}

impl CpalAppDelegateBuilder {
    pub fn output_name(self, output_name: Option<&str>) -> Self {
        Self {
            output_name: output_name.map(ToOwned::to_owned),
            ..self
        }
    }

    #[allow(dead_code)]
    pub fn sr(self, sr: u32) -> Self {
        Self {
            sr: Some(SampleRate(sr as u32)),
            ..self
        }
    }

    pub fn on_error(self, callback: impl FnMut(StreamError) + Send + 'static) -> Self {
        Self {
            err_fn: Box::new(callback),
            ..self
        }
    }

    pub fn build(self) -> anyhow::Result<CpalAppDelegate> {
        let Self {
            output_name,
            sr,
            err_fn,
        } = self;

        let host = cpal::default_host();
        let device = host
            .output_devices()
            .unwrap()
            .find(|x| x.name().ok() == output_name)
            .unwrap_or_else(|| host.default_output_device().unwrap());
        let config = get_output_config(&device, sr)?;
        let sample_rate = config.sample_rate().0;

        let context: AudioContext<1> = AudioContextBuilder::new()
            .sr(config.sample_rate().0 as usize)
            .build();
        let context = Arc::new(Mutex::new(context));

        let stream = match config.sample_format() {
            SampleFormat::F32 => {
                start_audio_stream::<f32, 1>(context.clone(), &device, &config.into(), err_fn)
            }
            SampleFormat::I16 => {
                start_audio_stream::<i16, 1>(context.clone(), &device, &config.into(), err_fn)
            }
            SampleFormat::U16 => {
                start_audio_stream::<u16, 1>(context.clone(), &device, &config.into(), err_fn)
            }
        }?;
        stream.play()?;

        Ok(CpalAppDelegate {
            context,
            _audio_stream: Mutex::new(Some(stream)),
            output_name: device.name()?,
            sample_rate,
        })
    }
}

fn get_output_config(
    device: &Device,
    sr: Option<SampleRate>,
) -> anyhow::Result<SupportedStreamConfig> {
    let config = if let Some(sr) = sr {
        device
            .supported_output_configs()?
            .map(|cfg| {
                if cfg.min_sample_rate() <= sr && sr <= cfg.max_sample_rate() {
                    cfg.with_sample_rate(sr)
                } else {
                    cfg.with_max_sample_rate()
                }
            })
            .next()
            .or_else(|| device.default_output_config().ok())
            .ok_or_else(|| {
                anyhow!(
                    "Can't find config for device {:?} with sample rate {sr:?}",
                    device.name()
                )
            })?
    } else {
        device.default_output_config()?
    };

    Ok(config)
}

fn start_audio_stream<T: Sample, const N: usize>(
    context: Arc<Mutex<AudioContext<N>>>,
    device: &Device,
    config: &StreamConfig,
    mut err_fn: Box<dyn FnMut(StreamError) + Send + 'static>,
) -> Result<Stream, anyhow::Error> {
    let channels = config.channels as usize;

    let err_fn = move |err: StreamError| {
        eprintln!("an error occurred on stream: {:?}", err);
        err_fn(err);
    };

    let stream = device.build_output_stream(
        config,
        move |data: &mut [T], _: &OutputCallbackInfo| {
            let context = &mut context.lock().unwrap();

            for (_sample_idx, frame) in data.chunks_mut(channels).enumerate() {
                let block = context.next_block();

                for (channel_idx, sample) in frame.iter_mut().enumerate() {
                    let s = &block[channel_idx][0];
                    *sample = Sample::from::<f32>(s);
                }
            }
        },
        err_fn,
    )?;

    Ok(stream)
}

impl AppDelegate for CpalAppDelegate {
    fn add_node(&self, node: &mut dyn Node) {
        let context = &mut self.context.lock().unwrap();
        node.add_to_context(context);
    }

    fn connect_nodes(&self, from: &dyn Node, from_output: &str, to: &dyn Node, to_input: &str) {
        let from = from.output(from_output).unwrap();
        let (order, to) = to.input(to_input).unwrap();
        let context = &mut self.context.lock().unwrap();
        context.connect_with_order(from, to, order);
    }

    fn set_parameter(&self, node: &dyn Node, param_idx: u8, param_value: f32) {
        let context = &mut self.context.lock().unwrap();
        node.set_parameter(context, param_idx, param_value);
    }

    fn reset(&self) {
        let context = &mut self.context.lock().unwrap();
        context.reset();
    }

    fn output_configuration(&self) -> DeviceConfiguration {
        DeviceConfiguration {
            current: Some(self.output_name.to_owned()),
            available: vec![],
            sample_rate: Some(SampleRateConfiguration {
                current: self.sample_rate,
                available: vec![],
            }),
        }
    }
}

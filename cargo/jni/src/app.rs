use std::sync::mpsc::Sender;
use std::sync::{mpsc, Arc, Mutex};
use std::thread;

use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use cpal::Stream;
use glicol_synth::operator::Mul;
use glicol_synth::oscillator::SinOsc;
use glicol_synth::{AudioContext, AudioContextBuilder};
use lazy_static::lazy_static;
use rand::{distributions, Rng};

use crate::modules::{Module, ModuleDescription, ModuleInfo, OutputModule, SinModule};
use crate::util::Observable;

pub struct App {
    state: Arc<Mutex<AppState>>,
    message_sender: Mutex<Sender<AppMsg>>,
}

impl App {
    pub fn current() -> &'static App {
        &APP
    }

    pub fn new() -> App {
        let (sender, receiver) = mpsc::channel();
        let state = AppState::new(44100).unwrap();

        let app = App {
            message_sender: Mutex::new(sender),
            state: Arc::new(Mutex::new(state)),
        };

        thread::spawn({
            let state = app.state.clone();
            move || {
                // блокировка берётся только один раз
                let state = &mut state.lock().unwrap();
                for msg in receiver {
                    state.update(&msg);
                }
            }
        });

        app
    }

    pub fn accept_message(&self, msg: AppMsg) {
        let sender = self.message_sender.lock().unwrap();
        sender.send(msg).unwrap();
    }
}

impl Default for App {
    fn default() -> Self {
        App::new()
    }
}

lazy_static! {
    static ref APP: App = App::new();
}

struct AppState {
    _audio_stream: Mutex<Stream>,
    context: Arc<Mutex<AudioContext<1>>>,
    // контекст используется и модифицируется в разных потоках
    available_modules: Observable<Vec<Box<dyn ModuleDescription>>>,
    modules: Vec<Box<dyn Module>>,
    sample_rate: usize,
}

// внутри _audio_stream лежит сырой указатель, для которого нет типажа Send
// при этом мы обещаем, что с этим указателем будет работать только один поток
unsafe impl Send for AppState {}

pub enum AppMsg {
    Start,
    Stop,
    AddModule {
        uid: String,
        id: usize,
    },
    RemoveModule {
        id: usize,
    },
    ConnectModules {
        from: (usize, usize),
        to: (usize, usize),
    },
    DisconnectModules {
        from: (usize, usize),
        to: (usize, usize),
    },
    ChangeConfiguration,
    RegisterAvailableModulesListener(Arc<dyn Fn(&[ModuleInfo])>),
}

unsafe impl Send for AppMsg {}

unsafe impl Sync for AppMsg {}

impl AppState {
    fn new(sr: usize) -> Result<AppState, anyhow::Error> {
        let context: AudioContext<1> = AudioContextBuilder::new().sr(sr).build();

        let context = Arc::new(Mutex::new(context));

        let host = cpal::default_host();
        let device = host.default_output_device().unwrap();
        let config = device.default_output_config().unwrap();

        let stream = match config.sample_format() {
            cpal::SampleFormat::F32 => {
                start_audio_stream::<f32, 1>(context.clone(), &device, &config.into())
            }
            cpal::SampleFormat::I16 => {
                start_audio_stream::<i16, 1>(context.clone(), &device, &config.into())
            }
            cpal::SampleFormat::U16 => {
                start_audio_stream::<u16, 1>(context.clone(), &device, &config.into())
            }
        }?;
        stream.play()?;

        let result = AppState {
            _audio_stream: Mutex::new(stream),
            context,
            available_modules: Observable::new(available_modules()),
            modules: vec![],
            sample_rate: sr,
        };

        Ok(result)
    }

    fn update(&mut self, msg: &AppMsg) {
        match msg {
            AppMsg::Start => self.start(),
            AppMsg::Stop => self.stop(),
            AppMsg::AddModule { uid, id } => self.add_module(uid, *id),
            AppMsg::RemoveModule { id } => self.remove_module(*id),
            AppMsg::ConnectModules { .. } => {}
            AppMsg::DisconnectModules { .. } => {}
            AppMsg::ChangeConfiguration => {}
            AppMsg::RegisterAvailableModulesListener(listener) => {
                self.add_available_modules_listener(listener.clone())
            }
        }
    }

    fn stop(&mut self) {
        println!("Stop!")
    }

    fn start(&mut self) {
        println!("Start!")
    }

    fn add_module(&mut self, uid: &str, id: usize) {
        let module_desc = self.available_modules.data.iter().find(|x| x.uid() == uid);

        if let Some(module_desc) = module_desc {
            let module = module_desc.create_instance(id);
            self.modules.push(module);
            self.recreate_context();
        } else {
            eprintln!("Node description with uid {uid} not found");
        }
    }

    fn remove_module(&mut self, id: usize) {
        self.modules.retain(|x| x.id() != Some(id));
        self.recreate_context();

        // if let Some(module) = module {
        //     let context = &mut self.context.lock().unwrap();
        //     module.remove_from_context(context);
        //     println!("Removed Node with id {id}");
        // } else {
        //     eprintln!("Node with id {id} not found");
        // }
    }

    pub fn add_available_modules_listener(&mut self, listener: Arc<dyn Fn(&[ModuleInfo])>) {
        let listener = move |module_descriptions: &Vec<Box<dyn ModuleDescription>>| {
            let module_infos: Vec<_> = module_descriptions.iter().map(|x| x.get_info()).collect();
            listener(&module_infos);
        };
        let listener = Arc::new(listener);

        self.available_modules.add_listener(listener);
    }

    fn recreate_context(&mut self) {
        let context = &mut self.context.lock().unwrap();
        context.reset();

        for module in &mut self.modules {
            module.add_to_context(context)
        }
    }
}

fn start_audio_stream<T: cpal::Sample, const N: usize>(
    context: Arc<Mutex<AudioContext<N>>>,
    device: &cpal::Device,
    config: &cpal::StreamConfig,
) -> Result<cpal::Stream, anyhow::Error> {
    let channels = config.channels as usize;

    let err_fn = |err| eprintln!("an error occurred on stream: {}", err);

    let stream = device.build_output_stream(
        config,
        move |data: &mut [T], _: &cpal::OutputCallbackInfo| {
            let context = &mut context.lock().unwrap();

            for (_sample_idx, frame) in data.chunks_mut(channels).enumerate() {
                let block = context.next_block();

                for (channel_idx, sample) in frame.iter_mut().enumerate() {
                    let s = &block[channel_idx][0];
                    *sample = cpal::Sample::from::<f32>(s);
                }
            }
        },
        err_fn,
    )?;

    Ok(stream)
}

fn available_modules() -> Vec<Box<dyn ModuleDescription>> {
    vec![
        Box::new(SinModule::default()),
        Box::new(OutputModule::default()),
    ]
}

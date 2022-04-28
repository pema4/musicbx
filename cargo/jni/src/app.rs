use std::collections::HashMap;
use std::sync::{Arc, mpsc, Mutex};
use std::sync::mpsc::Sender;
use std::thread;

use cpal::Stream;
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use glicol_synth::{AudioContext, AudioContextBuilder, Message};
use lazy_static::lazy_static;

use crate::modules::{
    AmpModule, Module, ModuleDescription, ModuleInfo, MulModule, OutputModule, SinModule,
};
use crate::patch::Cable;
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
        sender.send(msg).unwrap()
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
    // контекст используется и модифицируется в разных потоках, поэтому мьютекс
    context: Arc<Mutex<AudioContext<1>>>,
    _audio_stream: Mutex<Stream>,
    available_modules: Observable<Vec<Box<dyn ModuleDescription>>>,
    modules: HashMap<usize, Box<dyn Module>>,
    cables: Vec<Cable>,
    parameters: HashMap<(usize, u8), f32>,
}

// внутри _audio_stream лежит сырой указатель, для которого нет типажа Send
// при этом мы обещаем, что с этим указателем будет работать только один поток
unsafe impl Send for AppState {}

pub enum AppMsg {
    Reset,
    AddModule { uid: String, id: usize },
    RemoveModule { id: usize },
    AddCable(Cable),
    RemoveCable(Cable),
    ChangeConfiguration { output: String },
    RegisterAvailableModulesListener(Arc<dyn Fn(&[ModuleInfo])>),
    SetParameter { id: usize, index: u8, value: f32 },
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
            modules: HashMap::new(),
            cables: vec![],
            parameters: HashMap::new(),
        };

        Ok(result)
    }

    fn update(&mut self, msg: &AppMsg) {
        match msg {
            AppMsg::Reset => self.reset(),
            AppMsg::AddModule { uid, id } => self.add_module(uid, *id),
            AppMsg::RemoveModule { id } => self.remove_module(*id),
            AppMsg::AddCable(cable) => self.add_cable(cable),
            AppMsg::RemoveCable(cable) => self.remove_cable(cable),
            AppMsg::ChangeConfiguration { .. } => {}
            AppMsg::RegisterAvailableModulesListener(listener) => {
                self.add_available_modules_listener(listener.clone())
            }
            AppMsg::SetParameter { id, index, value } => self.set_parameter(*id, *index, *value),
        }
    }

    fn reset(&mut self) {
        self.modules.clear();
        self.cables.clear();
        self.parameters.clear();
        self.recreate_context();
    }

    fn add_module(&mut self, uid: &str, id: usize) {
        let module_desc = self.available_modules.data.iter().find(|x| x.uid() == uid);

        if let Some(module_desc) = module_desc {
            let mut module = module_desc.create_instance(id);
            let context = &mut self.context.lock().unwrap();
            module.add_to_context(context);
            self.modules.insert(id, module);
        } else {
            eprintln!("Node description with uid {uid} not found");
        }
    }

    fn remove_module(&mut self, id: usize) {
        self.modules.remove(&id);
        self.cables
            .retain(|x| x.from.module != id && x.to.module != id);
        self.recreate_context();
    }

    fn add_cable(&mut self, cable: &Cable) {
        self.cables.push(*cable);
        let context = &mut self.context.lock().unwrap();
        self.do_connect_modules(context, cable);
    }

    fn remove_cable(&mut self, cable: &Cable) {
        println!("Here: {cable:?}");
        self.cables.retain(|x| x != cable);
        self.recreate_context();
    }

    pub fn add_available_modules_listener(&mut self, listener: Arc<dyn Fn(&[ModuleInfo])>) {
        let listener = move |module_descriptions: &Vec<Box<dyn ModuleDescription>>| {
            let module_infos: Vec<_> = module_descriptions.iter().map(|x| x.info()).collect();
            listener(&module_infos);
        };
        let listener = Arc::new(listener);

        self.available_modules.add_listener(listener);
    }

    fn recreate_context(&mut self) {
        let context = &mut self.context.lock().unwrap();
        context.reset();

        for module in &mut self.modules.values_mut() {
            module.add_to_context(context)
        }

        for cable in &self.cables {
            self.do_connect_modules(context, cable);
        }

        for ((id, index), value) in &self.parameters {
            eprintln!("id: {id}, index: {index}, value: {value}");
            let module = self.modules.get(id);
            if let Some(module) = module {
                module.set_parameter(context, *index, *value);
            }
        }
    }

    fn do_connect_modules(&self, context: &mut AudioContext<1>, cable: &Cable) {
        let from = self.modules[&cable.from.module]
            .output(cable.from.socket)
            .unwrap();
        let (order, to) = self.modules[&cable.to.module]
            .input(cable.to.socket)
            .unwrap();

        println!("order: {order}, to: {}", to.index());
        context.connect_with_order(from, to, order);
        println!("Connecting {} with {}", from.index(), to.index());
    }

    fn set_parameter(&mut self, id: usize, index: u8, value: f32) {
        let module = self.modules.get(&id).unwrap();
        let context = &mut self.context.lock().unwrap();

        self.parameters.insert((id, index), value);

        module.set_parameter(context, index, value);
    }
}

fn start_audio_stream<T: cpal::Sample, const N: usize>(
    context: Arc<Mutex<AudioContext<N>>>,
    device: &cpal::Device,
    config: &cpal::StreamConfig,
) -> Result<cpal::Stream, anyhow::Error> {
    let channels = config.channels as usize;

    let err_fn = |err| {
        eprintln!("an error occurred on stream: {}", err);
    };

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
        Box::new(AmpModule::default()),
        Box::new(MulModule::default()),
    ]
}

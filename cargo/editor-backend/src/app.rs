use std::collections::HashMap;
use std::sync::{Arc, mpsc, Mutex};
use std::sync::mpsc::Sender;
use std::thread;

use cpal::{Device, OutputCallbackInfo, Sample, SampleFormat, Stream, StreamConfig};
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use glicol_synth::{AudioContext, AudioContextBuilder};
use lazy_static::lazy_static;

use musicbx::types::patch::Cable;

use crate::nodes::{
    AmpNodeDescription, MulNodeDescription, Node, NodeFactory, NodeInfo, NoiseNodeDescription,
    OutputNodeDescription, SimpleSawNodeDescription, SinNodeDescription, TestFmNodeDescription,
};
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
    available_nodes: Observable<Vec<Box<dyn NodeFactory>>>,
    nodes: HashMap<usize, Box<dyn Node>>,
    cables: Vec<Cable>,
    parameters: HashMap<(usize, u8), f32>,
}

// внутри _audio_stream лежит сырой указатель, для которого нет типажа Send
// при этом мы обещаем, что с этим указателем будет работать только один поток
unsafe impl Send for AppState {}

pub enum AppMsg {
    Reset,
    AddNode { uid: String, id: usize },
    RemoveNode { id: usize },
    AddCable(Cable),
    RemoveCable(Cable),
    ChangeConfiguration { output: String },
    RegisterAvailableNodesListener(Arc<dyn Fn(&[&NodeInfo])>),
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
            SampleFormat::F32 => {
                start_audio_stream::<f32, 1>(context.clone(), &device, &config.into())
            }
            SampleFormat::I16 => {
                start_audio_stream::<i16, 1>(context.clone(), &device, &config.into())
            }
            SampleFormat::U16 => {
                start_audio_stream::<u16, 1>(context.clone(), &device, &config.into())
            }
        }?;
        stream.play()?;

        let result = AppState {
            _audio_stream: Mutex::new(stream),
            context,
            available_nodes: Observable::new(available_nodes()),
            nodes: HashMap::new(),
            cables: vec![],
            parameters: HashMap::new(),
        };

        Ok(result)
    }

    fn update(&mut self, msg: &AppMsg) {
        match msg {
            AppMsg::Reset => self.reset(),
            AppMsg::AddNode { uid, id } => self.add_node(uid, *id),
            AppMsg::RemoveNode { id } => self.remove_node(*id),
            AppMsg::AddCable(cable) => self.add_cable(cable),
            AppMsg::RemoveCable(cable) => self.remove_cable(cable),
            AppMsg::ChangeConfiguration { .. } => {}
            AppMsg::RegisterAvailableNodesListener(listener) => {
                self.add_available_nodes_listener(listener.clone())
            }
            AppMsg::SetParameter { id, index, value } => self.set_parameter(*id, *index, *value),
        }
    }

    fn reset(&mut self) {
        self.nodes.clear();
        self.cables.clear();
        self.parameters.clear();
        self.recreate_context();
    }

    fn add_node(&mut self, uid: &str, id: usize) {
        let node_desc = self.available_nodes.data.iter().find(|x| x.uid() == uid);

        if let Some(node_desc) = node_desc {
            let mut node = node_desc.create_instance(id);
            let context = &mut self.context.lock().unwrap();
            node.add_to_context(context);
            self.nodes.insert(id, node);
        } else {
            eprintln!("Node description with uid {uid} not found");
        }
    }

    fn remove_node(&mut self, id: usize) {
        self.nodes.remove(&id);
        self.cables
            .retain(|x| x.from.node_id != id && x.to.node_id != id);
        self.recreate_context();
    }

    fn add_cable(&mut self, cable: &Cable) {
        self.cables.push(cable.clone());
        let context = &mut self.context.lock().unwrap();
        self.do_connect_nodes(context, cable);
    }

    fn remove_cable(&mut self, cable: &Cable) {
        self.cables.retain(|x| x != cable);
        self.recreate_context();
    }

    pub fn add_available_nodes_listener(&mut self, listener: Arc<dyn Fn(&[&NodeInfo])>) {
        let listener = move |node_descriptions: &Vec<Box<dyn NodeFactory>>| {
            let node_infos: Vec<_> = node_descriptions.iter().map(|x| x.info()).collect();
            listener(&node_infos);
        };
        let listener = Arc::new(listener);

        self.available_nodes.add_listener(listener);
    }

    fn recreate_context(&mut self) {
        let context = &mut self.context.lock().unwrap();
        context.reset();

        for node in &mut self.nodes.values_mut() {
            node.add_to_context(context)
        }

        for cable in &self.cables {
            self.do_connect_nodes(context, cable);
        }

        for ((id, index), value) in &self.parameters {
            let node = self.nodes.get(id);
            if let Some(node) = node {
                node.set_parameter(context, *index, *value);
            }
        }
    }

    fn do_connect_nodes(&self, context: &mut AudioContext<1>, cable: &Cable) {
        let from = self.nodes[&cable.from.node_id]
            .output(&cable.from.socket_name)
            .unwrap();
        let (order, to) = self.nodes[&cable.to.node_id]
            .input(&cable.to.socket_name)
            .unwrap();

        context.connect_with_order(from, to, order);
        println!("Connecting {} with {}", from.index(), to.index());
    }

    fn set_parameter(&mut self, id: usize, index: u8, value: f32) {
        let node = self.nodes.get(&id).unwrap();
        let context = &mut self.context.lock().unwrap();

        self.parameters.insert((id, index), value);

        node.set_parameter(context, index, value);
    }
}

fn start_audio_stream<T: Sample, const N: usize>(
    context: Arc<Mutex<AudioContext<N>>>,
    device: &Device,
    config: &StreamConfig,
) -> Result<Stream, anyhow::Error> {
    let channels = config.channels as usize;

    let err_fn = |err| {
        eprintln!("an error occurred on stream: {}", err);
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

fn available_nodes() -> Vec<Box<dyn NodeFactory>> {
    vec![
        Box::new(SinNodeDescription),
        Box::new(SimpleSawNodeDescription),
        Box::new(OutputNodeDescription),
        Box::new(AmpNodeDescription),
        Box::new(MulNodeDescription),
        Box::new(NoiseNodeDescription),
        Box::new(TestFmNodeDescription),
    ]
}

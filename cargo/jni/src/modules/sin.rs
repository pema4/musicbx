use glicol_synth::{AudioContext, BoxedNodeSend, Buffer, Input, Message, Node, NodeData};
use hashbrown::HashMap;
use petgraph::graph::NodeIndex;

use crate::modules::{
    Module, ModuleDescription, ModuleInfo, ModuleInput, ModuleOutput, ModuleParameter,
    ModuleParameterKind,
};

#[derive(Default)]
pub struct SinModule;

impl ModuleDescription for SinModule {
    fn uid(&self) -> &'static str {
        "std.v1.sin"
    }

    fn info(&self) -> ModuleInfo {
        ModuleInfo {
            uid: self.uid().to_string(),
            name: "Sin Osc".to_string(),
            description: "The sine oscillator with customizable frequency".to_string(),
            inputs: vec![
                ModuleInput {
                    number: 0,
                    name: "phase".to_string(),
                    description: "Phase modulation of the oscillator".to_string(),
                },
                ModuleInput {
                    number: 1,
                    name: "freq".to_string(),
                    description: "Freq modulation of the oscillator".to_string(),
                },
            ],
            outputs: vec![ModuleOutput {
                number: 0,
                name: "out".to_string(),
                description: "Mono output of the oscillator".to_string(),
            }],
            parameters: vec![ModuleParameter {
                number: 0,
                kind: ModuleParameterKind::HzFast,
                default: "440.0".to_string(),
                name: "Freq".to_string(),
                description: "Frequency".to_string(),
            }],
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Module> {
        Box::new(SinNode {
            id,
            ..SinNode::default()
        })
    }
}

#[derive(Default)]
pub struct SinNode {
    id: usize,
    sin_index: Option<NodeIndex>,
}

impl Module for SinNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, pos: usize) -> Option<(usize, NodeIndex)> {
        match pos {
            0 => self.sin_index.map(|x| (0, x)),
            1 => self.sin_index.map(|x| (1, x)),
            _ => None,
        }
    }

    fn output(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 => self.sin_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let sin = SinOsc::new().sr(context.sample_rate()).freq(440.0);
        let sin: NodeIndex = context.add_mono_node(sin);

        self.sin_index = Some(sin);
    }

    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32) {
        if index == 0 {
            let freq = ModuleParameterKind::HzFast.denormalize(value);
            context.send_msg(self.sin_index.unwrap(), Message::SetToNumber(index, freq));
        }
    }
}

#[derive(Debug, Clone)]
struct SinOsc {
    pub freq: f32,
    pub phase: f32,
    pub sr: usize,
    input_order: HashMap<usize, usize>,
}

impl Default for SinOsc {
    fn default() -> Self {
        Self {
            freq: 1.0,
            phase: 0.0,
            sr: 44100,
            input_order: HashMap::new(),
        }
    }
}

#[allow(dead_code)]
impl SinOsc {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn freq(self, freq: f32) -> Self {
        Self { freq, ..self }
    }
    pub fn sr(self, sr: usize) -> Self {
        Self { sr, ..self }
    }
    pub fn phase(self, phase: f32) -> Self {
        Self { phase, ..self }
    }

    pub fn to_boxed_nodedata<const N: usize>(
        &self,
        channels: usize,
    ) -> NodeData<BoxedNodeSend<N>, N> {
        NodeData::multi_chan_node(channels, BoxedNodeSend::<N>::new(self.clone()))
    }
}

impl<const N: usize> Node<N> for SinOsc {
    fn process(
        &mut self,
        inputs: &mut hashbrown::HashMap<usize, Input<N>>,
        output: &mut [Buffer<N>],
    ) {
        if inputs.is_empty() {
            self.generate_plain_sine(output);
        } else {
            self.generate_modulated_sine(inputs, output)
        }
    }

    fn send_msg(&mut self, info: Message) {
        match info {
            Message::SetToNumber(pos, value) => {
                if pos == 0 {
                    self.freq = value
                }
            }
            Message::IndexOrder(pos, index) => {
                self.input_order.insert(pos, index);
            }
            Message::ResetOrder => self.input_order.clear(),
            _ => {}
        }
    }
}

impl SinOsc {
    fn generate_plain_sine<const N: usize>(&mut self, output: &mut [Buffer<{ N }>]) {
        for i in 0..N {
            output[0][i] = (self.phase * 2.0 * std::f32::consts::PI).sin();
            self.phase += self.freq / self.sr as f32;
            if self.phase > 1.0 {
                self.phase %= 1.0
            }
        }
    }

    fn generate_modulated_sine<const N: usize>(
        &mut self,
        inputs: &mut HashMap<usize, Input<{ N }>>,
        output: &mut [Buffer<{ N }>],
    ) {
        let phases = self
            .input_order
            .get(&0)
            .and_then(|idx| inputs.get(idx))
            .map(|x| x.buffers());

        let freqs = self
            .input_order
            .get(&1)
            .and_then(|idx| inputs.get(idx))
            .map(|x| x.buffers());

        for i in 0..N {
            let freq = {
                let mut x = self.freq;
                if let Some(freqs) = freqs {
                    x += freqs[0][i];
                }
                x
            };

            self.phase += freq / (self.sr as f32);
            if let Some(phases) = phases {
                self.phase += phases[0][i];
            }

            if self.phase > 1.0 {
                self.phase %= 1.0
            }

            output[0][i] = (self.phase * 2.0 * std::f32::consts::PI).sin();
        }
    }
}

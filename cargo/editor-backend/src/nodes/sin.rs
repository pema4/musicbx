use std::ops::{Deref, DerefMut};

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::graph::NodeIndex;

use musicbx::std::osc::{SinOsc, SinOscParameters};
use musicbx::{DataMut, DataRef, FromSampleRate};

use crate::nodes::{Description, Node, NodeDescription, NodeFactory, NodeInfo, NodeParameterKind};

pub struct SinNodeDescription;

impl NodeFactory for SinNodeDescription {
    fn uid(&self) -> &'static str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn Node> {
        Box::new(SinOscNode {
            id,
            ..SinOscNode::default()
        })
    }
}

static INFO: NodeInfo = NodeInfo {
    definition: SinOsc::definition(),
    description: NodeDescription {
        node: Description::new("Sin", "The sine oscillator with customizable frequency"),
        inputs: &[
            Description::new("phase_mod", "Phase modulation of the oscillator"),
            Description::new("tune", "Tuning of the oscillator"),
        ],
        outputs: &[Description::new("output", "The output of the oscillator")],
        parameters: &[Description::new("freq", "Frequency")],
    },
};

#[derive(Default)]
pub struct SinOscNode {
    id: usize,
    sin_index: Option<NodeIndex>,
}

impl Node for SinOscNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, name: &str) -> Option<(usize, NodeIndex)> {
        match name {
            "phase_mod" => self.sin_index.map(|x| (0, x)),
            "tune" => self.sin_index.map(|x| (1, x)),
            _ => None,
        }
    }

    fn output(&self, name: &str) -> Option<NodeIndex> {
        match name {
            "output" => self.sin_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let sin = SinOscNodeImpl {
            inner: SinOsc::from_sample_rate(context.sample_rate() as f32),
            freq: 2000.0,
            input_order: HashMap::default(),
        };
        let sin: NodeIndex = context.add_mono_node(sin);

        self.sin_index = Some(sin);
    }

    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32) {
        if index == 0 {
            let freq = NodeParameterKind::HzWide.denormalize(value);
            context.send_msg(self.sin_index.unwrap(), Message::SetToNumber(index, freq));
        }
    }
}

#[derive(Debug, Clone)]
struct SinOscNodeImpl {
    inner: SinOsc,
    freq: f32,
    input_order: HashMap<usize, usize>,
}

impl<const N: usize> glicol_synth::Node<N> for SinOscNodeImpl {
    fn process(&mut self, inputs: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        // println!("inputs.size: {}", inputs.len());
        self.inner.process::<N>(
            N,
            SinOscParameters {
                freq: self.freq.into(),
                phase_mod: self
                    .input_order
                    .get(&0)
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| 0.0.into()),
                tune: self
                    .input_order
                    .get(&1)
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| 1.0.into()),
                output: DataMut::from(output[0].deref_mut()),
            },
        )
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

use std::ops::{Deref, DerefMut};

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::graph::NodeIndex;

use musicbx::{DataMut, DataRef, FromSampleRate};
use musicbx::osc::{SimpleSawOsc, SimpleSawOscParameters};

use crate::nodes::{Description, Node, NodeDescription, NodeFactory, NodeInfo, NodeParameterKind};

pub struct SimpleSawNodeDescription;

impl NodeFactory for SimpleSawNodeDescription {
    fn uid(&self) -> &str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn Node> {
        Box::new(SimpleSawNode {
            id,
            ..SimpleSawNode::default()
        })
    }
}

static INFO: NodeInfo = NodeInfo {
    definition: SimpleSawOsc::definition(),
    description: NodeDescription {
        node: Description::new("Simple Saw", "Simple aliased sawtooth oscillator"),
        inputs: &[Description::new("tune", "The pitch modulation")],
        outputs: &[Description::new("output", "The output of the oscillator")],
        parameters: &[Description::new("freq", "Frequency")],
    },
};

#[derive(Default)]
pub struct SimpleSawNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl Node for SimpleSawNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, name: &str) -> Option<(usize, NodeIndex)> {
        match name {
            "tune" => self.node_index.map(|x| (0, x)),
            _ => None,
        }
    }

    fn output(&self, name: &str) -> Option<NodeIndex> {
        match name {
            "output" => self.node_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let sin = SimpleSawNodeImpl {
            inner: SimpleSawOsc::from_sample_rate(context.sample_rate() as f32),
            freq: 2000.0,
            input_order: HashMap::default(),
        };
        let sin: NodeIndex = context.add_mono_node(sin);

        self.node_index = Some(sin);
    }

    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32) {
        if index == 0 {
            let freq = NodeParameterKind::HzSlow.denormalize(value);
            context.send_msg(self.node_index.unwrap(), Message::SetToNumber(index, freq));
        }
    }
}

#[derive(Debug, Clone)]
struct SimpleSawNodeImpl {
    inner: SimpleSawOsc,
    freq: f32,
    input_order: HashMap<usize, usize>,
}

impl<const N: usize> glicol_synth::Node<N> for SimpleSawNodeImpl {
    fn process(&mut self, inputs: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        self.inner.process::<N>(
            N,
            SimpleSawOscParameters {
                freq: self.freq.into(),
                tune: self
                    .input_order
                    .get(&0)
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

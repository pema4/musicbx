use std::ops::{Deref, DerefMut};

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::prelude::NodeIndex;

use musicbx::std::util::{Mul, MulParameters};
use musicbx::{DataMut, DataRef, Node};

use crate::nodes::{
    Description, NodeDescription, NodeFactory, NodeInfo, NodeParameterKind, NodeWrapper,
};

pub struct MulNodeDescription;

impl NodeFactory for MulNodeDescription {
    fn uid(&self) -> &str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn NodeWrapper> {
        Box::new(MulNode {
            id,
            ..MulNode::default()
        })
    }
}

static INFO: NodeInfo = NodeInfo {
    definition: Mul::definition(),
    description: NodeDescription {
        node: Description::new("Mul", "Multiplies two signals"),
        inputs: &[
            Description::new("a", "The first signal to be multiplied"),
            Description::new("b", "The second signal to be multiplied"),
        ],
        outputs: &[Description::new("output", "The multiplied signal")],
        parameters: &[Description::new("b", "Magnitude")],
    },
};

#[derive(Default)]
pub struct MulNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl NodeWrapper for MulNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, name: &str) -> Option<(usize, NodeIndex)> {
        self.node_index.and_then(|x| match name {
            "a" => Some((0, x)),
            "b" => Some((1, x)),
            _ => None,
        })
    }

    fn output(&self, name: &str) -> Option<NodeIndex> {
        if name == "output" {
            self.node_index
        } else {
            None
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let node = MulNodeImpl::default();
        self.node_index = Some(context.add_mono_node(node));
    }

    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32) {
        if index == 0 {
            let freq = NodeParameterKind::Number.denormalize(value);
            context.send_msg(self.node_index.unwrap(), Message::SetToNumber(index, freq));
        }
    }
}

#[derive(Default, Debug, Clone)]
struct MulNodeImpl {
    inner: Mul,
    default_right: f32,
    input_order: HashMap<usize, usize>,
}

impl<const N: usize> glicol_synth::Node<N> for MulNodeImpl {
    fn process(&mut self, inputs: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        self.inner.process::<N>(
            N,
            MulParameters {
                a: self
                    .input_order
                    .get(&0)
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| 0.0.into()),
                b: self
                    .input_order
                    .get(&1)
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| self.default_right.into()),
                output: DataMut::from(output[0].deref_mut()),
            },
        )
    }

    fn send_msg(&mut self, info: Message) {
        match info {
            Message::SetToNumber(pos, value) => {
                if pos == 0 {
                    self.default_right = value;
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

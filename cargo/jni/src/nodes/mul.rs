use std::ops::{Deref, DerefMut};

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::prelude::NodeIndex;

use musicbx::util::Mul;
use musicbx::{DataMut, DataRef};

use crate::nodes::{
    Node, NodeDescription, NodeInfo, NodeInput, NodeOutput, NodeParameter, NodeParameterKind,
};

pub struct MulNodeDescription;

impl NodeDescription for MulNodeDescription {
    fn uid(&self) -> &'static str {
        "std.v1.mul"
    }

    fn info(&self) -> NodeInfo {
        NodeInfo {
            uid: self.uid().to_string(),
            name: "Mul".to_string(),
            summary: "Multiplies two signals".to_string(),
            inputs: vec![
                NodeInput {
                    number: 0,
                    name: "a".to_string(),
                    description: "The first signal to be multiplied".to_string(),
                },
                NodeInput {
                    number: 1,
                    name: "b".to_string(),
                    description: "The second signal to be multiplied".to_string(),
                },
            ],
            outputs: vec![NodeOutput {
                number: 0,
                name: "out".to_string(),
                description: "The multiplied signal".to_string(),
            }],
            parameters: vec![NodeParameter {
                number: 0,
                kind: NodeParameterKind::Number,
                default: "1.0".to_string(),
                name: "Amp".to_string(),
                description: "Magnitude".to_string(),
            }],
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Node> {
        Box::new(MulNode {
            id,
            ..MulNode::default()
        })
    }
}

#[derive(Default)]
pub struct MulNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl Node for MulNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, pos: usize) -> Option<(usize, NodeIndex)> {
        self.node_index.and_then(|x| match pos {
            0 => Some((0, x)),
            1 => Some((1, x)),
            _ => None,
        })
    }

    fn output(&self, _: usize) -> Option<NodeIndex> {
        self.node_index
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
            musicbx::util::MulParameters {
                left: self
                    .input_order
                    .get(&0)
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| 0.0.into()),
                right: self
                    .input_order
                    .get(&1)
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| self.default_right.into()),
                out: DataMut::from(output[0].deref_mut()),
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

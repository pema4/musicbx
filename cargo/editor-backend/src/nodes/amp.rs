use std::ops::{Deref, DerefMut};

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::prelude::NodeIndex;

use musicbx::util::Amp;
use musicbx::{DataMut, DataRef, FromSampleRate};
use musicbx_types::parameter::NodeParameterKind;

use crate::nodes::{Description, Node, NodeDescription, NodeFactory, NodeInfo};

pub struct AmpNodeDescription;

static INFO: NodeInfo = NodeInfo {
    definition: Amp::definition(),
    description: NodeDescription {
        node: Description::new("Amp", "Amplifies the signal"),
        inputs: &[Description::new("input", "The input of the amplifier")],
        outputs: &[Description::new("output", "The amplified signal")],
        parameters: &[Description::new("db", "Amplitude in decibels")],
    },
};

impl NodeFactory for AmpNodeDescription {
    fn uid(&self) -> &str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn Node> {
        Box::new(AmpNode {
            id,
            ..AmpNode::default()
        })
    }
}

#[derive(Default)]
pub struct AmpNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl Node for AmpNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, name: &str) -> Option<(usize, NodeIndex)> {
        match name {
            "input" => self.node_index.map(|x| (0, x)),
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
        let amp = context.add_mono_node(AmpNodeImpl {
            db: -6.0,
            inner: Amp::from_sample_rate(context.sample_rate() as f32),
            input_index: None,
        });
        self.node_index = Some(amp);
    }

    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32) {
        if index == 0 {
            let db = NodeParameterKind::Db.denormalize(value);
            context.send_msg(self.node_index.unwrap(), Message::SetToNumber(index, db));
        }
    }
}

#[derive(Debug, Clone)]
struct AmpNodeImpl {
    inner: Amp,
    db: f32,
    input_index: Option<usize>,
}

impl<const N: usize> glicol_synth::Node<N> for AmpNodeImpl {
    fn process(&mut self, inputs: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        println!("db: {:?}", self.db);
        self.inner.process::<N>(
            N,
            musicbx::util::AmpParameters {
                input: self
                    .input_index
                    .as_ref()
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| 0.0.into()),
                db: self.db.into(),
                output: DataMut::from(output[0].deref_mut()),
            },
        )
    }

    fn send_msg(&mut self, info: Message) {
        match info {
            Message::SetToNumber(pos, value) => {
                if pos == 0 {
                    self.db = value
                }
            }
            Message::IndexOrder(pos, index) => {
                if pos == 0 {
                    self.input_index = Some(index);
                }
            }
            Message::ResetOrder => self.input_index = None,
            _ => {}
        }
    }
}

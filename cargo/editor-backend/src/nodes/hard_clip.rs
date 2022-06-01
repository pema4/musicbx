use std::ops::{Deref, DerefMut};

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::prelude::NodeIndex;

use musicbx::std::util::{HardClip, HardClipParameters};
use musicbx::{DataMut, DataRef, FromSampleRate};

use crate::nodes::{Description, Node, NodeDescription, NodeFactory, NodeInfo};

pub struct HardClipNodeDescription;

static INFO: NodeInfo = NodeInfo {
    definition: HardClip::definition(),
    description: NodeDescription {
        node: Description::new("Hard Clip", "Clips all signal below -1 or above 1"),
        inputs: &[Description::new("input", "The input of the clipper")],
        outputs: &[Description::new("output", "The amplified signal")],
        parameters: &[],
    },
};

impl NodeFactory for HardClipNodeDescription {
    fn uid(&self) -> &str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn Node> {
        Box::new(HardClipNode {
            id,
            ..HardClipNode::default()
        })
    }
}

#[derive(Default)]
pub struct HardClipNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl Node for HardClipNode {
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
        let amp = context.add_mono_node(HardClipNodeImpl {
            inner: HardClip::from_sample_rate(context.sample_rate() as f32),
            input_index: None,
        });
        self.node_index = Some(amp);
    }

    fn set_parameter(&self, _: &mut AudioContext<1>, _: u8, _: f32) {}
}

struct HardClipNodeImpl {
    inner: HardClip,
    input_index: Option<usize>,
}

impl<const N: usize> glicol_synth::Node<N> for HardClipNodeImpl {
    fn process(&mut self, inputs: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        self.inner.process::<N>(
            N,
            HardClipParameters {
                input: self
                    .input_index
                    .as_ref()
                    .and_then(|idx| inputs.get(idx))
                    .and_then(|x| x.buffers().get(0))
                    .map(|x| DataRef::from(x.deref()))
                    .unwrap_or_else(|| 0.0.into()),
                output: DataMut::from(output[0].deref_mut()),
            },
        )
    }

    fn send_msg(&mut self, info: Message) {
        match info {
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

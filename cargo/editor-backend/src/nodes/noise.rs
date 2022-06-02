use std::ops::DerefMut;

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::graph::NodeIndex;

use musicbx::std::util::{UniformRandom, UniformRandomParameters};
use musicbx::{DataMut, Node};

use crate::nodes::{Description, NodeDescription, NodeFactory, NodeInfo, NodeWrapper};

pub struct NoiseNodeDescription;

impl NodeFactory for NoiseNodeDescription {
    fn uid(&self) -> &str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn NodeWrapper> {
        Box::new(NoiseNode {
            id,
            ..NoiseNode::default()
        })
    }
}

static INFO: NodeInfo = NodeInfo {
    definition: UniformRandom::definition(),
    description: NodeDescription {
        node: Description::new("Noise", "Uniform noise generator (from -1.0 to 1.0)"),
        inputs: &[],
        outputs: &[Description::new("output", "Generated random signal")],
        parameters: &[],
    },
};

#[derive(Default)]
pub struct NoiseNode {
    id: usize,
    output_index: Option<NodeIndex>,
}

impl NodeWrapper for NoiseNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, _: &str) -> Option<(usize, NodeIndex)> {
        None
    }

    fn output(&self, name: &str) -> Option<NodeIndex> {
        match name {
            "output" => self.output_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let node = NoiseNodeImpl {
            inner: UniformRandom::default(),
        };
        self.output_index = Some(context.add_mono_node(node));
    }
}

#[derive(Debug, Clone)]
struct NoiseNodeImpl {
    inner: UniformRandom,
}

impl<const N: usize> glicol_synth::Node<N> for NoiseNodeImpl {
    fn process(&mut self, _: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        self.inner.process::<N>(
            N,
            UniformRandomParameters {
                output: DataMut::from(output[0].deref_mut()),
                ..UniformRandomParameters::default()
            },
        )
    }

    fn send_msg(&mut self, _: Message) {}
}

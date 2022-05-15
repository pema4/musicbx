use std::ops::DerefMut;

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::graph::NodeIndex;

use musicbx::DataMut;

use crate::nodes::{Node, NodeDescription, NodeInfo, NodeOutput};

pub struct NoiseNodeDescription;

impl NodeDescription for NoiseNodeDescription {
    fn uid(&self) -> &'static str {
        "std.osc.noise"
    }

    fn info(&self) -> NodeInfo {
        NodeInfo {
            uid: self.uid().to_string(),
            name: "Noise".to_string(),
            summary: "Uniform noise generator (from -1.0 to 1.0)".to_string(),
            inputs: vec![],
            outputs: vec![NodeOutput {
                number: 0,
                name: "out".to_string(),
                description: "Generated random signal".to_string(),
            }],
            parameters: vec![],
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Node> {
        Box::new(NoiseNode {
            id,
            ..NoiseNode::default()
        })
    }
}

#[derive(Default)]
pub struct NoiseNode {
    id: usize,
    output_index: Option<NodeIndex>,
}

impl Node for NoiseNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, _: usize) -> Option<(usize, NodeIndex)> {
        None
    }

    fn output(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 => self.output_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let node = NoiseNodeImpl {
            inner: musicbx::util::UniformRandom::default(),
        };
        self.output_index = Some(context.add_mono_node(node));
    }
}

#[derive(Debug, Clone)]
struct NoiseNodeImpl {
    inner: musicbx::util::UniformRandom,
}

impl<const N: usize> glicol_synth::Node<N> for NoiseNodeImpl {
    fn process(&mut self, _: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        self.inner.process::<N>(
            N,
            musicbx::util::UniformRandomParameters {
                out: DataMut::from(output[0].deref_mut()),
                ..musicbx::util::UniformRandomParameters::default()
            },
        )
    }

    fn send_msg(&mut self, _: Message) {}
}

use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;

use crate::nodes::{Node, NodeDescription, NodeInfo, NodeInput};

pub struct OutputNodeDescription;

impl NodeDescription for OutputNodeDescription {
    fn uid(&self) -> &'static str {
        "std.v1.output"
    }

    fn info(&self) -> NodeInfo {
        NodeInfo {
            uid: self.uid().to_string(),
            name: "Output".into(),
            summary: "The output node".into(),
            inputs: vec![NodeInput {
                number: 0,
                name: "Left".to_string(),
                description: "The left output".to_string(),
            }],
            ..NodeInfo::default()
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Node> {
        Box::new(OutputNode {
            id,
            node_index: None,
        })
    }
}

struct OutputNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl Node for OutputNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, pos: usize) -> Option<(usize, NodeIndex)> {
        match pos {
            0 => self.node_index.map(|x| (0, x)),
            _ => None,
        }
    }

    fn output(&self, _: usize) -> Option<NodeIndex> {
        None
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        self.node_index = Some(context.destination);
    }
}

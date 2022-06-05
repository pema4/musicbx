use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;

use musicbx::types::{NodeDefinition, NodeInput};

use crate::nodes::{Description, NodeDescription, NodeFactory, NodeInfo, NodeWrapper};

pub struct OutputNodeDescription;

impl NodeFactory for OutputNodeDescription {
    fn uid(&self) -> &str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn NodeWrapper> {
        Box::new(OutputNode {
            id,
            node_index: None,
        })
    }
}

static INFO: NodeInfo = NodeInfo {
    definition: NodeDefinition {
        uid: "_synthetic_output",
        inputs: &[NodeInput {
            number: 0,
            name: "input",
        }],
        outputs: &[],
        parameters: &[],
    },
    description: NodeDescription {
        node: Description::new("Output", "The output node"),
        inputs: &[Description::new("input", "The mono input")],
        outputs: &[],
        parameters: &[],
    },
};

struct OutputNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl NodeWrapper for OutputNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, name: &str) -> Option<(usize, NodeIndex)> {
        match name {
            "input" => self.node_index.map(|x| (0, x)),
            _ => None,
        }
    }

    fn output(&self, _: &str) -> Option<NodeIndex> {
        None
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        self.node_index = Some(context.destination);
    }
}

use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;

use crate::modules::{Module, ModuleDescription, ModuleInfo, ModuleInput};

#[derive(Default)]
pub struct OutputModule;

impl ModuleDescription for OutputModule {
    fn uid(&self) -> &'static str {
        "std.v1.output"
    }

    fn info(&self) -> ModuleInfo {
        ModuleInfo {
            uid: self.uid().to_string(),
            name: "Output Module".into(),
            description: "The output module".into(),
            inputs: vec![ModuleInput {
                number: 0,
                name: "Left".to_string(),
                description: "The left output".to_string(),
            }],
            ..ModuleInfo::default()
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Module> {
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

impl Module for OutputNode {
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

    fn set_parameter(&self, _: &mut AudioContext<1>, _: u8, _: f32) {}
}

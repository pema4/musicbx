use glicol_synth::{AudioContext, Pass};
use glicol_synth::operator::Mul;
use petgraph::prelude::NodeIndex;

use crate::modules::{Module, ModuleDescription, ModuleInfo, ModuleInput, ModuleOutput};

#[derive(Default)]
pub struct MulModule;

impl ModuleDescription for MulModule {
    fn uid(&self) -> &'static str {
        "std.v1.mul"
    }

    fn info(&self) -> ModuleInfo {
        ModuleInfo {
            uid: self.uid().to_string(),
            name: "Mul".to_string(),
            description: "Multiplies two signals".to_string(),
            inputs: vec![
                ModuleInput {
                    number: 0,
                    name: "a".to_string(),
                    description: "The first signal to be multiplied".to_string(),
                },
                ModuleInput {
                    number: 1,
                    name: "b".to_string(),
                    description: "The second signal to be multiplied".to_string(),
                },
            ],
            outputs: vec![ModuleOutput {
                number: 0,
                name: "out".to_string(),
                description: "The multiplied signal".to_string(),
            }],
            parameters: vec![],
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Module> {
        Box::new(MulNode {
            id,
            ..MulNode::default()
        })
    }
}

#[derive(Default)]
pub struct MulNode {
    id: usize,
    a_index: Option<NodeIndex>,
    b_index: Option<NodeIndex>,
}

impl Module for MulNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, pos: usize) -> Option<(usize, NodeIndex)> {
        match pos {
            0 => self.a_index.map(|x| (0, x)),
            1 => self.b_index.map(|x| (0, x)),
            _ => None,
        }
    }

    fn output(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 => self.a_index,
            1 => self.b_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let a = context.add_mono_node(Pass {});
        let b = context.add_mono_node(Pass {});

        let mul = context.add_mono_node(Mul::new(0.1));
        context.connect(a, mul);
        context.connect(b, mul);

        self.a_index = Some(a);
        self.b_index = Some(b);
    }

    fn set_parameter(&self, _: &mut AudioContext<1>, _: u8, _: f32) {}
}

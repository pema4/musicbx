use glicol_synth::{AudioContext, Message};
use glicol_synth::operator::Mul;
use petgraph::prelude::NodeIndex;

use crate::modules::{
    Module, ModuleDescription, ModuleInfo, ModuleInput, ModuleOutput, ModuleParameter,
    ModuleParameterKind,
};

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
                    description: "The first input of the amp".to_string(),
                },
                ModuleInput {
                    number: 1,
                    name: "b".to_string(),
                    description: "The second (optional) input of the amp".to_string(),
                },
            ],
            outputs: vec![ModuleOutput {
                number: 0,
                name: "out".to_string(),
                description: "The output of the amp".to_string(),
            }],
            parameters: vec![ModuleParameter {
                number: 0,
                kind: ModuleParameterKind::Db,
                default: "1.0".to_string(),
                name: "Amplitude".to_string(),
                description: "String".to_string(),
            }],
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

    fn input(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 | 1 => self.a_index,
            _ => None,
        }
    }

    fn output(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 => self.a_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let amp = context.add_mono_node(Mul::new(0.1));
        self.a_index = Some(amp);
    }

    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32) {
        if index == 0 {
            let amp = ModuleParameterKind::Number.denormalize(value);
            context.send_msg(self.a_index.unwrap(), Message::SetToNumber(index, amp));
        }
    }
}

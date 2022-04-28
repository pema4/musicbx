use glicol_synth::{AudioContext, Message};
use glicol_synth::operator::Mul;
use petgraph::prelude::NodeIndex;

use crate::modules::{
    Module, ModuleDescription, ModuleInfo, ModuleInput, ModuleOutput, ModuleParameter, ModuleParameterKind,
    to_amp,
};

#[derive(Default)]
pub struct AmpModule;

impl ModuleDescription for AmpModule {
    fn uid(&self) -> &'static str {
        "std.v1.amp"
    }

    fn info(&self) -> ModuleInfo {
        ModuleInfo {
            uid: self.uid().to_string(),
            name: "Amp".to_string(),
            description: "Amplifies the signal".to_string(),
            inputs: vec![ModuleInput {
                number: 0,
                name: "in".to_string(),
                description: "The input of the amp".to_string(),
            }],
            outputs: vec![ModuleOutput {
                number: 0,
                name: "out".to_string(),
                description: "The output of the amp".to_string(),
            }],
            parameters: vec![ModuleParameter {
                number: 0,
                kind: ModuleParameterKind::Db,
                default: "-6.0".to_string(),
                name: "amp".to_string(),
                description: "The amplitude".to_string(),
            }],
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Module> {
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

impl Module for AmpNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, pos: usize) -> Option<(usize, NodeIndex)> {
        match pos {
            0 => self.node_index.map(|x| (0, x)),
            1 => self.node_index.map(|x| (1, x)),
            _ => None,
        }
    }

    fn output(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 => self.node_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let amp = context.add_mono_node(Mul::new(to_amp(-6.0)));
        self.node_index = Some(amp);
    }

    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32) {
        if index == 0 {
            let db = ModuleParameterKind::Db.denormalize(value);
            let amp = to_amp(db);
            context.send_msg(self.node_index.unwrap(), Message::SetToNumber(index, amp));
        }
    }
}

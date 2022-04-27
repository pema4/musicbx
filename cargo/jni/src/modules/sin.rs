use glicol_synth::operator::Mul;
use glicol_synth::oscillator::SinOsc;
use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;

use crate::modules::{Module, ModuleDescription, ModuleInfo, ModuleInput, ModuleOutput};

#[derive(Default)]
pub struct SinModule;

impl ModuleDescription for SinModule {
    fn uid(&self) -> &'static str {
        "std.v1.sin"
    }

    fn info(&self) -> ModuleInfo {
        ModuleInfo {
            uid: self.uid().to_string(),
            name: "Sin Osc".to_string(),
            description: "The sine oscillator with customizable frequency".to_string(),
            inputs: vec![ModuleInput {
                number: 0,
                name: "Freq".to_string(),
                description: "Frequency of the oscillator".to_string(),
            }],
            outputs: vec![ModuleOutput {
                number: 0,
                name: "Oscillator".to_string(),
                description: "".to_string(),
            }],
        }
    }

    fn create_instance(&self, id: usize) -> Box<dyn Module> {
        Box::new(SinNode {
            id,
            ..SinNode::default()
        })
    }
}

#[derive(Default)]
pub struct SinNode {
    id: usize,
    sin_index: Option<NodeIndex>,
    amp_index: Option<NodeIndex>,
}

impl Module for SinNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 => self.sin_index,
            _ => None,
        }
    }

    fn output(&self, pos: usize) -> Option<NodeIndex> {
        match pos {
            0 => self.amp_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let sin = SinOsc::new().sr(context.sample_rate()).freq(440.0);
        let sin: NodeIndex = context.add_mono_node(sin);
        let amp = context.add_mono_node(Mul::new(0.5));
        context.connect(sin, amp);

        self.sin_index = Some(sin);
        self.amp_index = Some(amp);
    }
}

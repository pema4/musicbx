use std::collections::{HashMap, HashSet};
use std::hash::Hash;

use glicol_synth::operator::Mul;
use glicol_synth::oscillator::SinOsc;
use glicol_synth::{AudioContext, Message, Node};
use lazy_static::lazy_static;
use petgraph::graph::{EdgeIndex, NodeIndex};
use petgraph::visit::EdgeRef;

use crate::modules::{Module, ModuleDescription, ModuleInput, ModuleOutput};

#[derive(Default)]
pub struct SinModule {
    pub id: Option<usize>,
    node_indices: Vec<NodeIndex>,
}

impl Module for SinModule {
    fn id(&self) -> Option<usize> {
        self.id
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let sin = SinOsc::new().sr(context.sample_rate()).freq(440.0);
        let sin: NodeIndex = context.add_mono_node(sin);
        self.node_indices.push(sin);
        println!("Added sin node {}", sin.index());

        let amp = Mul::new(0.05);
        let amp = context.add_mono_node(amp);
        self.node_indices.push(amp);
        println!("Added amp node {}", amp.index());

        let edge = context.connect(sin, amp);
        println!("Added edge {}", edge.index());
        let edge = context.connect(amp, context.destination);
        println!("Added edge {}", edge.index());

        // println!("Added SinModule with id {:?}", self.id);
    }

    fn remove_from_context(&mut self, context: &mut AudioContext<1>) {
        let ids_to_delete: HashSet<_> = self.node_indices.iter().collect();

        let edges_to_delete: HashSet<_> = context
            .graph
            .node_indices()
            .filter(|node| ids_to_delete.contains(&node))
            .flat_map(|node| context.graph.edges(node))
            .map(|edge| edge.id())
            .collect();

        for edge in &edges_to_delete {
            println!("Edge to delete: {}", edge.index());
        }

        context.graph.retain_edges(|graph, edge| {
            if edges_to_delete.contains(&edge) {
                println!("Removed edge {}", edge.index());
            }
            edges_to_delete.contains(&edge)
        });
    }
}

impl ModuleDescription for SinModule {
    fn create_instance(&self, id: usize) -> Box<dyn Module> {
        Box::new(SinModule {
            id: Some(id),
            node_indices: vec![],
        })
    }

    fn uid(&self) -> &str {
        "sin"
    }

    fn name(&self) -> &str {
        "Sin Osc"
    }

    fn description(&self) -> &str {
        "The sine oscillator with customizable frequency"
    }

    fn inputs(&self) -> &[ModuleInput] {
        &INPUTS[..]
    }

    fn outputs(&self) -> &[ModuleOutput] {
        &OUTPUTS[..]
    }
}

lazy_static! {
    static ref INPUTS: [ModuleInput; 1] = [ModuleInput {
        number: 0,
        name: "Freq".to_string(),
        description: "Frequency of the oscillator".to_string()
    }];
    static ref OUTPUTS: [ModuleOutput; 1] = [ModuleOutput {
        number: 0,
        name: "Oscillator".to_string(),
        description: "".to_string()
    }];
}

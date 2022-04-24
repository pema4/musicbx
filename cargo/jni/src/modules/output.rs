use glicol_synth::AudioContext;
use lazy_static::lazy_static;

use crate::modules::{Module, ModuleDescription, ModuleInput, ModuleOutput};

#[derive(Default)]
pub struct OutputModule {
    pub id: Option<usize>,
}

impl Module for OutputModule {
    fn id(&self) -> Option<usize> {
        self.id
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let id = self.id;
        println!("Added OutputModule with id {id:?}");
        // OutputModule is a special one, because it is already in the graph
    }

    fn remove_from_context(&mut self, context: &mut AudioContext<1>) {
        // OutputModule cannot be deleted from the graph
    }
}

impl ModuleDescription for OutputModule {
    fn create_instance(&self, id: usize) -> Box<dyn Module> {
        Box::new(Self { id: Some(id) })
    }

    fn uid(&self) -> &str {
        "output"
    }

    fn name(&self) -> &str {
        "Output Module"
    }

    fn description(&self) -> &str {
        "The output module"
    }

    fn inputs(&self) -> &[ModuleInput] {
        &INPUTS[..]
    }

    fn outputs(&self) -> &[ModuleOutput] {
        &[]
    }
}

lazy_static! {
    static ref INPUTS: [ModuleInput; 1] = [ModuleInput {
        number: 0,
        name: "Left".to_string(),
        description: "The left output".to_string(),
    }];
}

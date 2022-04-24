use glicol_synth::{AudioContext, BoxedNode, Node, NodeData};
use serde::{Deserialize, Serialize};

pub use output::OutputModule;
pub use sin::SinModule;

mod output;
mod sin;

pub trait Module: ModuleDescription {
    fn id(&self) -> Option<usize>;
    fn add_to_context(&mut self, context: &mut AudioContext<1>);
    fn remove_from_context(&mut self, context: &mut AudioContext<1>);
}

pub trait ModuleDescription {
    fn create_instance(&self, id: usize) -> Box<dyn Module>;

    fn uid(&self) -> &str;
    fn name(&self) -> &str;
    fn description(&self) -> &str;
    fn inputs(&self) -> &[ModuleInput];
    fn outputs(&self) -> &[ModuleOutput];

    fn get_info(&self) -> ModuleInfo {
        ModuleInfo {
            uid: self.uid().into(),
            name: self.name().into(),
            description: self.description().into(),
            inputs: self.inputs().into(),
            outputs: self.outputs().into(),
        }
    }
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct ModuleInfo {
    uid: String,
    name: String,
    description: String,
    inputs: Vec<ModuleInput>,
    outputs: Vec<ModuleOutput>,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub struct ModuleInput {
    pub number: usize,
    pub name: String,
    pub description: String,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub struct ModuleOutput {
    pub number: usize,
    pub name: String,
    pub description: String,
}

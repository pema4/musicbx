use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;
use serde::{Deserialize, Serialize};

pub use output::OutputModule;
pub use sin::SinModule;

mod output;
mod sin;

pub trait ModuleDescription {
    fn uid(&self) -> &'static str;
    fn info(&self) -> ModuleInfo;
    fn create_instance(&self, id: usize) -> Box<dyn Module>;
}

pub trait Module {
    fn id(&self) -> usize;
    fn input(&self, pos: usize) -> Option<NodeIndex>;
    fn output(&self, pos: usize) -> Option<NodeIndex>;
    fn add_to_context(&mut self, context: &mut AudioContext<1>);
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

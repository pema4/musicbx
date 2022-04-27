use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;
use serde::{Deserialize, Serialize};

pub use mul::MulModule;
pub use output::OutputModule;
pub use sin::SinModule;

mod mul;
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
    fn set_parameter(&self, context: &mut AudioContext<1>, index: u8, value: f32);
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Default)]
pub struct ModuleInfo {
    uid: String,
    name: String,
    description: String,
    inputs: Vec<ModuleInput>,
    outputs: Vec<ModuleOutput>,
    parameters: Vec<ModuleParameter>,
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

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub struct ModuleParameter {
    pub number: usize,
    pub kind: ModuleParameterKind,
    pub default: String,
    pub name: String,
    pub description: String,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub enum ModuleParameterKind {
    Number,
    HzSlow,
    HzFast,
    Db,
}

impl ModuleParameterKind {
    fn min(&self) -> f32 {
        match self {
            ModuleParameterKind::Number => 0.0,
            ModuleParameterKind::HzSlow => 0.001f32.log2(),
            ModuleParameterKind::HzFast => 20f32.log2(),
            ModuleParameterKind::Db => 0f32.log2(),
        }
    }

    fn max(&self) -> f32 {
        match self {
            ModuleParameterKind::Number => 1.0,
            ModuleParameterKind::HzSlow => 200f32.log2(),
            ModuleParameterKind::HzFast => 22000f32.log2(),
            ModuleParameterKind::Db => 1.0f32.log2(),
        }
    }

    pub fn denormalize(&self, normalized: f32) -> f32 {
        let x = normalized * (self.max() - self.min()) + self.min();

        match self {
            ModuleParameterKind::Number => x,
            ModuleParameterKind::HzSlow | ModuleParameterKind::HzFast => x.exp2(),
            ModuleParameterKind::Db => x,
        }
    }
}

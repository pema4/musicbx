use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;
use serde::{Deserialize, Serialize};

pub use amp::AmpNodeDescription;
pub use mul::MulNodeDescription;
pub use noise::NoiseNodeDescription;
pub use output::OutputNodeDescription;
pub use simple_saw::SimpleSawNodeDescription;
pub use sin::SinNodeDescription;

mod amp;
mod mul;
mod noise;
mod output;
mod simple_saw;
mod sin;

pub trait NodeDescription {
    fn uid(&self) -> &'static str;
    fn info(&self) -> NodeInfo;
    fn create_instance(&self, id: usize) -> Box<dyn Node>;
}

pub trait Node {
    fn id(&self) -> usize;
    fn input(&self, pos: usize) -> Option<(usize, NodeIndex)>;
    fn output(&self, pos: usize) -> Option<NodeIndex>;
    fn add_to_context(&mut self, context: &mut AudioContext<1>);
    fn set_parameter(&self, _context: &mut AudioContext<1>, _index: u8, _value: f32) {}
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Default)]
pub struct NodeInfo {
    pub uid: String,
    pub name: String,
    pub summary: String,
    pub inputs: Vec<NodeInput>,
    pub outputs: Vec<NodeOutput>,
    pub parameters: Vec<NodeParameter>,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub struct NodeInput {
    pub number: usize,
    pub name: String,
    pub description: String,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub struct NodeOutput {
    pub number: usize,
    pub name: String,
    pub description: String,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub struct NodeParameter {
    pub number: usize,
    pub kind: NodeParameterKind,
    pub default: String,
    pub name: String,
    pub description: String,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Clone)]
pub enum NodeParameterKind {
    Number,
    HzSlow,
    HzFast,
    HzWide,
    Db,
}

impl NodeParameterKind {
    fn min(&self) -> f32 {
        use NodeParameterKind::*;
        match self {
            Number => 0.0,
            HzSlow => 0.001f32.log2(),
            HzFast => 20f32.log2(),
            HzWide => 0.001f32.log2(),
            Db => -120.0,
        }
    }

    fn max(&self) -> f32 {
        use NodeParameterKind::*;
        match self {
            Number => to_amp(12.0),
            HzSlow => 200f32.log2(),
            HzFast => 22000f32.log2(),
            HzWide => 22000f32.log2(),
            Db => 12.0,
        }
    }

    pub fn normalize(&self, denormalized: f32) -> f32 {
        let x = (denormalized - self.min()) / (self.max() - self.min());

        use NodeParameterKind::*;
        match self {
            Number => x,
            HzSlow | HzFast | HzWide => 2.0f32.powf(x),
            Db => x,
        }
    }

    pub fn denormalize(&self, normalized: f32) -> f32 {
        let x = normalized * (self.max() - self.min()) + self.min();

        use NodeParameterKind::*;
        match self {
            Number => x,
            HzSlow | HzFast | HzWide => x.exp2(),
            Db => x,
        }
    }
}

pub fn to_amp(db: f32) -> f32 {
    10.0f32.powf(db / 20.0)
}

pub fn to_db(amp: f32) -> f32 {
    20.0 * amp.log10()
}

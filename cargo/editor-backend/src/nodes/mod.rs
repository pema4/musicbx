use glicol_synth::AudioContext;
use petgraph::graph::NodeIndex;
use serde::Serialize;

pub use add::AddNodeDescription;
pub use amp::AmpNodeDescription;
pub use hard_clip::HardClipNodeDescription;
pub use mul::MulNodeDescription;
use musicbx::types::{NodeDefinition, NodeParameterKind};
pub use noise::NoiseNodeDescription;
pub use output::OutputNodeDescription;
pub use simple_saw::SimpleSawNodeDescription;
pub use sin::SinNodeDescription;
pub use test_fm::TestFmNodeDescription;

mod add;
mod amp;
mod hard_clip;
mod mul;
mod noise;
mod output;
mod simple_saw;
mod sin;
mod test_fm;

pub trait NodeFactory {
    fn uid(&self) -> &str;
    fn info(&self) -> &NodeInfo;
    fn create_instance(&self, id: usize) -> Box<dyn Node>;
}

pub trait Node {
    fn id(&self) -> usize;
    fn input(&self, name: &str) -> Option<(usize, NodeIndex)>;
    fn output(&self, name: &str) -> Option<NodeIndex>;
    fn add_to_context(&mut self, context: &mut AudioContext<1>);
    fn set_parameter(&self, _context: &mut AudioContext<1>, _index: u8, _value: f32) {}
}

#[derive(PartialEq, Eq, Debug, Serialize, Default)]
pub struct NodeInfo {
    pub definition: NodeDefinition,
    pub description: NodeDescription,
}

#[derive(PartialEq, Eq, Debug, Serialize, Default)]
pub struct NodeDescription {
    pub node: Description,
    pub inputs: &'static [Description],
    pub outputs: &'static [Description],
    pub parameters: &'static [Description],
}

#[derive(PartialEq, Eq, Debug, Serialize, Default)]
pub struct Description {
    pub name: &'static str,
    pub summary: &'static str,
}

impl Description {
    const fn new(name: &'static str, summary: &'static str) -> Self {
        Description { name, summary }
    }
}

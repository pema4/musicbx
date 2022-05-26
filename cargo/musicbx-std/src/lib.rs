// для использования макроса node

use musicbx_types::description::{ModuleDefinition, NodeDefinition};
extern crate musicbx_core as musicbx;

pub mod osc;
pub mod util;
pub mod filter;

#[derive(Clone, Copy)]
pub struct StdModuleDefinition;

impl ModuleDefinition for StdModuleDefinition {
    fn info_for_uid(&self, uid: &str) -> Option<&NodeDefinition> {
        NODE_DEFINITIONS.info_for_uid(uid)
    }
}

static NODE_DEFINITIONS: &[NodeDefinition] = &[
    osc::SinOsc::definition(),
    osc::SimpleSawOsc::definition(),
    util::Add::definition(),
    util::Amp::definition(),
    util::Mul::definition(),
    util::UniformRandom::definition(),
];

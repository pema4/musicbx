// #![feature(generic_associated_types)]

// для использования макроса node
extern crate musicbx_core as musicbx;

use musicbx_types::{ModuleDefinition, NodeDefinition};

pub mod filter;
pub mod osc;
pub mod util;

#[derive(Clone, Copy)]
pub struct StdModuleDefinition;

impl ModuleDefinition for StdModuleDefinition {
    fn info_for_uid(&self, uid: &str) -> Option<&NodeDefinition> {
        NODE_DEFINITIONS.iter().find(|x| x.uid == uid)
    }
}

static NODE_DEFINITIONS: &[NodeDefinition] = &[
    osc::SimpleSawOsc::definition(),
    osc::SinOsc::definition(),
    util::Add::definition(),
    util::Amp::definition(),
    util::HardClip::definition(),
    util::Mul::definition(),
    util::UniformRandom::definition(),
];

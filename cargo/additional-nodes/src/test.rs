#![allow(dead_code)]

use musicbx::node;
use musicbx::std::osc::SinOsc;
use musicbx::FromSampleRate;

#[node {
freq -> modulator.freq,
freq -> carrier.freq,
modulator.output -> carrier.phase_mod,
carrier.output -> output,
}]
#[derive(FromSampleRate)]
pub struct MyNode {
    carrier: SinOsc,
    modulator: SinOsc,
}

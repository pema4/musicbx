#![allow(dead_code)]

use musicbx::node;
use musicbx::std::osc;
use musicbx::FromSampleRate;

#[node {
    freq -> modulator.freq,
    freq -> carrier.freq,
    modulator.output -> carrier.phase_mod,
    carrier.output -> output,
}]
#[derive(FromSampleRate)]
pub struct MyNode {
    carrier: osc::SinOsc,
    modulator: osc::SinOsc,
}

#[derive(FromSampleRate)]
pub struct X {
    carrier: osc::SinOsc,
}

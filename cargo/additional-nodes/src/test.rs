#![allow(dead_code)]

use musicbx::FromSampleRate;

#[musicbx::node {
freq -> modulator.freq,
freq -> carrier.freq,
modulator.output -> carrier.phase_mod,
carrier.output -> output,
}]
#[derive(FromSampleRate)]
pub struct MyNode {
    carrier: musicbx::osc::SinOsc,
    modulator: musicbx::osc::SinOsc,
}

#[derive(FromSampleRate)]
pub struct X {
    carrier: musicbx::osc::SinOsc,
}
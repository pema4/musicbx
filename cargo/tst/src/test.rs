#![allow(dead_code)]

#[musicbx::node {
    freq -> modulator.freq,
    freq -> carrier.freq,
    modulator.out -> carrier.phase_mod,
    carrier.out -> out,
}]
pub struct MyNode {
    carrier: musicbx::osc::SinOsc,
    modulator: musicbx::osc::SinOsc,
}

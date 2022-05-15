#![allow(dead_code)]

use musicbx::{osc, util};

mod test;

#[musicbx::node(
    carrier_freq -> carrier.freq,
    mod_freq -> modulator.freq,
    mod_amount -> modulator_amp.left,
    modulator.out -> modulator_amp.right,
    modulator_amp.out -> carrier.phase_mod,
    carrier.out -> out,
)]
pub struct Synth {
    modulator: osc::SinOsc,
    modulator_amp: util::Mul,
    carrier: osc::SinOsc,
}

fn main() {
    println!("{:.7}", 23);
}

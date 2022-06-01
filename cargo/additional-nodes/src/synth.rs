use musicbx::std::{osc, util};
use musicbx::{node, FromSampleRate};

#[node(
    carrier_freq -> carrier.freq,
    mod_freq -> modulator.freq,
    mod_amount -> modulator_amp.a,
    modulator.output -> modulator_amp.b,
    modulator_amp.output -> carrier.phase_mod,
    carrier.output -> out,
)]
#[derive(FromSampleRate)]
pub struct Synth {
    modulator: osc::SinOsc,
    modulator_amp: util::Mul,
    carrier: osc::SinOsc,
}

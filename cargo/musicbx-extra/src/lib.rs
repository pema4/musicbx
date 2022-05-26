#[musicbx::node {
    freq -> modulator.freq,
    freq -> carrier.freq,
    modulator.output -> carrier.phase_mod,
    carrier.output -> out,
}]
pub struct MyNode {
    carrier: musicbx::osc::SinOsc,
    modulator: musicbx::osc::SinOsc,
}

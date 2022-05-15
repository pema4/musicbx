use musicbx::FromSampleRate;
use musicbx_core::{DataMut, DataRef};

#[derive(Debug, Clone)]
pub struct SinOsc {
    phase: f32,
    sr: f32,
}

impl FromSampleRate for SinOsc {
    fn from_sample_rate(sr: f32) -> Self {
        Self { phase: 0.0, sr }
    }
}

pub struct SinOscParameters<'a> {
    pub freq: DataRef<'a>,
    pub tune: DataRef<'a>,
    pub phase_mod: DataRef<'a>,
    pub out: DataMut<'a>,
}

impl Default for SinOscParameters<'static> {
    fn default() -> Self {
        Self {
            freq: 440.0.into(),
            tune: 1.0.into(),
            phase_mod: 0.0.into(),
            out: 0.0.into(),
        }
    }
}

impl SinOsc {
    pub fn process<'a, const N: usize>(&mut self, n: usize, parameters: SinOscParameters) {
        let SinOscParameters {
            freq,
            tune,
            phase_mod,
            mut out,
        } = parameters;

        for i in 0..n {
            self.phase += (freq[i] / self.sr) * tune[i] + phase_mod[i];
            self.phase %= 1.0;
            out[i] = (self.phase * 2.0 * std::f32::consts::PI).sin();
        }
    }
}

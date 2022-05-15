use musicbx::FromSampleRate;
use musicbx_core::{DataMut, DataRef};

#[derive(Debug, Clone)]
pub struct SimpleSawOsc {
    phase: f32,
    sr: f32,
}

impl FromSampleRate for SimpleSawOsc {
    fn from_sample_rate(sr: f32) -> Self {
        Self { phase: 0.0, sr }
    }
}

pub struct SimpleSawOscParameters<'a> {
    pub freq: DataRef<'a>,
    pub tune: DataRef<'a>,
    pub out: DataMut<'a>,
}

impl Default for SimpleSawOscParameters<'static> {
    fn default() -> Self {
        Self {
            freq: 440.0.into(),
            tune: 1.0.into(),
            out: 0.0.into(),
        }
    }
}

impl SimpleSawOsc {
    pub fn process<'a, const N: usize>(&mut self, n: usize, parameters: SimpleSawOscParameters) {
        let SimpleSawOscParameters {
            freq,
            tune,
            mut out,
        } = parameters;

        for i in 0..n {
            self.phase += (freq[i] / self.sr) * tune[i];
            self.phase %= 1.0;
            out[i] = self.phase * 2.0 - 1.0;
        }
    }
}

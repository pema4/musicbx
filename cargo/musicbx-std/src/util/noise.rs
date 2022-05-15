use musicbx::DataRef;
use musicbx_core::DataMut;
use rand::prelude::*;

#[derive(Debug, Clone)]
pub struct UniformRandom {
    rng: StdRng,
}

impl Default for UniformRandom {
    fn default() -> Self {
        UniformRandom {
            rng: StdRng::from_seed(random()),
        }
    }
}

pub struct UniformRandomParameters<'a> {
    pub low: DataRef<'a>,
    pub high: DataRef<'a>,
    pub out: DataMut<'a>,
}

impl Default for UniformRandomParameters<'static> {
    fn default() -> Self {
        Self {
            low: DataRef::Float(-1.0),
            high: DataRef::Float(1.0),
            out: DataMut::Float(0.0),
        }
    }
}

impl UniformRandom {
    pub fn process<const N: usize>(&mut self, n: usize, parameters: UniformRandomParameters) {
        let UniformRandomParameters { low, high, mut out } = parameters;

        for i in 0..n {
            let next = self.rng.gen::<f32>();
            out[i] = next * (high[i] - low[i]) + low[i];
        }
    }
}

use rand::prelude::*;

use musicbx::{DataRef, Node};
use musicbx_core::DataMut;
use musicbx_types::{NodeDefinition, NodeOutput};

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
    pub output: DataMut<'a>,
}

impl<'a> Default for UniformRandomParameters<'a> {
    fn default() -> Self {
        Self {
            low: DataRef::Float(-1.0),
            high: DataRef::Float(1.0),
            output: DataMut::Float(0.0),
        }
    }
}

impl<'a> Node<'a> for UniformRandom {
    type Parameters = UniformRandomParameters<'a>;
    fn process<const N: usize>(&mut self, n: usize, parameters: Self::Parameters) {
        let UniformRandomParameters {
            low,
            high,
            mut output,
        } = parameters;

        for i in 0..n {
            let next = self.rng.gen::<f32>();
            output[i] = next * (high[i] - low[i]) + low[i];
        }
    }
}

impl UniformRandom {
    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::std::util::UniformRandom",
            inputs: &[],
            outputs: &[NodeOutput {
                number: 0,
                name: "output",
            }],
            parameters: &[],
        }
    }
}

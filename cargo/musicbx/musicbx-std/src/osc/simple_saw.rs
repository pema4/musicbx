use musicbx::Node;
use musicbx_core::{DataMut, DataRef};
use musicbx_derive::FromSampleRate;
use musicbx_types::{NodeDefinition, NodeInput, NodeOutput, NodeParameter, NodeParameterKind};

#[derive(Debug, Clone, FromSampleRate)]
pub struct SimpleSawOsc {
    phase: f32,
    #[from(sr)]
    sr: f32,
}

pub struct SimpleSawOscParameters<'a> {
    pub freq: DataRef<'a>,
    pub tune: DataRef<'a>,
    pub output: DataMut<'a>,
}

impl<'a> Default for SimpleSawOscParameters<'a> {
    fn default() -> Self {
        Self {
            freq: 440.0.into(),
            tune: 1.0.into(),
            output: 0.0.into(),
        }
    }
}

impl<'a> Node<'a> for SimpleSawOsc {
    type Parameters = SimpleSawOscParameters<'a>;

    fn process<const N: usize>(&mut self, n: usize, parameters: Self::Parameters) {
        let Self::Parameters {
            freq,
            tune,
            output: mut out,
        } = parameters;

        for i in 0..n {
            self.phase += (freq[i] / self.sr) * tune[i];
            self.phase %= 1.0;
            out[i] = self.phase * 2.0 - 1.0;
        }
    }
}

impl SimpleSawOsc {
    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::std::osc::SimpleSawOsc",
            inputs: &[NodeInput {
                number: 0,
                name: "tune",
            }],
            outputs: &[NodeOutput {
                number: 0,
                name: "output",
            }],
            parameters: &[NodeParameter {
                number: 0,
                kind: NodeParameterKind::HzSlow,
                default: "0.5",
                name: "freq",
            }],
        }
    }
}

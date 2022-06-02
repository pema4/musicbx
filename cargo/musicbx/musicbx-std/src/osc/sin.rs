use musicbx::Node;
use musicbx_core::{DataMut, DataRef};
use musicbx_derive::FromSampleRate;
use musicbx_types::{NodeDefinition, NodeInput, NodeOutput, NodeParameter, NodeParameterKind};

#[derive(Debug, Clone, FromSampleRate)]
pub struct SinOsc {
    phase: f32,
    #[from(sr)]
    sr: f32,
}

pub struct SinOscParameters<'a> {
    pub freq: DataRef<'a>,
    pub tune: DataRef<'a>,
    pub phase_mod: DataRef<'a>,
    pub output: DataMut<'a>,
}

impl<'a> Default for SinOscParameters<'a> {
    fn default() -> Self {
        Self {
            freq: 440.0.into(),
            tune: 1.0.into(),
            phase_mod: 0.0.into(),
            output: 0.0.into(),
        }
    }
}

impl<'a> Node<'a> for SinOsc {
    type Parameters = SinOscParameters<'a>;

    fn process<const N: usize>(&mut self, n: usize, parameters: SinOscParameters) {
        let SinOscParameters {
            freq,
            tune,
            phase_mod,
            output: mut out,
        } = parameters;

        for i in 0..n {
            self.phase += (freq[i] / self.sr) * tune[i] + phase_mod[i];
            self.phase %= 1.0;
            out[i] = (self.phase * 2.0 * std::f32::consts::PI).sin();
        }
    }
}

impl SinOsc {
    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::std::osc::SinOsc",
            inputs: &[
                NodeInput {
                    number: 0,
                    name: "phase_mod",
                },
                NodeInput {
                    number: 1,
                    name: "tune",
                },
            ],
            outputs: &[NodeOutput {
                number: 0,
                name: "output",
            }],
            parameters: &[NodeParameter {
                number: 0,
                kind: NodeParameterKind::HzWide,
                default: "440.0",
                name: "freq",
            }],
        }
    }
}

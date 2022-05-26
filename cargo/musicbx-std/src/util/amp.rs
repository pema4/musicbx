use musicbx_core::{DataMut, DataRef};
use musicbx_types::{
    description::{NodeDefinition, NodeInput, NodeOutput, NodeParameter},
    parameter::{to_amp, NodeParameterKind},
};

#[derive(Default, Debug, Clone)]
pub struct Amp;

pub struct AmpParameters<'a> {
    pub input: DataRef<'a>,
    pub db: DataRef<'a>,
    pub output: DataMut<'a>,
}

impl Default for AmpParameters<'static> {
    fn default() -> Self {
        Self {
            input: DataRef::Float(0.0),
            db: DataRef::Float(-6.0),
            output: DataMut::Float(0.0),
        }
    }
}

impl Amp {
    pub fn process<'a, const N: usize>(&mut self, n: usize, parameters: AmpParameters) {
        let AmpParameters {
            input,
            db,
            mut output,
        } = parameters;

        for i in 0..n {
            output[i] = input[i] * to_amp(db[i]);
        }
    }

    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::util::Amp",
            inputs: &[NodeInput {
                number: 0,
                name: "input",
            }],
            outputs: &[NodeOutput {
                number: 0,
                name: "output",
            }],
            parameters: &[NodeParameter {
                number: 0,
                kind: NodeParameterKind::Db,
                default: "-6.0",
                name: "db",
            }],
        }
    }
}

use musicbx::Node;
use musicbx_core::{DataMut, DataRef};
use musicbx_types::{
    to_amp, NodeDefinition, NodeInput, NodeOutput, NodeParameter, NodeParameterKind,
};

#[derive(Default, Debug, Clone)]
pub struct Amp;

pub struct AmpParameters<'a> {
    pub input: DataRef<'a>,
    pub db: DataRef<'a>,
    pub output: DataMut<'a>,
}

impl<'a> Default for AmpParameters<'a> {
    fn default() -> Self {
        Self {
            input: DataRef::Float(0.0),
            db: DataRef::Float(-6.0),
            output: DataMut::Float(0.0),
        }
    }
}

impl<'a> Node<'a> for Amp {
    type Parameters = AmpParameters<'a>;

    fn process<const N: usize>(&mut self, n: usize, parameters: Self::Parameters) {
        let Self::Parameters {
            input,
            db,
            mut output,
        } = parameters;

        for i in 0..n {
            output[i] = input[i] * to_amp(db[i]);
        }
    }
}

impl Amp {
    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::std::util::Amp",
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

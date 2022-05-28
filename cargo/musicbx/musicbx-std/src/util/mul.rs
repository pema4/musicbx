use musicbx_core::{DataMut, DataRef};
use musicbx_types::{NodeDefinition, NodeInput, NodeOutput, NodeParameter, NodeParameterKind};

#[derive(Default, Debug, Clone)]
pub struct Mul;

pub struct MulParameters<'a> {
    pub a: DataRef<'a>,
    pub b: DataRef<'a>,
    pub output: DataMut<'a>,
}

impl Default for MulParameters<'static> {
    #[inline]
    fn default() -> Self {
        Self {
            a: DataRef::Float(1.0),
            b: DataRef::Float(1.0),
            output: DataMut::Float(0.0),
        }
    }
}

impl Mul {
    pub fn process<'a, const N: usize>(&mut self, n: usize, parameters: MulParameters) {
        let MulParameters { a, b, mut output } = parameters;

        for i in 0..n {
            output[i] = a[i] * b[i]
        }
    }

    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::std::util::Mul",
            inputs: &[
                NodeInput {
                    number: 0,
                    name: "a",
                },
                NodeInput {
                    number: 1,
                    name: "b",
                },
            ],
            outputs: &[NodeOutput {
                number: 0,
                name: "output",
            }],
            parameters: &[NodeParameter {
                number: 0,
                kind: NodeParameterKind::Number,
                default: "1.0",
                name: "b",
            }],
        }
    }
}

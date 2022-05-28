use musicbx_core::{DataMut, DataRef};
use musicbx_types::{NodeDefinition, NodeInput, NodeOutput, NodeParameter, NodeParameterKind};

#[derive(Default)]
pub struct Add;

pub struct AddParameters<'a> {
    pub left: DataRef<'a>,
    pub right: DataRef<'a>,
    pub out: DataMut<'a>,
}

impl Default for AddParameters<'static> {
    #[inline]
    fn default() -> Self {
        Self {
            left: DataRef::Float(1.0),
            right: DataRef::Float(1.0),
            out: DataMut::Float(0.0),
        }
    }
}

impl Add {
    pub fn process<'a, const N: usize>(&mut self, n: usize, parameters: AddParameters) {
        let AddParameters {
            left,
            right,
            mut out,
        } = parameters;

        for i in 0..n {
            out[i] = left[i] + right[i]
        }
    }

    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::std::util::Add",
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

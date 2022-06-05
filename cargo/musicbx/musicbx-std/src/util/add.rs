use musicbx::Node;
use musicbx_core::{DataMut, DataRef};
use musicbx_types::{NodeDefinition, NodeInput, NodeOutput, NodeParameter, NodeParameterKind};

#[derive(Default, Debug, Copy, Clone)]
pub struct Add;

#[derive(Default)]
pub struct AddParameters<'a> {
    pub a: DataRef<'a>,
    pub b: DataRef<'a>,
    pub output: DataMut<'a>,
}

impl<'a> Node<'a> for Add {
    type Parameters = AddParameters<'a>;

    fn process<const N: usize>(&mut self, n: usize, parameters: Self::Parameters) {
        let AddParameters { a, b, mut output } = parameters;

        for i in 0..n {
            output[i] = a[i] + b[i]
        }
    }
}

impl Add {
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
                default: "0.0",
                name: "b",
            }],
        }
    }
}

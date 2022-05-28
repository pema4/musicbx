use musicbx_core::{DataMut, DataRef};
use musicbx_types::{NodeDefinition, NodeInput, NodeOutput};

#[derive(Default, Debug, Clone)]
pub struct HardClip;

pub struct HardClipParameters<'a> {
    pub input: DataRef<'a>,
    pub output: DataMut<'a>,
}

impl Default for HardClipParameters<'static> {
    #[inline]
    fn default() -> Self {
        Self {
            input: DataRef::Float(0.0),
            output: DataMut::Float(0.0),
        }
    }
}

impl HardClip {
    pub fn process<'a, const N: usize>(&mut self, n: usize, parameters: HardClipParameters) {
        let HardClipParameters { input, mut output } = parameters;

        for i in 0..n {
            output[i] = if input[i] <= -1.0 {
                -1.0
            } else if input[i] <= 1.0 {
                input[i]
            } else {
                1.0
            }
        }
    }

    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "musicbx::std::util::HardClip",
            inputs: &[NodeInput {
                number: 0,
                name: "input",
            }],
            outputs: &[NodeOutput {
                number: 0,
                name: "output",
            }],
            parameters: &[],
        }
    }
}

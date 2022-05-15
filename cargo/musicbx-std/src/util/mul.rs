use musicbx_core::{DataMut, DataRef};

#[derive(Default, Debug, Clone)]
pub struct Mul;

pub struct MulParameters<'a> {
    pub left: DataRef<'a>,
    pub right: DataRef<'a>,
    pub out: DataMut<'a>,
}

impl Default for MulParameters<'static> {
    fn default() -> Self {
        Self {
            left: DataRef::Float(1.0),
            right: DataRef::Float(1.0),
            out: DataMut::Float(0.0),
        }
    }
}

impl Mul {
    pub fn process<'a, const N: usize>(&mut self, n: usize, parameters: MulParameters) {
        let MulParameters {
            left,
            right,
            mut out,
        } = parameters;

        for i in 0..n {
            out[i] = left[i] * right[i]
        }
    }
}

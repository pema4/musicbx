use musicbx_core::{DataRef, DataMut};

#[derive(Default)]
pub struct Add;

pub struct AddParameters<'a> {
    pub left: DataRef<'a>,
    pub right: DataRef<'a>,
    pub out: DataMut<'a>,
}

impl Default for AddParameters<'static> {
    fn default() -> Self {
        Self {
            left: DataRef::Float(1.0),
            right: DataRef::Float(1.0),
            out: DataMut::Float(0.0),
        }
    }
}

impl Add {
    pub fn process<'a, const N: usize>(
        &mut self,
        n: usize,
        parameters: AddParameters,
    ) {
        let AddParameters { left, right, mut out } = parameters;

        for i in 0..n {
            out[i] = left[i] + right[i]
        }
    }
}

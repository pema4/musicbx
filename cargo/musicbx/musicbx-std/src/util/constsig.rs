use musicbx::Node;
use musicbx_core::DataMut;

#[derive(Default)]
pub struct ConstSig {
    value: f32,
}

impl ConstSig {
    #[inline]
    pub fn new(value: f32) -> Self {
        Self { value }
    }
}

#[derive(Default)]
pub struct ConstSigParameters<'a> {
    pub output: DataMut<'a>,
}

impl<'a> Node<'a> for ConstSig {
    type Parameters = ConstSigParameters<'a>;

    fn process<const N: usize>(&mut self, n: usize, parameters: ConstSigParameters) {
        let ConstSigParameters { output } = parameters;

        match output {
            DataMut::Audio(audio) => audio[..n].fill(self.value),
            DataMut::Float(_) => (),
        };
    }
}

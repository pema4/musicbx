use musicbx_core::DataMut;

pub struct ConstSig {
    value: f32,
}

impl ConstSig {
    pub fn new(value: f32) -> Self {
        Self { value }
    }
}

pub struct ConstSigParameters<'a> {
    pub output: DataMut<'a>,
}

impl Default for ConstSigParameters<'static> {
    fn default() -> Self {
        Self {
            output: DataMut::Float(0.0),
        }
    }
}

impl ConstSig {
    pub fn process<'a, const N: usize>(
        &mut self,
        n: usize,
        parameters: ConstSigParameters,
    ) {
        let ConstSigParameters { output } = parameters;

        match output {
            DataMut::Audio(audio) => audio[..n].fill(self.value),
            DataMut::Float(_) => (),
        };
    }
}
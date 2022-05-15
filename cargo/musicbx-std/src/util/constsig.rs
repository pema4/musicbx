use musicbx_core::DataMut;

pub struct ConstSig {
    value: f32,
}

pub struct ConstSigParameters<'a> {
    pub out: DataMut<'a>,
}

impl Default for ConstSigParameters<'static> {
    fn default() -> Self {
        Self {
            out: DataMut::Float(0.0),
        }
    }
}

impl ConstSig {
    pub fn process<'a, const N: usize>(
        &mut self,
        n: usize,
        parameters: ConstSigParameters,
    ) {
        let ConstSigParameters { out } = parameters;

        match out {
            DataMut::Audio(audio) => audio[..n].fill(self.value),
            DataMut::Float(_) => (),
        };
    }
}
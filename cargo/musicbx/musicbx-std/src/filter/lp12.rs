use musicbx::Node;
use musicbx_core::{DataMut, DataRef};

#[derive(Default)]
pub struct LP12Filter {
    _phase: f32,
}

pub struct LP12FilterParameters<'a> {
    pub input: DataRef<'a>,
    pub cutoff: DataRef<'a>,
    pub q: DataRef<'a>,
    pub out: DataMut<'a>,
}

impl<'a> Default for LP12FilterParameters<'a> {
    fn default() -> Self {
        Self {
            input: 0.0.into(),
            cutoff: 22050f32.into(),
            q: 0.71.into(),
            out: 0.0.into(),
        }
    }
}

impl<'a> Node<'a> for LP12Filter {
    type Parameters = LP12FilterParameters<'a>;

    fn process<const N: usize>(&mut self, n: usize, parameters: Self::Parameters) {
        let Self::Parameters {
            input,
            cutoff: _,
            q: _,
            mut out,
        } = parameters;

        for i in 0..n {
            // self._phase += 1.0 / freq[i] + phase_mod[i];
            // self._phase %= 1.0;
            out[i] = input[i];
        }
    }
}

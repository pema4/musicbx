use crate::FromSampleRate;

pub trait Node<'a>: FromSampleRate {
    type Parameters: 'a + Default;
    fn process<const N: usize>(&mut self, n: usize, param: Self::Parameters);
}

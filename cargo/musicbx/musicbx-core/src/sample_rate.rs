pub trait FromSampleRate {
    fn from_sample_rate(sr: f32) -> Self;
}

impl<T: Default> FromSampleRate for T {
    fn from_sample_rate(_: f32) -> Self {
        Self::default()
    }
}

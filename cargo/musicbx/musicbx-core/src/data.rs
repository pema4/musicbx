use std::ops::{Index, IndexMut};

#[derive(Debug, Copy, Clone)]
pub enum DataRef<'a> {
    Audio(&'a [f32]),
    Float(f32),
}

impl<'a> From<&'a [f32]> for DataRef<'a> {
    fn from(audio: &'a [f32]) -> Self {
        Self::Audio(audio)
    }
}

impl<'a, const N: usize> From<&'a [f32; N]> for DataRef<'a> {
    fn from(audio: &'a [f32; N]) -> Self {
        Self::Audio(audio)
    }
}

impl From<f32> for DataRef<'static> {
    fn from(float: f32) -> Self {
        Self::Float(float)
    }
}

impl<'a> Index<usize> for DataRef<'a> {
    type Output = f32;

    fn index(&self, index: usize) -> &Self::Output {
        match self {
            DataRef::Audio(floats) => &floats[index],
            DataRef::Float(float) => &float,
        }
    }
}

#[derive(Debug)]
pub enum DataMut<'a> {
    Audio(&'a mut [f32]),
    Float(f32),
}

impl<'a> From<&'a mut [f32]> for DataMut<'a> {
    fn from(audio: &'a mut [f32]) -> Self {
        Self::Audio(audio)
    }
}

impl<'a, const N: usize> From<&'a mut [f32; N]> for DataMut<'a> {
    fn from(audio: &'a mut [f32; N]) -> Self {
        Self::Audio(audio)
    }
}

impl From<f32> for DataMut<'static> {
    fn from(float: f32) -> Self {
        Self::Float(float)
    }
}

impl<'a> Index<usize> for DataMut<'a> {
    type Output = f32;

    fn index(&self, index: usize) -> &Self::Output {
        match self {
            DataMut::Audio(floats) => &floats[index],
            DataMut::Float(float) => float,
        }
    }
}

impl<'a> IndexMut<usize> for DataMut<'a> {
    fn index_mut(&mut self, index: usize) -> &mut Self::Output {
        match self {
            DataMut::Audio(floats) => &mut floats[index],
            DataMut::Float(float) => float,
        }
    }
}

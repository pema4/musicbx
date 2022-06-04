use serde::{Deserialize, Serialize};

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize, Copy, Clone)]
pub enum NodeParameterKind {
    Number,
    HzSlow,
    HzFast,
    HzWide,
    Db,
}

impl NodeParameterKind {
    fn min(&self) -> f32 {
        use NodeParameterKind::*;
        match self {
            Number => 0.0,
            HzSlow => 0.001f32.log2(),
            HzFast => 20f32.log2(),
            HzWide => 0.001f32.log2(),
            Db => -120.0,
        }
    }

    fn max(&self) -> f32 {
        use NodeParameterKind::*;
        match self {
            Number => 1.0,
            HzSlow => 200f32.log2(),
            HzFast => 22000f32.log2(),
            HzWide => 22000f32.log2(),
            Db => 12.0,
        }
    }

    pub fn normalize(&self, denormalized: f32) -> f32 {
        let x = (denormalized - self.min()) / (self.max() - self.min());

        use NodeParameterKind::*;
        match self {
            Number => x,
            HzSlow | HzFast | HzWide => 2.0f32.powf(x),
            Db => x,
        }
    }

    pub fn denormalize(&self, normalized: f32) -> f32 {
        let x = normalized * (self.max() - self.min()) + self.min();

        use NodeParameterKind::*;
        match self {
            Number => x,
            HzSlow | HzFast | HzWide => x.exp2(),
            Db => x,
        }
    }
}

pub fn to_amp(db: f32) -> f32 {
    10.0f32.powf(db / 20.0)
}

pub fn to_db(amp: f32) -> f32 {
    20.0 * amp.log10()
}

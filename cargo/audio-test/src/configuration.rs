use serde::{Deserialize, Serialize};

#[derive(PartialEq, Debug, Serialize, Deserialize)]
pub struct Configuration {
    pub output: OutputConfiguration,
}

#[derive(PartialEq, Debug, Serialize, Deserialize)]
pub struct OutputConfiguration {
    pub current: String,
    pub available: Vec<String>,
    pub sample_rate: SampleRateConfiguration,
}

#[derive(PartialEq, Debug, Serialize, Deserialize)]
pub struct SampleRateConfiguration {
    pub current: f64,
    pub available: Vec<f64>,
}

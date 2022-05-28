use serde::{Deserialize, Serialize};

#[derive(Default, PartialEq, Debug, Serialize, Deserialize)]
pub struct IOConfiguration {
    pub output: DeviceConfiguration,
}

#[derive(Default, PartialEq, Debug, Serialize, Deserialize)]
pub struct DeviceConfiguration {
    pub current: Option<String>,
    pub available: Vec<String>,
    pub sample_rate: Option<SampleRateConfiguration>,
}

#[derive(Default, PartialEq, Debug, Serialize, Deserialize)]
pub struct SampleRateConfiguration {
    pub current: u32,
    pub available: Vec<u32>,
}

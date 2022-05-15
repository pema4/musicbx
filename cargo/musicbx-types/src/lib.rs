use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct PatchInfo {
    pub nodes: Vec<NodeInfo>,
    pub cables: Vec<Cable>,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct NodeInfo {
    pub id: usize,
    pub uid: String,
    pub offset: GridOffset,
    pub parameters: HashMap<u8, String>,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct GridOffset {
    pub x: i32,
    pub y: i32,
}

#[derive(Eq, PartialEq, Hash, Copy, Clone, Debug, Serialize, Deserialize)]
pub struct Cable {
    pub from: CableEnd,
    pub to: CableEnd,
}

#[derive(Eq, PartialEq, Hash, Copy, Clone, Debug, Serialize, Deserialize)]
pub struct CableEnd {
    pub node: usize,
    pub socket: usize,
}

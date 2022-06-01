use std::collections::HashMap;

use serde::{Deserialize, Serialize};

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct Patch {
    pub nodes: Vec<Node>,
    pub cables: Vec<Cable>,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct Node {
    pub id: usize,
    pub uid: String,
    pub offset: GridOffset,
    pub parameters: HashMap<String, String>,
    #[serde(default)]
    pub collapsed: bool,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct GridOffset {
    pub x: i32,
    pub y: i32,
}

#[derive(Eq, PartialEq, Hash, Clone, Debug, Serialize, Deserialize)]
pub struct Cable {
    pub from: CableEnd,
    pub to: CableEnd,
}

#[derive(Eq, PartialEq, Hash, Clone, Debug, Serialize, Deserialize)]
pub struct CableEnd {
    pub node_id: usize,
    pub socket_name: String,
}

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

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

#[cfg(test)]
mod test {
    use super::*;
    use serde_json::json;

    #[test]
    fn deserialize() {
        let json_string = {
            let json = json!({
                "nodes": [
                    {
                        "id": 0usize,
                        "uid": "std.v1.sin",
                        "offset": {
                            "x": 10usize,
                            "y": 20usize,
                        },
                        "parameters": {}
                    }
                ],
                "cables": [
                    {
                        "from": {
                            "node": 0usize,
                            "socket": 0usize,
                        },
                        "to": {
                            "node": 1usize,
                            "socket": 1usize,
                        }
                    }
                ],
            });
            json.to_string()
        };

        let result: serde_json::Result<Patch> = serde_json::from_str(&json_string);
        assert!(result.is_ok());
    }
}

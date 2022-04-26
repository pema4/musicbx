use serde::{Deserialize, Serialize};

use crate::modules::{ModuleInput, ModuleOutput};

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct PatchInfo {
    pub modules: Vec<ModuleInfo>,
    pub cables: Vec<Cable>,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct ModuleInfo {
    pub id: usize,
    pub uid: String,
    pub name: String,
    pub inputs: Vec<ModuleInput>,
    pub outputs: Vec<ModuleOutput>,
    pub offset: GridOffset,
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
    pub module: usize,
    pub socket: usize,
}

#[cfg(test)]
mod test {
    use serde_json::json;

    use crate::patch::PatchInfo;

    #[test]
    fn deserialize() {
        let json_string = {
            let json = json!({
                "modules": [
                    {
                        "id": 0usize,
                        "name": "First",
                        "inputs": [],
                        "outputs": [],
                        "offset": {
                            "x": 10usize,
                            "y": 20usize,
                        }
                    }
                ],
                "cables": [
                    {
                        "from": {
                            "module": 0usize,
                            "socket": 0usize,
                        },
                        "to": {
                            "module": 1usize,
                            "socket": 1usize,
                        }
                    }
                ],
            });
            json.to_string()
        };

        let result: serde_json::Result<PatchInfo> = serde_json::from_str(&json_string);
        assert!(result.is_ok());
    }
}

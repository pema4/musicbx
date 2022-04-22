use serde::{Deserialize, Serialize};

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct Patch {
    modules: Vec<Module>,
    cables: Vec<Cable>,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct Module {
    id: i32,
    name: String,
    inputs: Vec<ModuleInput>,
    outputs: Vec<ModuleOutput>,
    offset: GridOffset,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct GridOffset {
    x: i32,
    y: i32,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct ModuleInput {
    number: i32,
    name: String,
    description: String,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct ModuleOutput {
    number: i32,
    name: String,
    description: String,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
pub struct Cable {
    from: CableEnd,
    to: CableEnd,
}

#[derive(PartialEq, Eq, Debug, Serialize, Deserialize)]
struct CableEnd {
    module_id: i32,
    socket_number: i32,
}

mod test {
    use serde_json::json;

    use crate::model::*;

    #[test]
    fn deserialize() {
        let json_string = {
            let json = json!({
                "modules": [
                    {
                        "id": 0i32,
                        "name": "First",
                        "inputs": [],
                        "outputs": [],
                        "offset": {
                            "x": 10i32,
                            "y": 20i32,
                        }
                    }
                ],
                "cables": [
                    {
                        "from": {
                            "module_id": 0i32,
                            "socket_number": 0i32,
                        },
                        "to": {
                            "module_id": 1i32,
                            "socket_number": 1i32,
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

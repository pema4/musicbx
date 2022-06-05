use std::sync::Arc;

use jni::objects::{JClass, JObject, JValue};
use jni::JNIEnv;
use serde::Serialize;

use musicbx::types::NodeParameterKind;

use crate::nodes::{Description, NodeInfo};
use crate::unwrap_or_throw;
use crate::{App, AppMsg};

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_NativeAvailableNodesService_registerListener(
    env: JNIEnv,
    _: JClass,
    callback: JObject,
) {
    let vm = Arc::new(env.get_java_vm().unwrap());
    let callback = Arc::new(env.new_global_ref(callback).unwrap());
    let callback = move |nodes: &[&NodeInfo]| {
        let _attach_guard = vm.attach_current_thread().unwrap();
        let env = vm.get_env().unwrap();
        let listener = callback.as_obj();

        let nodes: Vec<_> = nodes.iter().map(|&x| x.into()).collect();
        unwrap_or_throw!(env, invoke_listener(env, listener, &nodes[..]));
    };
    let callback = Arc::new(callback);
    App::current().accept_message(AppMsg::RegisterAvailableNodesListener(callback));
}

fn invoke_listener(env: JNIEnv, listener: JObject, nodes: &[Node]) -> anyhow::Result<()> {
    let nodes_json = serde_json::to_string(nodes)?;
    let args: [JValue; 1] = [env.new_string(&nodes_json)?.into()];
    env.call_method(listener, "accept", "(Ljava/lang/String;)V", &args)
        .unwrap();
    Ok(())
}

#[derive(PartialEq, Eq, Debug, Serialize, Default)]
struct Node {
    pub uid: String,
    pub name: String,
    pub summary: String,
    pub inputs: Vec<NodeInput>,
    pub outputs: Vec<NodeOutput>,
    pub parameters: Vec<NodeParameter>,
}

impl From<&NodeInfo> for Node {
    fn from(info: &NodeInfo) -> Self {
        let name = info.description.node.name.to_string();
        Self {
            uid: info.definition.uid.into(),
            name,
            summary: info.description.node.summary.into(),
            inputs: info
                .definition
                .inputs
                .iter()
                .filter_map(|def| {
                    let desc = info
                        .description
                        .inputs
                        .iter()
                        .find(|x| x.name == def.name)?;
                    Some((def, desc).into())
                })
                .collect(),
            outputs: info
                .definition
                .outputs
                .iter()
                .filter_map(|def| {
                    let desc = info
                        .description
                        .outputs
                        .iter()
                        .find(|x| x.name == def.name)?;
                    Some((def, desc).into())
                })
                .collect(),
            parameters: info
                .definition
                .parameters
                .iter()
                .filter_map(|def| {
                    let desc = info
                        .description
                        .parameters
                        .iter()
                        .find(|x| x.name == def.name)?;
                    Some((def, desc).into())
                })
                .collect(),
        }
    }
}

#[derive(PartialEq, Eq, Debug, Serialize, Clone)]
struct NodeInput {
    pub number: usize,
    pub name: String,
    pub description: String,
}

impl From<(&musicbx::types::NodeInput, &Description)> for NodeInput {
    fn from((input, desc): (&musicbx::types::NodeInput, &Description)) -> Self {
        Self {
            number: input.number,
            name: input.name.to_string(),
            description: desc.summary.to_string(),
        }
    }
}

#[derive(PartialEq, Eq, Debug, Serialize, Clone)]
struct NodeOutput {
    pub number: usize,
    pub name: String,
    pub description: String,
}

impl From<(&musicbx::types::NodeOutput, &Description)> for NodeOutput {
    fn from((input, desc): (&musicbx::types::NodeOutput, &Description)) -> Self {
        Self {
            number: input.number,
            name: input.name.to_string(),
            description: desc.summary.to_string(),
        }
    }
}

#[derive(PartialEq, Eq, Debug, Serialize, Clone)]
struct NodeParameter {
    pub number: usize,
    pub kind: NodeParameterKind,
    pub default: String,
    pub name: String,
    pub description: String,
}

impl From<(&musicbx::types::NodeParameter, &Description)> for NodeParameter {
    fn from((param, desc): (&musicbx::types::NodeParameter, &Description)) -> Self {
        Self {
            number: param.number,
            kind: param.kind,
            default: param.default.to_string(),
            name: param.name.to_string(),
            description: desc.summary.to_string(),
        }
    }
}

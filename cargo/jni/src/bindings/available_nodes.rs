use std::sync::Arc;

use jni::objects::{JClass, JObject, JValue};
use jni::JNIEnv;

use crate::bindings::throw_illegal_state_exception;
use crate::nodes::NodeInfo;
use crate::{App, AppMsg};

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_AvailableNodesService_registerListener(
    env: JNIEnv,
    _: JClass,
    callback: JObject,
) {
    let vm = Arc::new(env.get_java_vm().unwrap());
    let callback = Arc::new(env.new_global_ref(callback).unwrap());
    let callback = move |nodes: &[NodeInfo]| {
        let _attach_guard = vm.attach_current_thread().unwrap();
        let env = vm.get_env().unwrap();
        let listener = callback.as_obj();

        let result = invoke_listener(env, listener, nodes);
        if let Err(error) = result {
            throw_illegal_state_exception(&env, &error);
        }
    };
    let callback = Arc::new(callback);
    App::current().accept_message(AppMsg::RegisterAvailableNodesListener(callback));
}

fn invoke_listener(env: JNIEnv, listener: JObject, nodes: &[NodeInfo]) -> anyhow::Result<()> {
    let nodes_json = serde_json::to_string(nodes)?;
    let args: [JValue; 1] = [env.new_string(&nodes_json)?.into()];
    env.call_method(listener, "accept", "(Ljava/lang/String;)V", &args)
        .unwrap();
    Ok(())
}

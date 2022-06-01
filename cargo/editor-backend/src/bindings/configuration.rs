use std::sync::Arc;

use jni::objects::{JClass, JObject, JString, JValue};
use jni::JNIEnv;

use crate::model::configuration::IOConfiguration;
use crate::{unwrap_or_throw, App, AppMsg};

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_ConfigurationService_registerListener(
    env: JNIEnv,
    _class: JClass,
    callback: JObject,
) {
    let vm = Arc::new(env.get_java_vm().unwrap());
    let callback = Arc::new(env.new_global_ref(callback).unwrap());
    let callback = move |config: &IOConfiguration| {
        let _attach_guard = vm.attach_current_thread().unwrap();
        let env = vm.get_env().unwrap();
        let listener = callback.as_obj();

        unwrap_or_throw!(env, invoke_listener(env, listener, config));
    };
    let callback = Arc::new(callback);
    App::current().accept_message(AppMsg::RegisterConfigurationListener(callback));
}

fn invoke_listener(env: JNIEnv, listener: JObject, config: &IOConfiguration) -> anyhow::Result<()> {
    let config_json = serde_json::to_string(config)?;
    let args: [JValue; 1] = [env.new_string(&config_json)?.into()];
    env.call_method(listener, "accept", "(Ljava/lang/String;)V", &args)
        .unwrap();
    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_ConfigurationService_changeCurrentOutput(
    env: JNIEnv,
    _class: JClass,
    output: JString,
) {
    let output: String = unwrap_or_throw!(env, env.get_string(output)).into();
    let msg = AppMsg::ChangeCurrentOutput {
        output: Some(output),
    };
    App::current().accept_message(msg)
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_ConfigurationService_refresh(
    _env: JNIEnv,
    _class: JClass,
) {
    App::current().accept_message(AppMsg::RefreshConfiguration)
}

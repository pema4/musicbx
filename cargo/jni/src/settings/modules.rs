use std::sync::Arc;

use jni::objects::{JClass, JObject, JValue};
use jni::JNIEnv;
use lazy_static::lazy_static;
use serde::{Deserialize, Serialize};

use crate::modules::{ModuleDescription, ModuleInfo};
use crate::modules::{OutputModule, SinModule};
use crate::{App, AppMsg};

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_AvailableModulesService_registerListener(
    env: JNIEnv,
    _class: JClass,
    callback: JObject,
) {
    let vm = Arc::new(env.get_java_vm().unwrap());
    let callback = Arc::new(env.new_global_ref(callback).unwrap());

    let listener = move |modules: &[ModuleInfo]| {
        let _attach_guard = vm.attach_current_thread().unwrap();
        let env = vm.get_env().unwrap();
        let listener = callback.as_obj();

        // std::thread::sleep(std::time::Duration::from_secs(2));

        let modules_json = serde_json::to_string(modules).unwrap();
        let args: [JValue; 1] = [env.new_string(&modules_json).unwrap().into()];
        env.call_method(listener, "accept", "(Ljava/lang/String;)V", &args)
            .unwrap();
    };
    let listener = Arc::new(listener);

    App::current().accept_message(AppMsg::RegisterAvailableModulesListener(listener));
    // App::current().add_available_modules_listener(Arc::new(listener));
}

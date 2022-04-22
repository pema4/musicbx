use std::ops::Deref;
use std::sync::Arc;
use std::thread::sleep;
use std::time::Duration;

use jni::objects::{JClass, JObject, JString, JValue};
use jni::JNIEnv;
use lazy_static::lazy_static;
use serde::de::Unexpected::Str;
use serde_json::json;

use crate::configuration::{Configuration, OutputConfiguration, SampleRateConfiguration};
use crate::model::Module;

mod configuration;
mod model;

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_start(
    _env: JNIEnv,
    _class: JClass,
) {
    println!("Backend started");
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_addModule(
    env: JNIEnv,
    _class: JClass,
    module_json: JString,
) {
    let module_json = env.get_string(module_json).unwrap();
    let module_json_bytes: &[u8] = module_json.to_bytes();
    let module: serde_json::Result<Module> = serde_json::from_slice(module_json_bytes);
    println!("{:?}", module);
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_removeModule(
    env: JNIEnv,
    _class: JClass,
    module_json: JString,
) {
    let module_json = env.get_string(module_json).unwrap();
    let module_json_bytes: &[u8] = module_json.to_bytes();
    let module: serde_json::Result<Module> = serde_json::from_slice(module_json_bytes);
    println!("{:?}", module);
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_ConfigurationService_registerListener(
    env: JNIEnv,
    _class: JClass,
    listener: JObject,
) {
    let vm = Arc::new(env.get_java_vm().unwrap());
    let listener = Arc::new(env.new_global_ref(listener).unwrap());

    std::thread::spawn({
        let vm = vm.clone();
        let listener = listener.clone();
        move || {
            let _attach_guard = vm.attach_current_thread().unwrap();
            let env = vm.get_env().unwrap();
            let listener = listener.as_obj();

            // sleep(Duration::from_secs(1));

            let configuration: &Configuration = &TEST_CONFIG;
            let config_json = serde_json::to_string(configuration).unwrap();
            let args: [JValue; 1] = [env.new_string(&config_json).unwrap().into()];
            env.call_method(listener, "accept", "(Ljava/lang/String;)V", &args)
                .unwrap();
        }
    });
}

lazy_static! {
    static ref TEST_CONFIG: Configuration = Configuration {
        output: OutputConfiguration {
            current: "Default".into(),
            available: vec!["Default".into(), "Headphones".into(),],
            sample_rate: SampleRateConfiguration {
                current: 44100.0,
                available: vec![44100.0, 88200.0]
            }
        }
    };
}

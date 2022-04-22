use std::sync::Arc;

use jni::objects::{JClass, JObject, JString, JValue};
use jni::JNIEnv;
use lazy_static::lazy_static;

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

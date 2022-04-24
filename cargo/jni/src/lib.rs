use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jint;

use crate::app::{App, AppMsg};

pub mod app;
pub mod modules;
pub mod patch;
pub mod settings;
mod util;

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_start(
    _env: JNIEnv,
    _class: JClass,
) {
    App::current().accept_message(AppMsg::Start);
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_stop(
    _env: JNIEnv,
    _class: JClass,
) {
    App::current().accept_message(AppMsg::Stop);
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_addModule(
    env: JNIEnv,
    _class: JClass,
    uid: JString,
    id: jint,
) {
    let uid = env.get_string(uid).unwrap();
    let uid = String::from(uid);
    let id: usize = usize::try_from(id).unwrap();

    App::current().accept_message(AppMsg::AddModule { uid, id });
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_removeModule(
    _env: JNIEnv,
    _class: JClass,
    id: jint,
) {
    let id: usize = id.try_into().unwrap();

    App::current().accept_message(AppMsg::RemoveModule { id });
}

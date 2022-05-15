use std::fmt::Display;

use jni::JNIEnv;

mod available_nodes;
mod configuration;
mod editor;

fn throw_illegal_state_exception(env: &JNIEnv, msg: &impl Display) {
    env.throw_new("java/lang/IllegalArgumentException", msg.to_string())
        .unwrap();
}

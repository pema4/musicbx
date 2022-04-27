use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jfloat, jint};

use crate::app::{App, AppMsg};
use crate::patch::{Cable, CableEnd};
use crate::util::throw_illegal_state_exception;

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_reset(
    _env: JNIEnv,
    _class: JClass,
) {
    App::current().accept_message(AppMsg::Reset);
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_addModule(
    env: JNIEnv,
    _class: JClass,
    uid: JString,
    id: jint,
) {
    let result = add_module(env, uid, id);

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn add_module(env: JNIEnv, uid: JString, id: jint) -> anyhow::Result<()> {
    let uid = env.get_string(uid)?;
    let uid = String::from(uid);
    let id = usize::try_from(id)?;

    App::current().accept_message(AppMsg::AddModule { uid, id });

    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_removeModule(
    env: JNIEnv,
    _: JClass,
    id: jint,
) {
    let result = remove_module(id);

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn remove_module(id: jint) -> anyhow::Result<()> {
    let id: usize = id.try_into()?;

    App::current().accept_message(AppMsg::RemoveModule { id });

    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_connectModules(
    env: JNIEnv,
    _: JClass,
    from_module_id: jint,
    from_socket_number: jint,
    to_module_id: jint,
    to_socket_number: jint,
) {
    let result = connect_modules(
        from_module_id,
        from_socket_number,
        to_module_id,
        to_socket_number,
    );

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn connect_modules(
    from_module: jint,
    from_output: jint,
    to_module: jint,
    to_output: jint,
) -> anyhow::Result<()> {
    let msg = AppMsg::AddCable(cable(from_module, from_output, to_module, to_output)?);
    App::current().accept_message(msg);
    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_disconnectModules(
    env: JNIEnv,
    _: JClass,
    from_module_id: jint,
    from_socket_number: jint,
    to_module_id: jint,
    to_socket_number: jint,
) {
    let result = disconnect_modules(
        from_module_id,
        from_socket_number,
        to_module_id,
        to_socket_number,
    );

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn disconnect_modules(
    from_module: jint,
    from_output: jint,
    to_module: jint,
    to_output: jint,
) -> anyhow::Result<()> {
    let msg = AppMsg::RemoveCable(cable(from_module, from_output, to_module, to_output)?);
    App::current().accept_message(msg);
    Ok(())
}

fn cable(
    from_module: jint,
    from_output: jint,
    to_module: jint,
    to_output: jint,
) -> anyhow::Result<Cable> {
    Ok(Cable {
        from: CableEnd {
            module: from_module.try_into()?,
            socket: from_output.try_into()?,
        },
        to: CableEnd {
            module: to_module.try_into()?,
            socket: to_output.try_into()?,
        },
    })
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_PlaybackService_setParameter(
    env: JNIEnv,
    _: JClass,
    module_id: jint,
    parameter_index: jint,
    value: jfloat,
) {
    let result = set_parameter(module_id, parameter_index, value);

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn set_parameter(module_id: jint, parameter_index: jint, value: jfloat) -> anyhow::Result<()> {
    let msg = AppMsg::SetParameter {
        id: module_id.try_into()?,
        index: parameter_index.try_into()?,
        value: value.into(),
    };
    App::current().accept_message(msg);
    Ok(())
}

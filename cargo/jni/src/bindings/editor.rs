use jni::objects::{JClass, JString};
use jni::sys::{jfloat, jint};
use jni::JNIEnv;

use crate::app::{App, AppMsg};
use crate::bindings::throw_illegal_state_exception;
use crate::patch::{Cable, CableEnd};

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_reset(
    _env: JNIEnv,
    _class: JClass,
) {
    App::current().accept_message(AppMsg::Reset);
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_addNodeOnBackend(
    env: JNIEnv,
    _class: JClass,
    uid: JString,
    id: jint,
) {
    let result = add_node(env, uid, id);

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn add_node(env: JNIEnv, uid: JString, id: jint) -> anyhow::Result<()> {
    let uid = env.get_string(uid)?;
    let uid = String::from(uid);
    let id = usize::try_from(id)?;

    App::current().accept_message(AppMsg::AddNode { uid, id });

    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_removeNode(
    env: JNIEnv,
    _: JClass,
    id: jint,
) {
    let result = remove_node(id);

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn remove_node(id: jint) -> anyhow::Result<()> {
    let id: usize = id.try_into()?;

    App::current().accept_message(AppMsg::RemoveNode { id });

    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_connectNodes(
    env: JNIEnv,
    _: JClass,
    from_node_id: jint,
    from_socket_number: jint,
    to_node_id: jint,
    to_socket_number: jint,
) {
    let result = connect_nodes(
        from_node_id,
        from_socket_number,
        to_node_id,
        to_socket_number,
    );

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn connect_nodes(
    from_node: jint,
    from_output: jint,
    to_node: jint,
    to_output: jint,
) -> anyhow::Result<()> {
    let msg = AppMsg::AddCable(cable(from_node, from_output, to_node, to_output)?);
    App::current().accept_message(msg);
    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_disconnectNodes(
    env: JNIEnv,
    _: JClass,
    from_node_id: jint,
    from_socket_number: jint,
    to_node_id: jint,
    to_socket_number: jint,
) {
    let result = disconnect_nodes(
        from_node_id,
        from_socket_number,
        to_node_id,
        to_socket_number,
    );

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn disconnect_nodes(
    from_node: jint,
    from_output: jint,
    to_node: jint,
    to_output: jint,
) -> anyhow::Result<()> {
    let msg = AppMsg::RemoveCable(cable(from_node, from_output, to_node, to_output)?);
    App::current().accept_message(msg);
    Ok(())
}

fn cable(
    from_node: jint,
    from_output: jint,
    to_node: jint,
    to_output: jint,
) -> anyhow::Result<Cable> {
    Ok(Cable {
        from: CableEnd {
            node: from_node.try_into()?,
            socket: from_output.try_into()?,
        },
        to: CableEnd {
            node: to_node.try_into()?,
            socket: to_output.try_into()?,
        },
    })
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_setParameter(
    env: JNIEnv,
    _: JClass,
    node_id: jint,
    parameter_index: jint,
    value: jfloat,
) {
    let result = set_parameter(node_id, parameter_index, value);

    if let Err(error) = result {
        throw_illegal_state_exception(&env, &error);
    }
}

fn set_parameter(node_id: jint, parameter_index: jint, value: jfloat) -> anyhow::Result<()> {
    let msg = AppMsg::SetParameter {
        id: node_id.try_into()?,
        index: parameter_index.try_into()?,
        value,
    };
    App::current().accept_message(msg);
    Ok(())
}

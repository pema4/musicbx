use jni::objects::{JClass, JString};
use jni::sys::{jfloat, jint};
use jni::JNIEnv;

use musicbx::types::patch::{Cable, CableEnd};

use crate::app::{App, AppMsg};
use crate::unwrap_or_throw;

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
    let uid = unwrap_or_throw!(env, env.get_string(uid));
    let uid = String::from(uid);
    let id = unwrap_or_throw!(env, usize::try_from(id));
    App::current().accept_message(AppMsg::AddNode { uid, id });
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_removeNode(
    env: JNIEnv,
    _: JClass,
    id: jint,
) {
    let id: usize = unwrap_or_throw!(env, id.try_into());
    App::current().accept_message(AppMsg::RemoveNode { id });
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_connectNodes(
    env: JNIEnv,
    _: JClass,
    from_node_id: jint,
    from_socket_name: JString,
    to_node_id: jint,
    to_socket_name: JString,
) {
    let result = connect_nodes(
        env,
        from_node_id,
        from_socket_name,
        to_node_id,
        to_socket_name,
    );

    unwrap_or_throw!(env, result);
}

fn connect_nodes(
    env: JNIEnv,
    from_node: jint,
    from_output: JString,
    to_node: jint,
    to_input: JString,
) -> anyhow::Result<()> {
    let msg = AppMsg::AddCable(Cable {
        from: CableEnd {
            node_id: from_node.try_into()?,
            socket_name: env.get_string(from_output)?.into(),
        },
        to: CableEnd {
            node_id: to_node.try_into()?,
            socket_name: env.get_string(to_input)?.into(),
        },
    });
    App::current().accept_message(msg);
    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_disconnectNodes(
    env: JNIEnv,
    _: JClass,
    from_node_id: jint,
    from_socket_name: JString,
    to_node_id: jint,
    to_socket_name: JString,
) {
    let result = disconnect_nodes(
        env,
        from_node_id,
        from_socket_name,
        to_node_id,
        to_socket_name,
    );

    unwrap_or_throw!(env, result);
}

fn disconnect_nodes(
    env: JNIEnv,
    from_node: jint,
    from_output: JString,
    to_node: jint,
    to_input: JString,
) -> anyhow::Result<()> {
    let msg = AppMsg::RemoveCable(Cable {
        from: CableEnd {
            node_id: from_node.try_into()?,
            socket_name: env.get_string(from_output)?.into(),
        },
        to: CableEnd {
            node_id: to_node.try_into()?,
            socket_name: env.get_string(to_input)?.into(),
        },
    });
    App::current().accept_message(msg);
    Ok(())
}

#[no_mangle]
pub extern "system" fn Java_ru_pema4_musicbx_service_EditorService_setParameter(
    env: JNIEnv,
    _: JClass,
    node_id: jint,
    parameter_index: jint,
    value: jfloat,
) {
    let msg = AppMsg::SetParameter {
        id: unwrap_or_throw!(env, node_id.try_into()),
        index: unwrap_or_throw!(env, parameter_index.try_into()),
        value,
    };
    App::current().accept_message(msg);
}

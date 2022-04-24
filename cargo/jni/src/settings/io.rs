use std::sync::Arc;

use jni::objects::{JClass, JObject, JValue};
use jni::JNIEnv;
use lazy_static::lazy_static;
use serde::{Deserialize, Serialize};

#[derive(PartialEq, Debug, Serialize, Deserialize)]
pub struct IOConfiguration {
    pub output: OutputConfiguration,
}

#[derive(PartialEq, Debug, Serialize, Deserialize)]
pub struct OutputConfiguration {
    pub current: String,
    pub available: Vec<String>,
    pub sample_rate: SampleRateConfiguration,
}

#[derive(PartialEq, Debug, Serialize, Deserialize)]
pub struct SampleRateConfiguration {
    pub current: f64,
    pub available: Vec<f64>,
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
        // let vm = vm.clone();
        // let listener = listener.clone();
        move || {
            let _attach_guard = vm.attach_current_thread().unwrap();
            let env = vm.get_env().unwrap();
            let listener = listener.as_obj();

            // sleep(Duration::from_secs(1));

            let configuration: &IOConfiguration = &TEST_CONFIG;
            let config_json = serde_json::to_string(configuration).unwrap();
            let args: [JValue; 1] = [env.new_string(&config_json).unwrap().into()];
            env.call_method(listener, "accept", "(Ljava/lang/String;)V", &args)
                .unwrap();
        }
    });
}

lazy_static! {
    static ref TEST_CONFIG: IOConfiguration = IOConfiguration {
        output: OutputConfiguration {
            current: "Default".into(),
            available: vec!["Default".into(), "Headphones".into()],
            sample_rate: SampleRateConfiguration {
                current: 44100.0,
                available: vec![44100.0, 88200.0]
            }
        }
    };
}

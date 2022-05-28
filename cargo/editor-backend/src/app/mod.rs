use std::sync::mpsc::Sender;
use std::sync::{mpsc, Arc, Mutex};
use std::thread;

use lazy_static::lazy_static;

use musicbx::types::patch::Cable;

use crate::app::state::AppState;
use crate::model::configuration::IOConfiguration;
use crate::nodes::NodeInfo;

mod delegate;
mod state;

pub struct App {
    state: Arc<Mutex<AppState>>,
    message_sender: Mutex<Sender<AppMsg>>,
}

impl App {
    pub fn current() -> &'static App {
        &APP
    }

    pub fn new() -> App {
        let (sender, receiver) = mpsc::channel();
        let state = AppState::new();

        let app = App {
            message_sender: Mutex::new(sender),
            state: Arc::new(Mutex::new(state)),
        };

        thread::spawn({
            let state = app.state.clone();
            move || {
                // блокировка берётся только один раз
                let state = &mut state.lock().unwrap();
                for msg in receiver {
                    state
                        .update(&msg)
                        .unwrap_or_else(|err| eprintln!("Got error {err}"));
                }
            }
        });

        app
    }

    pub fn accept_message(&self, msg: AppMsg) {
        let sender = self.message_sender.lock().unwrap();
        sender.send(msg).unwrap()
    }
}

// внутри _audio_stream лежит сырой указатель, для которого нет типажа Send
// при этом мы обещаем, что с этим указателем будет работать только один поток
unsafe impl Send for AppState {}

#[derive(Clone)]
pub enum AppMsg {
    Reset,
    AddNode { uid: String, id: usize },
    RemoveNode { id: usize },
    AddCable(Cable),
    RemoveCable(Cable),
    ChangeCurrentOutput { output: Option<String> },
    RegisterAvailableNodesListener(Arc<dyn Fn(&[&NodeInfo])>),
    RegisterConfigurationListener(Arc<dyn Fn(&IOConfiguration)>),
    SetParameter { id: usize, index: u8, value: f32 },
    RefreshConfiguration,
}

unsafe impl Send for AppMsg {}

impl Default for App {
    fn default() -> Self {
        App::new()
    }
}

lazy_static! {
    static ref APP: App = {
        let app = App::new();
        app.accept_message(AppMsg::ChangeCurrentOutput { output: None });
        app
    };
}

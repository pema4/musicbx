use std::collections::HashMap;
use std::sync::Arc;

use cpal::traits::{DeviceTrait, HostTrait};

use musicbx::types::patch::Cable;

use crate::app::delegate::{AppDelegate, CpalAppDelegate};
use crate::model::configuration::IOConfiguration;
use crate::nodes::{
    AddNodeDescription, AmpNodeDescription, HardClipNodeDescription, MulNodeDescription, Node,
    NodeFactory, NodeInfo, NoiseNodeDescription, OutputNodeDescription, SimpleSawNodeDescription,
    SinNodeDescription, TestFmNodeDescription,
};
use crate::util::Observable;
use crate::{App, AppMsg};

#[derive(Default)]
pub struct AppState {
    delegate: Box<dyn AppDelegate>,
    available_nodes: Observable<Vec<Box<dyn NodeFactory>>>,
    configuration: Observable<IOConfiguration>,
    nodes: HashMap<usize, Box<dyn Node>>,
    cables: Vec<Cable>,
    parameters: HashMap<(usize, u8), f32>,
}

impl AppState {
    pub fn new() -> AppState {
        AppState {
            available_nodes: Observable::new(available_nodes()),
            ..Default::default()
        }
    }

    pub fn update(&mut self, msg: &AppMsg) -> anyhow::Result<()> {
        match msg {
            AppMsg::Reset => self.reset(),
            AppMsg::AddNode { uid, id } => self.add_node(uid, *id),
            AppMsg::RemoveNode { id } => self.remove_node(*id),
            AppMsg::AddCable(cable) => self.add_cable(cable),
            AppMsg::RemoveCable(cable) => self.remove_cable(cable),
            AppMsg::ChangeCurrentOutput { output } => {
                let output = output.as_ref().map(|s| s.as_str());
                self.change_current_output(output)?
            }
            AppMsg::RegisterAvailableNodesListener(listener) => {
                self.add_available_nodes_listener(listener.clone())
            }
            AppMsg::RegisterConfigurationListener(listener) => {
                self.add_configuration_listener(listener.clone())
            }
            AppMsg::SetParameter { id, index, value } => self.set_parameter(*id, *index, *value),
            AppMsg::RefreshConfiguration => self.refresh_configuration()?,
        };
        Ok(())
    }

    fn reset(&mut self) {
        self.nodes.clear();
        self.cables.clear();
        self.parameters.clear();
        self.recreate_context();
    }

    fn add_node(&mut self, uid: &str, id: usize) {
        let node_desc = self.available_nodes.data.iter().find(|x| x.uid() == uid);

        if let Some(node_desc) = node_desc {
            let mut node = node_desc.create_instance(id);
            self.delegate.add_node(node.as_mut());
            self.nodes.insert(id, node);
        } else {
            eprintln!("Node description with uid {uid} not found");
        }
    }

    fn remove_node(&mut self, id: usize) {
        self.nodes.remove(&id);
        self.cables
            .retain(|x| x.from.node_id != id && x.to.node_id != id);
        self.recreate_context();
    }

    fn add_cable(&mut self, cable: &Cable) {
        self.cables.push(cable.clone());
        self.do_connect_nodes(cable);
    }

    fn remove_cable(&mut self, cable: &Cable) {
        self.cables.retain(|x| x != cable);
        self.recreate_context();
    }

    pub fn add_available_nodes_listener(&mut self, listener: Arc<dyn Fn(&[&NodeInfo])>) {
        let listener = move |node_descriptions: &Vec<Box<dyn NodeFactory + 'static>>| {
            let node_infos: Vec<_> = node_descriptions.iter().map(|x| x.info()).collect();
            listener(&node_infos);
        };
        let listener = Arc::new(listener);

        self.available_nodes.add_listener(listener);
    }

    pub fn add_configuration_listener(&mut self, listener: Arc<dyn Fn(&IOConfiguration)>) {
        self.configuration.add_listener(listener);
    }

    fn recreate_context(&mut self) {
        self.delegate.reset();

        for node in self.nodes.values_mut() {
            self.delegate.add_node(node.as_mut());
        }

        for cable in &self.cables {
            self.do_connect_nodes(cable);
        }

        for ((id, index), value) in &self.parameters {
            let node = self.nodes.get(id);
            if let Some(node) = node {
                self.delegate.set_parameter(node.as_ref(), *index, *value);
            }
        }
    }

    fn do_connect_nodes(&self, cable: &Cable) {
        let from = &self.nodes[&cable.from.node_id];
        let to = &self.nodes[&cable.to.node_id];

        self.delegate.connect_nodes(
            from.as_ref(),
            &cable.from.socket_name,
            to.as_ref(),
            &cable.to.socket_name,
        );
    }

    fn set_parameter(&mut self, node_id: usize, index: u8, value: f32) {
        let node = self.nodes.get(&node_id).unwrap();
        self.parameters.insert((node_id, index), value);
        self.delegate.set_parameter(node.as_ref(), index, value);
    }

    fn change_current_output(&mut self, output: Option<&str>) -> anyhow::Result<()> {
        let error_callback = |err| {
            eprintln!("Got stream error {err}");
            App::current().accept_message(AppMsg::RefreshConfiguration);
        };

        let new_context = CpalAppDelegate::builder()
            .output_name(output)
            .on_error(error_callback)
            .build()?;

        self.delegate = Box::new(new_context);
        self.refresh_configuration()?;
        self.recreate_context();

        Ok(())
    }

    fn refresh_configuration(&mut self) -> anyhow::Result<()> {
        let output_configuration = {
            let mut config = self.delegate.output_configuration();

            let host = cpal::default_host();
            config.available = host
                .output_devices()?
                .filter_map(|x| x.name().ok())
                .collect();

            config
        };

        self.configuration.data = IOConfiguration {
            output: output_configuration,
        };
        self.configuration.notify_all();

        Ok(())
    }
}

fn available_nodes() -> Vec<Box<dyn NodeFactory>> {
    vec![
        Box::new(AddNodeDescription),
        Box::new(AmpNodeDescription),
        Box::new(MulNodeDescription),
        Box::new(NoiseNodeDescription),
        Box::new(OutputNodeDescription),
        Box::new(SimpleSawNodeDescription),
        Box::new(SinNodeDescription),
        Box::new(TestFmNodeDescription),
        Box::new(HardClipNodeDescription),
    ]
}

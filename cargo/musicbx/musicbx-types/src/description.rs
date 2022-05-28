use std::ops::Deref;

use serde::Serialize;

use crate::parameter::NodeParameterKind;

pub trait ModuleDefinition {
    fn info_for_uid(&self, uid: &str) -> Option<&NodeDefinition>;
}

impl ModuleDefinition for &[NodeDefinition] {
    fn info_for_uid(&self, uid: &str) -> Option<&NodeDefinition> {
        self.iter().find(|x| x.uid == uid)
    }
}

impl<T: ModuleDefinition> ModuleDefinition for &[T] {
    fn info_for_uid(&self, uid: &str) -> Option<&NodeDefinition> {
        self.iter().filter_map(|x| x.info_for_uid(uid)).next()
    }
}

impl ModuleDefinition for Box<dyn ModuleDefinition> {
    fn info_for_uid(&self, uid: &str) -> Option<&NodeDefinition> {
        self.deref().info_for_uid(uid)
    }
}

#[derive(PartialEq, Eq, Debug, Serialize, Default, Clone)]
pub struct NodeDefinition {
    pub uid: &'static str,
    pub inputs: &'static [NodeInput],
    pub outputs: &'static [NodeOutput],
    pub parameters: &'static [NodeParameter],
}

#[derive(PartialEq, Eq, Debug, Serialize, Clone)]
pub struct NodeInput {
    pub number: usize,
    pub name: &'static str,
}

#[derive(PartialEq, Eq, Debug, Serialize, Clone)]
pub struct NodeOutput {
    pub number: usize,
    pub name: &'static str,
}

#[derive(PartialEq, Eq, Debug, Serialize, Clone)]
pub struct NodeParameter {
    pub number: usize,
    pub kind: NodeParameterKind,
    pub default: &'static str,
    pub name: &'static str,
}

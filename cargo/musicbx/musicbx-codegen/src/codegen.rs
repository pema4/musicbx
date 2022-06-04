use std::collections::{HashMap, HashSet};
use std::path::{Path, PathBuf};
use std::{fs, io};

use itertools::Itertools;
use proc_macro2::{Ident, Span, TokenStream};
use quote::{format_ident, quote};
use thiserror::Error;

use musicbx_types::patch::{Cable, Node, Patch};
use musicbx_types::ModuleDefinition;

#[derive(Default)]
pub struct MusicbxCodegen {
    modules: Vec<Box<dyn ModuleDefinition>>,
    inputs: Vec<PathBuf>,
    output_dir: Option<PathBuf>,
}

#[derive(Debug, Error)]
#[error(transparent)]
pub enum MusicbxCodegenError {
    #[error("No output dir")]
    NoOutDir,
    ExecutionError(#[from] MusicbxExecutionError),
}

#[derive(Debug, Error)]
#[error(transparent)]
pub enum MusicbxExecutionError {
    IoError(#[from] io::Error),

    InvalidJsonFormat(#[from] serde_json::Error),

    #[error("Invalid input file name: {0}")]
    InvalidInputFileName(String),

    InvalidNodeUid(#[from] InvalidNodeUid),

    #[error("Unknown parameter name {0} for node with uid {1}")]
    UnknownParameterName(String, String),

    #[error("Invalid parameter value: {0}")]
    InvalidParameterValue(String),

    #[error("Should not really happen")]
    InvalidState,
}

impl MusicbxCodegen {
    pub fn new() -> Self {
        MusicbxCodegen {
            modules: vec![],
            inputs: vec![],
            output_dir: None,
        }
    }

    pub fn module<M>(mut self, module: &M) -> Self
    where
        M: ModuleDefinition + Clone + 'static,
    {
        self.modules.push(Box::new(module.clone()));
        self
    }

    pub fn output_dir(self, path: &Path) -> Self {
        MusicbxCodegen {
            output_dir: Some(path.to_owned()),
            ..self
        }
    }

    pub fn inputs(self, paths: &[&str]) -> Self {
        MusicbxCodegen {
            inputs: paths.iter().map(PathBuf::from).collect(),
            ..self
        }
    }

    pub fn run(self) -> Result<(), MusicbxCodegenError> {
        let MusicbxCodegen {
            output_dir: out_dir,
            inputs,
            ..
        } = &self;

        let out_dir = out_dir.as_ref().ok_or(MusicbxCodegenError::NoOutDir)?;

        for input in inputs {
            let mut out_file = out_dir.clone();
            out_file.push({
                let input_file = extract_name_from_input_file(&input)?;
                format!("{input_file}.rs")
            });
            self.generate(input, &out_file)?;
        }

        Ok(())
    }

    fn generate(&self, input: &Path, output: &Path) -> Result<(), MusicbxExecutionError> {
        let node_description = fs::read(input)?;
        let Patch { nodes, cables } = serde_json::from_slice(&node_description[..])?;

        let node_name = extract_name_from_input_file(input)?;
        let node_name = Ident::new(node_name, Span::call_site());

        let nodes: HashMap<usize, Node> = nodes.into_iter().map(|node| (node.id, node)).collect();

        let route_declarations = declare_routing(&nodes, cables.iter())?;
        let field_declarations = declare_node_fields(nodes.values())?;

        let parameters: HashSet<(usize, &str, String)> =
            self.extract_parameters(&nodes, &cables[..])?;
        let parameter_field_declarations: Vec<TokenStream> = parameters
            .iter()
            .map(|(node_id, param_name, param_value)| {
                let param_ident = format_ident!("{}_{param_name}", node_ident(*node_id));
                let param_value: f32 = param_value.parse().unwrap();
                quote! {
                    #[from(musicbx::std::util::ConstSig::new(#param_value))]
                    #param_ident: musicbx::std::util::ConstSig
                }
            })
            .collect();
        let parameter_route_declarations: Vec<TokenStream> = parameters
            .iter()
            .map(|(node_id, param_name, _)| {
                let param_ident = format_ident!("{}_{param_name}", node_ident(*node_id));
                let node_ident = node_ident(*node_id);
                let node_input = format_ident!("{param_name}");
                quote! { #param_ident.output -> #node_ident.#node_input }
            })
            .collect();

        let generated_code = quote! {
            #[musicbx::node {
                #( #route_declarations, )*
                #( #parameter_route_declarations, )*
            }]
            #[derive(musicbx::FromSampleRate)]
            pub struct #node_name {
                #( #field_declarations, )*
                #( #parameter_field_declarations, )*
            }
        };

        fs::write(output, generated_code.to_string())?;

        Ok(())
    }

    fn extract_parameters<'a>(
        &self,
        nodes: &'a HashMap<usize, Node>,
        routes: &'a [Cable],
    ) -> Result<HashSet<(usize, &'a str, String)>, MusicbxExecutionError> {
        let overridden_parameters: HashSet<(usize, &str)> = routes
            .iter()
            .map(|cable| &cable.to)
            .map(|cable_end| (cable_end.node_id, cable_end.socket_name.as_str()))
            .collect();

        nodes
            .values()
            .flat_map(|node| {
                node.parameters
                    .iter()
                    .map(|(name, value)| (node.id, name.as_str(), value.as_str()))
            })
            .filter(|(node_id, name, _)| !overridden_parameters.contains(&(*node_id, name)))
            .map(|(node_id, param_name, param_value)| {
                let node = &nodes[&node_id];
                let node_info = self
                    .modules
                    .as_slice()
                    .info_for_uid(&node.uid)
                    .ok_or_else(|| InvalidNodeUid(node.uid.to_string()))
                    .cloned()?;
                let param_kind = node_info
                    .parameters
                    .iter()
                    .find(|x| x.name == param_name)
                    .map(|x| &x.kind)
                    .ok_or_else(|| {
                        MusicbxExecutionError::UnknownParameterName(
                            param_name.to_string(),
                            node.uid.to_string(),
                        )
                    })?;
                let param_value: f32 = param_value.parse().map_err(|_| {
                    MusicbxExecutionError::InvalidParameterValue(param_value.to_string())
                })?;

                let param_value = param_kind.denormalize(param_value);
                Ok((node_id, param_name, param_value.to_string()))
            })
            .collect()
    }
}

fn extract_name_from_input_file(input: &Path) -> Result<&str, MusicbxExecutionError> {
    input
        .file_name()
        .and_then(|x| x.to_str())
        .and_then(|x| x.split('.').next())
        .ok_or_else(|| {
            let input_name = format!("{input:?}");
            MusicbxExecutionError::InvalidInputFileName(input_name)
        })
}

fn declare_node_fields<'a>(
    nodes: impl IntoIterator<Item = &'a Node>,
) -> Result<Vec<TokenStream>, MusicbxExecutionError> {
    nodes
        .into_iter()
        .map(|x| {
            let name = node_ident(x.id);
            let ty = get_node_type(&x.uid)?;

            Ok(if let NodeType::Node(ty) = ty {
                Some(quote! { #name : #ty })
            } else {
                None
            })
        })
        .filter_map_ok(|x| x)
        .collect::<Result<_, _>>()
}

fn declare_route(
    from: &Node,
    from_output: &str,
    to: &Node,
    to_input: &str,
) -> Result<TokenStream, MusicbxExecutionError> {
    let from_output_ident = format_ident!("{from_output}");
    let from_part: TokenStream = match get_node_type(&from.uid)? {
        NodeType::Output => Err(MusicbxExecutionError::InvalidState)?,
        NodeType::Input => quote! { #from_output_ident },
        NodeType::Node(_) => {
            let from_ident = node_ident(from.id);
            quote! { #from_ident . #from_output_ident }
        }
    };

    let to_input_ident = format_ident!("{to_input}");
    let to_part: TokenStream = match get_node_type(&to.uid)? {
        NodeType::Output => quote! { output },
        NodeType::Input => Err(MusicbxExecutionError::InvalidState)?,
        NodeType::Node(_) => {
            let to_ident = node_ident(to.id);
            quote! { #to_ident . #to_input_ident }
        }
    };

    Ok(quote! {#from_part -> #to_part })
}

fn declare_routing<'a>(
    nodes: &HashMap<usize, Node>,
    cables: impl IntoIterator<Item = &'a Cable>,
) -> Result<Vec<TokenStream>, MusicbxExecutionError> {
    cables
        .into_iter()
        .map(|Cable { from, to }| {
            let from_node = &nodes[&from.node_id];
            let to_node = &nodes[&to.node_id];

            declare_route(from_node, &from.socket_name, to_node, &to.socket_name)
        })
        .collect::<Result<_, _>>()
}

fn node_ident(id: usize) -> Ident {
    Ident::new(&format!("v{id}"), Span::call_site())
}

enum NodeType {
    Input,
    Output,
    Node(syn::Path),
}

#[derive(Debug, Error)]
#[error("Invalid node UID: {0}")]
pub struct InvalidNodeUid(String);

fn get_node_type(uid: &str) -> Result<NodeType, InvalidNodeUid> {
    Ok(match uid {
        "_synthetic_input" => NodeType::Input,
        "_synthetic_output" => NodeType::Output,
        uid => {
            let err_fn = || InvalidNodeUid(uid.to_string());

            let tokens: TokenStream = uid.parse().map_err(|_| err_fn())?;
            let ty = syn::parse2(tokens).map_err(|_| err_fn())?;

            NodeType::Node(ty)
        }
    })
}

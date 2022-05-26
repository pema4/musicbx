use std::collections::HashMap;
use std::ops::Deref;

use itertools::Itertools;
use petgraph::algo::toposort;
use petgraph::graph::NodeIndex;
use petgraph::Graph;
use proc_macro2::Span;
use quote::format_ident;
use quote::quote;
use syn::parse::{Parse, ParseStream};
use syn::punctuated::Punctuated;
use syn::{Data, DataStruct, DeriveInput, Ident, Path, Token};
use thiserror::Error;

#[derive(Clone)]
pub struct Routing {
    routes: Punctuated<Route, Token![,]>,
}

impl Parse for Routing {
    fn parse(input: ParseStream) -> syn::Result<Self> {
        Ok(Routing {
            routes: input.parse_terminated(Route::parse)?,
        })
    }
}

#[derive(Debug, Clone, Eq, PartialEq, Hash)]
struct Route {
    from: RouteEnd,
    to: RouteEnd,
}

impl Parse for Route {
    fn parse(input: ParseStream) -> syn::Result<Self> {
        let from = input.parse()?;
        input.parse::<Token![->]>()?;
        let to = input.parse()?;
        Ok(Route { from, to })
    }
}

#[derive(Debug, Clone, Eq, PartialEq, Hash)]
enum RouteEnd {
    Param(Ident),
    Inner(Ident, Ident),
}

impl Parse for RouteEnd {
    fn parse(input: ParseStream) -> syn::Result<Self> {
        let ident: Ident = input.parse()?;
        let dot: Option<Token![.]> = input.parse()?;

        let result = if dot.is_some() {
            RouteEnd::Inner(ident, input.parse()?)
        } else {
            RouteEnd::Param(ident)
        };

        Ok(result)
    }
}

#[derive(Error, Debug)]
enum CompositeNodeError {
    #[error("A cyclic graph is not supported")]
    CyclicGraph,
}

pub fn node_attribute_macro(input: DeriveInput, routing: Routing) -> proc_macro2::TokenStream {
    let input_struct = match &input.data {
        Data::Struct(x) => x,
        _ => panic!("Expected struct"),
    };

    let parameter_struct_definition = define_parameters_struct(&input, &routing);
    let impls = define_impls(&input, &input_struct, &routing);

    (quote! {
        #input
        #parameter_struct_definition
        #impls
    })
    .into()
}

fn define_parameters_struct(input: &DeriveInput, routing: &Routing) -> proc_macro2::TokenStream {
    let vis = &input.vis;
    let ident = &input.ident;

    let parameters_ident = format_ident!("{}Parameters", ident);
    let inputs: Vec<_> = routing
        .routes
        .iter()
        .filter_map(|route| match &route.from {
            RouteEnd::Param(param) => Some(param),
            _ => None,
        })
        .unique()
        .collect();

    let outputs: Vec<_> = routing
        .routes
        .iter()
        .filter_map(|route| match &route.to {
            RouteEnd::Param(param) => Some(param),
            _ => None,
        })
        .unique()
        .collect();

    quote! {
        #vis struct #parameters_ident<'a> {
            #(#vis #inputs: musicbx::DataRef<'a>, )*
            #(#vis #outputs: musicbx::DataMut<'a>, )*
        }

        impl Default for #parameters_ident<'static> {
            fn default() -> Self {
                Self {
                    #( #inputs: 0.0f32.into(), )*
                    #( #outputs: 0.0f32.into(), )*
                }
            }
        }
    }
}

fn define_impls(
    input: &DeriveInput,
    input_struct: &DataStruct,
    routing: &Routing,
) -> proc_macro2::TokenStream {
    let ident = &input.ident;
    let sorted_fields = fields_topo_sort(&input_struct, &routing).unwrap();

    let mut parts = Vec::new();

    let mut all_outputs = HashMap::new();
    for route in &routing.routes {
        if let RouteEnd::Param(x) = &route.from {
            all_outputs.insert(&route.from, x.clone());
        }

        if let RouteEnd::Inner(subnode, field) = &route.from {
            let out_name = Ident::new(&format!("__{subnode}_{field}"), Span::call_site());
            parts.push(quote! {
                let mut #out_name = [0.0f32; N];
            });
            all_outputs.insert(&route.from, out_name.clone());
        }
    }

    for subnode in sorted_fields {
        let subnode_parameters_type = input_struct
            .fields
            .iter()
            .find(|x| x.ident.as_ref() == Some(subnode))
            .map(|x| {
                let field_type = &x.ty;
                let mut path: Path = syn::parse_quote! { #field_type };

                if let Some(ident) = path.segments.last_mut() {
                    ident.ident =
                        Ident::new(&format!("{}Parameters", ident.ident), Span::call_site())
                }
                path
            })
            .expect("expected struct field");

        let inputs: Vec<_> = routing
            .routes
            .iter()
            .filter_map(|route| match &route.to {
                RouteEnd::Inner(x, y) if x == subnode => {
                    let input = match &route.from {
                        RouteEnd::Param(param) => quote! { #param },
                        inner @ RouteEnd::Inner(..) => {
                            let temp = &all_outputs[inner];
                            quote! { musicbx::DataRef::from(&#temp[..]) }
                        }
                    };
                    Some(quote! { #y: #input })
                }
                _ => None,
            })
            .collect();

        let outputs: Vec<_> = routing
            .routes
            .iter()
            .filter_map(|route| match &route.from {
                RouteEnd::Inner(x, y) if x == subnode => {
                    let output = match &route.to {
                        RouteEnd::Param(param) => quote! { #param },
                        RouteEnd::Inner(..) => {
                            let temp = &all_outputs[&route.from];
                            quote! { musicbx::DataMut::from(&mut #temp[..]) }
                        }
                    };
                    Some(quote! { #y: #output })
                }
                _ => None,
            })
            .collect();

        parts.push(quote! {
            self.#subnode.process::<N>(
                n,
                #subnode_parameters_type {
                    #( #inputs, )*
                    #( #outputs, )*
                    ..#subnode_parameters_type::default()
                });
        });
    }

    let mut inputs = Vec::new();
    let mut outputs = Vec::new();
    for route in &routing.routes {
        if let RouteEnd::Param(ident) = &route.from {
            inputs.push(ident.clone());
        }

        if let RouteEnd::Param(ident) = &route.to {
            outputs.push(ident.clone());
        }
    }
    inputs = inputs.into_iter().unique().collect();
    outputs = outputs.into_iter().unique().collect();

    let parameters_ident = format_ident!("{}Parameters", input.ident);

    quote! {
        impl #ident {
            #[allow(clippy::needless_update)]
            pub fn process<const N: usize>(
                &mut self,
                n: usize,
                parameters: #parameters_ident,
            ) {
                let #parameters_ident { #( #inputs, )* #( mut #outputs, )* .. } = parameters;
                #( #parts )*
            }
        }
    }
}

fn fields_topo_sort<'a>(
    input_struct: &'a DataStruct,
    routing: &'a Routing,
) -> Result<Vec<&'a Ident>, CompositeNodeError> {
    let input_ident = Ident::new("__input", Span::call_site());
    let output_ident = Ident::new("__output", Span::call_site());

    let fields: Vec<&Ident> = input_struct
        .fields
        .iter()
        .map(|x| x.ident.as_ref().unwrap())
        .collect();

    let mut graph = Graph::new();
    let mut node_indices: HashMap<&Ident, NodeIndex> = HashMap::new();
    for node in fields
        .iter()
        .map(Deref::deref)
        .chain([&input_ident, &output_ident].into_iter())
    {
        node_indices.insert(&node, graph.add_node(node));
    }

    for route in &routing.routes {
        let from = match &route.from {
            RouteEnd::Param(_) => &input_ident,
            RouteEnd::Inner(field, _) => &field,
        };
        let from = node_indices[from];

        let to = match &route.to {
            RouteEnd::Param(_) => &output_ident,
            RouteEnd::Inner(field, _) => &field,
        };
        let to = node_indices[to];

        graph.add_edge(from, to, ());
    }

    let order: HashMap<&Ident, usize> = toposort(&graph, None)
        .map_err(|_| CompositeNodeError::CyclicGraph)?
        .into_iter()
        .enumerate()
        .map(|(order, node_idx)| (graph[node_idx], order))
        .collect();

    let mut result: Vec<&Ident> = fields.clone();
    result.sort_by_key(|ident| order[ident]);
    Ok(result)
}

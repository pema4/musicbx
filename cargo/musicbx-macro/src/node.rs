use std::collections::HashMap;

use itertools::Itertools;
use petgraph::algo::toposort;
use petgraph::graph::NodeIndex;
use petgraph::Graph;
use proc_macro2::{Punct, Spacing, Span};
use quote::{format_ident, TokenStreamExt};
use quote::{quote, ToTokens};
use syn::parse::{Parse, ParseStream};
use syn::punctuated::Punctuated;
use syn::{braced, token, Generics, Ident, Token, Visibility};
use thiserror::Error;

struct NodeStruct {
    visibility: Visibility,
    _struct_token: Token![struct],
    ident: Ident,
    generics: Generics,
    _braces_token: token::Brace,
    fields: Punctuated<NodeField, Token![,]>,
}

impl Parse for NodeStruct {
    fn parse(input: ParseStream) -> syn::Result<Self> {
        let content;
        Ok(NodeStruct {
            visibility: input.parse()?,
            _struct_token: input.parse()?,
            ident: input.parse()?,
            generics: input.parse()?,
            _braces_token: braced!(content in input),
            fields: content.parse_terminated(NodeField::parse)?,
        })
    }
}

struct NodeField {
    visibility: Visibility,
    ident: Ident,
    _colon_token: Token![:],
    ty: syn::Path,
}

impl Parse for NodeField {
    fn parse(input: ParseStream) -> syn::Result<Self> {
        Ok(NodeField {
            visibility: input.parse()?,
            ident: input.parse()?,
            _colon_token: input.parse()?,
            ty: input.parse()?,
        })
    }
}

impl ToTokens for NodeField {
    fn to_tokens(&self, tokens: &mut proc_macro2::TokenStream) {
        self.visibility.to_tokens(tokens);
        tokens.append(Ident::new(&self.ident.to_string(), Span::call_site()));
        tokens.append(Punct::new(':', Spacing::Joint));
        self.ty.to_tokens(tokens);
    }
}

#[derive(Clone)]
struct Routing {
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

pub fn node_attribute_macro(
    attr: proc_macro::TokenStream,
    item: proc_macro::TokenStream,
) -> proc_macro::TokenStream {
    let item: proc_macro2::TokenStream = item.into();
    let item_copy = item.clone();
    let item: proc_macro::TokenStream = item.into();

    let node_struct = syn::parse_macro_input!(item as NodeStruct);
    let routing = syn::parse_macro_input!(attr as Routing);
    let from_sample_rate_impl = define_from_sample_rate_impl(&node_struct);

    let parameter_struct_definition = define_parameters_struct(&node_struct, &routing);

    let subnodes: Vec<_> = node_struct.fields.iter().map(|x| x.ident.clone()).collect();
    let subnodes = order_subnodes(&subnodes[..], &routing).unwrap();
    let impls = define_impls(&node_struct, &subnodes[..], &routing);

    (quote! {
        #item_copy
        #from_sample_rate_impl
        #parameter_struct_definition
        #impls
    })
    .into()
}

fn define_parameters_struct(
    node_struct: &NodeStruct,
    routing: &Routing,
) -> proc_macro2::TokenStream {
    let NodeStruct {
        visibility, ident, ..
    } = node_struct;

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
        #visibility struct #parameters_ident<'a> {
            #(#visibility #inputs: musicbx::DataRef<'a> ),*,
            #(#visibility #outputs: musicbx::DataMut<'a> ),*
        }

        impl Default for #parameters_ident<'static> {
            fn default() -> Self {
                Self {
                    #( #inputs: 0.0f32.into()),*,
                    #( #outputs: 0.0f32.into()),*,
                }
            }
        }
    }
}

fn define_impls(
    node_struct: &NodeStruct,
    subnodes: &[Ident],
    routing: &Routing,
) -> proc_macro2::TokenStream {
    let NodeStruct {
        ident, generics, ..
    } = &node_struct;
    let generics_where = &generics.where_clause;

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

    for subnode in subnodes {
        let subnode_parameters_type = node_struct
            .fields
            .iter()
            .find(|x| &x.ident == subnode)
            .map(|x| {
                let mut path = x.ty.clone();
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
                    #(#inputs),*,
                    #(#outputs),*,
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

    let parameters_ident = format_ident!("{}Parameters", node_struct.ident);

    quote! {
        impl #generics #ident #generics #generics_where {
            #[allow(clippy::needless_update)]
            pub fn process<const N: usize>(
                &mut self,
                n: usize,
                parameters: #parameters_ident,
            ) {
                let #parameters_ident { #(#inputs),*, #(mut #outputs)* } = parameters;
                #(#parts)*
            }
        }
    }
}

fn order_subnodes(subnodes: &[Ident], routing: &Routing) -> Result<Vec<Ident>, CompositeNodeError> {
    let input_ident = Ident::new("__input", Span::call_site());
    let output_ident = Ident::new("__output", Span::call_site());

    let mut graph = Graph::new();
    let mut node_indices: HashMap<&Ident, NodeIndex> = HashMap::new();
    for node in subnodes
        .iter()
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

    let mut result: Vec<Ident> = subnodes.iter().map(Ident::clone).collect();
    result.sort_by_key(|ident| order[ident]);
    Ok(result)
}

fn define_from_sample_rate_impl(node_struct: &NodeStruct) -> proc_macro2::TokenStream {
    let NodeStruct { fields, ident, .. } = node_struct;

    let fields: Vec<_> = fields.iter().map(|x| &x.ident).collect();

    quote! {
        impl musicbx::FromSampleRate for #ident {
            fn from_sample_rate(sr: f32) -> Self {
                Self {
                    #( #fields: musicbx::FromSampleRate::from_sample_rate(sr) ),*
                }
            }
        }
    }
}

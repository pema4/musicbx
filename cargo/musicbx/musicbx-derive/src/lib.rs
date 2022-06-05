use syn::{parse_macro_input, DeriveInput};

use node::Routing;

mod from_sample_rate;
mod node;

#[proc_macro_attribute]
pub fn node(
    attr: proc_macro::TokenStream,
    item: proc_macro::TokenStream,
) -> proc_macro::TokenStream {
    let input = syn::parse_macro_input!(item as DeriveInput);
    let routing = syn::parse_macro_input!(attr as Routing);
    node::node_attribute_macro(input, routing).into()
}

#[proc_macro_derive(FromSampleRate, attributes(from))]
pub fn derive_from_sample_rate(input: proc_macro::TokenStream) -> proc_macro::TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    from_sample_rate::derive(input).unwrap().into()
}

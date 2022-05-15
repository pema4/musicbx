mod ast;
mod node;

#[proc_macro_attribute]
pub fn node(
    attr: proc_macro::TokenStream,
    item: proc_macro::TokenStream,
) -> proc_macro::TokenStream {
    node::node_attribute_macro(attr, item)
}
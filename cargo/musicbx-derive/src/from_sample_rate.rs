use proc_macro2::TokenStream;
use quote::quote;
use syn::{Data, DeriveInput};
use thiserror::Error;

#[derive(Debug, Error)]
pub enum DeriveError {
    #[error("FromSampleRate can be derived only for structs")]
    NotStruct,

    #[error("FromSampleRate can't be derived for tuple structs")]
    UnexpectedTupleStruct,

    #[error(transparent)]
    ParseError(#[from] syn::Error),
}

pub fn derive(input: DeriveInput) -> Result<TokenStream, DeriveError> {
    let fields = match &input.data {
        Data::Struct(struct_data) => &struct_data.fields,
        _ => Err(DeriveError::NotStruct)?,
    };

    let field_with_values: Vec<TokenStream> = fields.iter()
        .map(|field| {
            let ident = field.ident.as_ref().ok_or(DeriveError::UnexpectedTupleStruct)?;

            let constructor_attr = field
                .attrs
                .iter()
                .find(|attr| attr.path.is_ident("from"));

            let constructor = if let Some(attr) = constructor_attr {
                attr.parse_args()?
            } else {
                quote! { musicbx::FromSampleRate::from_sample_rate(sr) }
            };

            Ok(quote! { #ident : #constructor })
        })
        .collect::<Result<_, DeriveError>>()?;

    let name = input.ident;
    Ok(quote! {
        impl musicbx::FromSampleRate for #name {
            fn from_sample_rate(sr: f32) -> Self {
                Self {
                    #( #field_with_values, )*
                }
            }
        }
    })
}
use std::path::{Path, PathBuf};
use std::{fs, io};

use quote::__private::{Ident, Span};
use quote::quote;
use thiserror::Error;

#[derive(Default)]
pub struct MusicbxCodegen {
    inputs: Vec<PathBuf>,
    output_dir: Option<PathBuf>,
}

#[derive(Debug, Error)]
pub enum MusicbxCodegenError {
    #[error("No output dir")]
    NoOutDir,

    #[error("Input must be file, not {0}")]
    InputMustBeFile(PathBuf),

    #[error(transparent)]
    GenerationError(#[from] MusicbxGenerationError),
}

impl MusicbxCodegen {
    pub fn new() -> Self {
        MusicbxCodegen {
            inputs: vec![],
            output_dir: None,
        }
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
        } = self;

        let out_dir = out_dir.ok_or(MusicbxCodegenError::NoOutDir)?;

        for input in inputs {
            let mut out_file = out_dir.clone();
            let input_file = input
                .file_name()
                .ok_or_else(|| MusicbxCodegenError::InputMustBeFile(input.clone()))?;
            out_file.push(input_file);

            generate(&input, &out_file)?;
        }

        Ok(())
    }
}

#[derive(Debug, Error)]
pub enum MusicbxGenerationError {
    #[error(transparent)]
    IoError(#[from] io::Error),

    #[error(transparent)]
    InvalidJsonFormat(#[from] serde_json::Error),

    #[error("Encountered unexpected error.")]
    UnexpectedError,
}

fn generate(input: &Path, output: &Path) -> Result<(), MusicbxGenerationError> {
    let input = fs::read(input)?;
    let musicbx_types::PatchInfo { .. } = serde_json::from_slice(&input[..])?;

    let node_name = output
        .file_name()
        .and_then(|x| x.to_str())
        .ok_or(MusicbxGenerationError::UnexpectedError)?;
    let node_name = Ident::new(node_name, Span::call_site());

    let generated_code = quote! {
        #[musicbx::node {

        }]
        pub struct #node_name {

        }
    };

    fs::write(output, generated_code.to_string())?;

    Ok(())
}

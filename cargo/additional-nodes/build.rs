use std::env;
use std::path::Path;

use musicbx_codegen::MusicbxCodegen;
use musicbx_std::StdModuleDefinition;

fn main() {
    let output_dir_env = env::var_os("OUT_DIR").unwrap();
    let output_dir = Path::new(&output_dir_env);
    MusicbxCodegen::new()
        .module(&StdModuleDefinition)
        .output_dir(output_dir)
        .inputs(&["nodes/FastTremolo.json", "nodes/TestFm.json"])
        .run()
        .expect("Codegen failed");
}

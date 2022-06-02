use std::env;
use std::path::Path;

use musicbx::codegen::MusicbxCodegen;
use musicbx::std::StdModuleDefinition;

fn main() {
    let output_dir_env = env::var_os("OUT_DIR").unwrap();
    let output_dir = Path::new(&output_dir_env);

    MusicbxCodegen::with_output_dir(&output_dir)
        .module(&StdModuleDefinition)
        .inputs(&["nodes/FastTremolo.json", "nodes/TestFm.json"])
        .run()
        .expect("Codegen failed");
}

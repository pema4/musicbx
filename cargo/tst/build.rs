use std::env;
use std::path::Path;

use musicbx_codegen::MusicbxCodegen;

fn main() {
    let output_dir_env = env::var_os("OUT_DIR").unwrap();
    let output_dir = Path::new(&output_dir_env);
    MusicbxCodegen::new()
        .output_dir(output_dir)
        .inputs(&["nodes/test.json"])
        .run()
        .expect("Codegen failed");
}

use musicbx::types::{NodeDefinition, NodeOutput};

include!(concat!(env!("OUT_DIR"), "/TestFm.rs"));

impl TestFm {
    pub const fn definition() -> NodeDefinition {
        NodeDefinition {
            uid: "additional_modes::test_fm::TestFm",
            inputs: &[],
            outputs: &[NodeOutput {
                number: 0,
                name: "output",
            }],
            parameters: &[],
        }
    }
}

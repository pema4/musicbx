use std::ops::DerefMut;

use glicol_synth::{AudioContext, Buffer, Input, Message};
use hashbrown::HashMap;
use petgraph::graph::NodeIndex;

use additional_nodes::test_fm::TestFm;
use musicbx::Node;
use musicbx::{DataMut, FromSampleRate};

use crate::nodes::{Description, NodeDescription, NodeFactory, NodeInfo, NodeWrapper};

pub struct TestFmNodeDescription;

impl NodeFactory for TestFmNodeDescription {
    fn uid(&self) -> &'static str {
        INFO.definition.uid
    }

    fn info(&self) -> &NodeInfo {
        &INFO
    }

    fn create_instance(&self, id: usize) -> Box<dyn NodeWrapper> {
        Box::new(TestFmNode {
            id,
            ..TestFmNode::default()
        })
    }
}

static INFO: NodeInfo = NodeInfo {
    definition: TestFm::definition(),
    description: NodeDescription {
        node: Description::new("Test FM", "Test generated FM patch"),
        inputs: &[],
        outputs: &[Description::new("output", "The output of the node")],
        parameters: &[],
    },
};

#[derive(Default)]
pub struct TestFmNode {
    id: usize,
    node_index: Option<NodeIndex>,
}

impl NodeWrapper for TestFmNode {
    fn id(&self) -> usize {
        self.id
    }

    fn input(&self, _: &str) -> Option<(usize, NodeIndex)> {
        None
    }

    fn output(&self, name: &str) -> Option<NodeIndex> {
        match name {
            "output" => self.node_index,
            _ => None,
        }
    }

    fn add_to_context(&mut self, context: &mut AudioContext<1>) {
        let sin = TestFmNodeImpl {
            inner: TestFm::from_sample_rate(context.sample_rate() as f32),
        };
        let sin: NodeIndex = context.add_mono_node(sin);

        self.node_index = Some(sin);
    }
}

struct TestFmNodeImpl {
    inner: TestFm,
}

impl<const N: usize> glicol_synth::Node<N> for TestFmNodeImpl {
    fn process(&mut self, _: &mut HashMap<usize, Input<N>>, output: &mut [Buffer<N>]) {
        self.inner.process::<N>(
            N,
            additional_nodes::test_fm::TestFmParameters {
                output: DataMut::from(output[0].deref_mut()),
            },
        )
    }

    fn send_msg(&mut self, _: Message) {}
}

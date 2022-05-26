include!(concat!(env!("OUT_DIR"), "/FastTremolo.rs"));

// #[musicbx::node {
//     v1 . output -> out,
//     v4 . output -> v2 . b ,
//     v2 . output -> v5 . a ,
//     v5 . output -> output
// }]
// pub struct FastTremolo {
//     v1: musicbx::osc::SinOsc,
//     v2: musicbx::util::Mul,
//     v4: musicbx::osc::SinOsc,
//     v5: musicbx::util::Mul,
// }

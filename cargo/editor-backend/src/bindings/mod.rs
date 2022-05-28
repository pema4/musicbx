mod available_nodes;
mod configuration;
mod editor;

#[macro_export]
macro_rules! unwrap_or_throw {
    ($env:expr, $result:expr) => {
        match $result {
            Ok(v) => v,
            Err(err) => {
                $env.throw_new("java/lang/IllegalArgumentException", err.to_string())
                    .unwrap();
                return;
            }
        }
    };
}

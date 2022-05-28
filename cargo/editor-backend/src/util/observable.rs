use std::sync::{Arc, Mutex};

pub type Listener<T> = Arc<dyn Fn(&T)>;

pub struct Observable<T> {
    pub data: T,
    listeners: Mutex<Vec<Listener<T>>>,
}

impl<T> Observable<T> {
    pub fn new(data: T) -> Self {
        Observable {
            data,
            listeners: Mutex::new(vec![]),
        }
    }

    pub fn add_listener(&self, listener: Listener<T>) {
        self.listeners.lock().unwrap().push(listener.clone());
        listener(&self.data)
    }

    #[allow(clippy::vtable_address_comparisons, dead_code)]
    pub fn remove_listener(&self, listener: &Listener<T>) {
        let listeners = &mut self.listeners.lock().unwrap();
        listeners.retain(|x| !Arc::ptr_eq(x, listener));
    }

    pub fn notify_all(&mut self) {
        for listener in self.listeners.lock().unwrap().iter() {
            listener(&self.data)
        }
    }
}

impl<T> Default for Observable<T>
where
    T: Default,
{
    fn default() -> Self {
        Observable::new(Default::default())
    }
}

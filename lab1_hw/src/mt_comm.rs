use std::{
    sync::mpsc,
    os::unix::io::{RawFd, AsRawFd},
};
use nix::poll::{PollFd, PollFlags};

pub type NotifyCallback = Box<dyn Fn () -> nix::Result<()> + Send + Sync>;

/// This struct should imitate linux eventfd API which is unsupported on MacOS
pub struct EventFd {
    pub sleep_fd: RawFd,
    pub wake_up_fd: RawFd,
}

impl EventFd {
    pub fn new() -> nix::Result<Self> {
        let (sleep_fd, wake_up_fd) = nix::unistd::pipe()?;
        Ok(Self { sleep_fd, wake_up_fd })
    }

    pub fn notify(&self) -> nix::Result<()> {
        nix::unistd::write(self.wake_up_fd, &[0u8]).map(|_| ())
    }

    pub fn pop(&self) -> nix::Result<()> {
        let mut buf = [0u8];
        nix::unistd::read(self.sleep_fd, &mut buf[..]).map(|_| ())
    }

    pub fn poll_fd(&self) -> PollFd {
        PollFd::new(self.sleep_fd, PollFlags::POLLIN)
    }

    pub fn is_event(&self, poll_fd: &PollFd) -> bool {
        if poll_fd.as_raw_fd() != self.sleep_fd {
            return false;
        }

        if let Some(revents) = poll_fd.revents() {
            return revents.contains(PollFlags::POLLIN);
        }
        return false;
    }

    pub fn notify_cb(&self) -> NotifyCallback {
        let wake_up_fd = self.wake_up_fd;
        let cb = move || { nix::unistd::write(wake_up_fd, &[0u8]).map(|_| ()) };
        Box::new(cb)
    }
}

pub struct ChannelIn<T> {
    sender: mpsc::Sender<T>,
    notify: NotifyCallback,
}

impl <T> ChannelIn<T> {
    pub fn send(&self, message: T) -> nix::Result<()> {
        self.sender.send(message)
            .map_err(|_| nix::Error::EIO)?;

        (*self.notify)()
    }
}

unsafe impl <T> Send for ChannelIn<T> {}

pub struct ChannelOut<T> {
    receiver: mpsc::Receiver<T>,
    sender: mpsc::Sender<T>,
    eventfd: EventFd,
}

impl <T> ChannelOut<T> {
    pub fn new() -> nix::Result<Self> {
        let (sender, receiver) = mpsc::channel();
        Ok(Self {
            receiver,
            sender,
            eventfd: EventFd::new()?,
        })
    }

    pub fn channel_in(&self) -> ChannelIn<T> {
        ChannelIn {
            sender: self.sender.clone(),
            notify: self.eventfd.notify_cb(),
        }
    }

    pub fn poll_fd(&self) -> PollFd {
        self.eventfd.poll_fd()
    }

    pub fn is_event(&self, poll_fd: &PollFd) -> bool {
        self.eventfd.is_event(poll_fd)
    }

    pub fn try_pop(&self, poll_fd: &PollFd) -> Option<T> {
        if !self.eventfd.is_event(poll_fd) {
            return None;
        }
        if self.eventfd.pop().is_err() {
            return None;
        }

        self.receiver
            .try_recv()
            .ok()
    }
}

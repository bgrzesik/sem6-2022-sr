use std::{
    sync::{atomic::{AtomicUsize, Ordering}, Arc},
    os::unix::{io::RawFd, prelude::AsRawFd},
    thread::{self, JoinHandle},
    collections::{HashMap, VecDeque}, usize, 
};

use nix::{
    unistd::close,
    poll::*,
    sys::socket::*,
};

mod common;
use common::*;

mod mt_comm;
use mt_comm::*;

#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
struct ClientId(usize);

#[derive(Debug)]
enum Event {
    ClientMessage {
        id: ClientId,
        message: Message,
    },
    Disconnect {
        id: ClientId,
        nickname: Option<String>,
    },
}

struct ClientWorker {
    id: ClientId,
    client_sock: RawFd,

    outgoing: ChannelOut<Message>,
    events: ChannelIn<Event>,
}

impl ClientWorker {
    fn new(id: ClientId, client_sock: RawFd, events: ChannelIn<Event>) -> nix::Result<Self> {
        Ok(Self {
            id,
            client_sock,
            events,
            outgoing: ChannelOut::<Message>::new()?,
        })
    }

    fn outgoing_channel(&self) -> ChannelIn<Message> {
        self.outgoing.channel_in()
    }
}

// This is safe if we use `broadcast` field only on single thread.
unsafe impl Sync for ClientWorker {}

impl std::fmt::Debug for ClientWorker {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ClientWorker")
            .field("id", &self.id)
            .field("client_sock", &self.client_sock)
            .finish()
    }
}

struct ClientState<'c> {
    worker: &'c ClientWorker,

    /// Messages to be sent to remote client
    outgoing_queue: VecDeque<Message>,

    connected: bool,
    nickname: Option<String>,
}

impl <'c> ClientState<'c> {

    fn on_message(&mut self, message: Message) -> nix::Result<()> {
        let client_id = self.worker.id;

        match &message {
            Message::Handshake { nickname } => {
                let nickname = nickname.clone();
                println!("[client #{client_id:?}] handshook {nickname}");
                self.nickname = Some(nickname);
            }
            Message::Content { nickname, content } => {
                if self.nickname.as_ref() != Some(nickname) {
                    // TODO(bgrzesik): requeue until handshake
                    return Ok(());
                }

                println!("[client #{client_id:?}] says {content}",
                         content = content);
            }
            Message::Disconnect { .. } => {
                self.connected = false;
            }
        }

        // Queue message for broadcast
        self.worker.events.send(Event::ClientMessage {
            id: client_id,
            message,
        })
    }

    fn handle_client_poll(&mut self, poll_fd: &PollFd) -> nix::Result<()> {
        assert_eq!(poll_fd.as_raw_fd(), self.worker.client_sock);

        let client_id = self.worker.id;
        // Return EIO if revents hasn't been set by poll
        let revents = if let Some(revents) = poll_fd.revents() {
            revents
        } else {
            return Ok(());
        };

        if revents.contains(PollFlags::POLLHUP) {
            println!("[client #{client_id:?}] received HUP");
            self.connected = false;
            return Ok(());
        }

        if revents.contains(PollFlags::POLLOUT) && !self.outgoing_queue.is_empty() {
            // Dequeue pending message and send to client
            let message = self.outgoing_queue.pop_front()
                .ok_or(nix::Error::ENOMSG)?;

            println!("[client #{client_id:?}] sending {message:?}");
            message.send(self.worker.client_sock, None /* to */)?;
        }

        if revents.contains(PollFlags::POLLIN) {
            // Receive message and enqueue for server main thread
            let message = Message::receive(self.worker.client_sock)?;
            println!("[client #{client_id:?}] received {message:?}");

            self.on_message(message)?;
        }

        Ok(())
    }

    fn handle_outgoing_poll(&mut self, poll_fd: &PollFd) -> nix::Result<()> {
        let client_id = self.worker.id;

        let message = if let Some(message) = self.worker.outgoing.try_pop(poll_fd) {
            message
        } else {
            return Ok(());
        };

        println!("[client #{client_id:?}] Queueing message for sending {message:?}");
        self.outgoing_queue.push_front(message);

        Ok(())
    }
}


impl ClientWorker {
    fn handle_connection(&self) -> nix::Result<()> {
        let client_id = self.id;
        let mut client_state = ClientState {
            worker: self,
            outgoing_queue: Default::default(),
            nickname: None,
            connected: true,
        };

        println!("[client #{client_id:?}] entering loop");
        while client_state.connected {
            let mut client_flags = PollFlags::POLLIN | PollFlags::POLLHUP;
            if !client_state.outgoing_queue.is_empty() {
                client_flags |= PollFlags::POLLOUT;
            }

            let mut poll_fds = [
                PollFd::new(self.client_sock, client_flags),
                self.outgoing.poll_fd(),
            ];

            if poll(&mut poll_fds, i32::MAX)? == 0 {
                println!("[client #{client_id:?}] poll returned 0");
                continue;
            }
            println!("[client #{client_id:?}] poll waken up");

            client_state.handle_client_poll(&poll_fds[0])?;
            client_state.handle_outgoing_poll(&poll_fds[1])?;
        }

        self.events.send(Event::Disconnect {
            id: client_id,
            nickname: client_state.nickname,
        })?;

        println!("[client #{client_id:?}] exited loop");

        Ok(())
    }
}

struct Server {
    // TODO(bgrzesik): probably usize would do as well
    last_client_id: AtomicUsize,
    workers: HashMap<ClientId, Arc<ClientWorker>>,
    join_handles: HashMap<ClientId, JoinHandle<()>>,

    stream_sock: RawFd,
    dgram_sock: RawFd,

    events: ChannelOut<Event>,
}

impl Server {
    fn new() -> nix::Result<Self> {
        Ok(Self {
            last_client_id: AtomicUsize::new(0),

            workers: HashMap::new(),
            join_handles: HashMap::new(),

            stream_sock: -1,
            dgram_sock: -1,

            events: ChannelOut::<Event>::new()?,
        })
    }

    fn listen(&mut self) -> nix::Result<()> {
        self.stream_sock = socket(
            AddressFamily::Inet,
            SockType::Stream,
            SockFlag::empty(),
            None,
        )?;

        let addr = SockAddr::new_inet(InetAddr::new(IP_ALL_INTERFACES, PORT_SERVER_TCP));
        bind(self.stream_sock, &addr)?;
        listen(self.stream_sock, 8)?;
        println!("[tcp] listening {IP_ALL_INTERFACES}:{PORT_SERVER_TCP}");

        self.dgram_sock = socket(
            AddressFamily::Inet,
            SockType::Datagram,
            SockFlag::empty(),
            None,
        )?;

        let addr = SockAddr::new_inet(InetAddr::new(IP_ALL_INTERFACES, PORT_SERVER_UDP));
        bind(self.dgram_sock, &addr)?;
        println!("[udp] listening {IP_ALL_INTERFACES}:{PORT_SERVER_UDP}");

        Ok(())
    }

    fn accept(&mut self) -> nix::Result<()> {
        let client_sock = accept(self.stream_sock);
        println!("[tcp] accepted new connection");

        if let Err(nix::Error::EWOULDBLOCK) = client_sock {
            return Ok(())
        }

        let client_sock = client_sock?;

        let id = ClientId(self.last_client_id.fetch_add(1, Ordering::Relaxed));

        let client = ClientWorker::new(id, client_sock, self.events.channel_in())?;
        let client = Arc::new(client);

        let thread = {
            let client = client.clone();
            thread::spawn(move || client.handle_connection().expect("Connection handle"))
        };

        self.workers.insert(id, client);
        self.join_handles.insert(id, thread);

        Ok(())
    }

    fn broadcast_except(&mut self, id: ClientId, message: Message) -> nix::Result<()> {
        for (client_id, worker) in &self.workers {
            if client_id == &id {
                continue;
            }

            worker.outgoing_channel().send(message.clone())?;
        }

        Ok(())
    }

    fn on_event(&mut self, event: Event) -> nix::Result<()> {
        match event {
            Event::Disconnect { id, nickname } => {
                if let Some(join_handle) = self.join_handles.remove(&id) {
                    let _ = join_handle.join();
                    let _ = self.workers.remove(&id);
                    println!("[server] client disconnected {id:?}");

                    self.broadcast_except(id, Message::Disconnect { 
                        nickname: if let Some(nickname) = nickname {
                            nickname
                        } else {
                            String::from("Unknown")
                        }, 
                    })?;
                }
            }
            Event::ClientMessage { id, message } => self.broadcast_except(id, message)?
        }
        Ok(())
    }

    fn handle_poll(&mut self, poll_fd: &PollFd) -> nix::Result<()> {
        let fd = poll_fd.as_raw_fd();
        let poll_in = poll_fd.revents()
            .map(|revents| revents.contains(PollFlags::POLLIN))
            .unwrap_or(false);

        if fd == self.stream_sock && poll_in {
            self.accept()?;
        } else if fd == self.dgram_sock && poll_in {
            println!("[udp] poll read");
            let mut sender: Option<SockAddr> = None;
            let message = Message::receive_from(self.dgram_sock, &mut sender)?;

            println!("[udp] received {sender:?} {message:?}");

            // NOTE(bgrzesik): no point to redirect to client thread
            self.events.channel_in().send(Event::ClientMessage {
                id: ClientId(usize::MAX),
                message,
            })?;
        } else if let Some(event) = self.events.try_pop(poll_fd) {
            println!("[server] received broadcast event {:?}", event);
            self.on_event(event)?;
        }

        Ok(())
    }

    fn enter_loop(&mut self) -> nix::Result<()> {
        loop {
            let mut poll_fds = vec![
                PollFd::new(self.stream_sock, PollFlags::POLLIN),
                PollFd::new(self.dgram_sock, PollFlags::POLLIN),
                self.events.poll_fd(),
            ];

            println!("[server] sleeping with poll");
            if poll(&mut poll_fds, i32::MAX)? == 0 {
                println!("[server] poll returned 0");
                continue;
            }

            println!("[server] poll finished");
            for poll_fd in &poll_fds {
                self.handle_poll(poll_fd)?;
            }
        }
    }

}

impl Drop for Server {
    fn drop(&mut self) {
        let _ = close(self.stream_sock);
        let _ = close(self.dgram_sock);
    }
}

fn main() {
    let mut server = Server::new().unwrap();

    server.listen().unwrap();
    server.enter_loop().unwrap();
}

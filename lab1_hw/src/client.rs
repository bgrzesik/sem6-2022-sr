use clap::Parser;

use std::{
    os::unix::{io::RawFd, prelude::AsRawFd},
    net,
    collections::VecDeque, 
    io::Write,
};

use nix::{
    unistd::close,
    poll::*,
    sys::socket::*,
};

mod common;
use common::*;

struct Client {
    stream_sock: RawFd,
    dgram_sock: RawFd,
    outgoing_stream: VecDeque<Message>,
    outgoing_dgram: VecDeque<Message>,
    connected: bool,
}

impl Client {
    fn new() -> nix::Result<Self> {
        Ok(Self {
            stream_sock: -1,
            dgram_sock: -1,
            outgoing_stream: VecDeque::new(),
            outgoing_dgram: VecDeque::new(),
            connected: false,
        })
    }

    fn connect(&mut self, stream: (IpAddr, u16), dgram: (IpAddr, u16)) -> nix::Result<()> {
        let stream_sock = socket(AddressFamily::Inet, SockType::Stream, SockFlag::empty(), None)?;
        let stream_addr = SockAddr::new_inet(InetAddr::new(stream.0, stream.1));
        connect(stream_sock, &stream_addr)?;

        let dgram_sock = socket(AddressFamily::Inet, SockType::Datagram, SockFlag::empty(), None)?;
        let dgram_addr = SockAddr::new_inet(InetAddr::new(dgram.0, dgram.1));

        // Bind UDP socket wih server
        connect(dgram_sock, &dgram_addr)?;

        self.stream_sock = stream_sock;
        self.dgram_sock = dgram_sock;
        self.connected = true;
        Ok(())
    }

    fn stream_poll_fd(&self) -> PollFd {
        let mut events = PollFlags::POLLIN;
        if !self.outgoing_stream.is_empty() {
            events |= PollFlags::POLLOUT;
        }

        PollFd::new(self.stream_sock, events)
    }
    
    fn dgram_poll_fd(&self) -> PollFd {
        let mut events = PollFlags::POLLIN;
        if !self.outgoing_dgram.is_empty() {
            events |= PollFlags::POLLOUT;
        }

        PollFd::new(self.dgram_sock, events)
    }

    fn handle_poll(&mut self, poll_fd: &PollFd, stream: bool) -> nix::Result<Option<Message>> {
        let (sock, outgoing) = if stream {
            (self.stream_sock, &mut self.outgoing_stream)
        } else {
            (self.dgram_sock, &mut self.outgoing_dgram)
        };

        assert!(sock == poll_fd.as_raw_fd());

        let revents = if let Some(revents) = poll_fd.revents() {
            revents
        } else {
            return Ok(None);
        };

        if revents.contains(PollFlags::POLLHUP) {
            println!("[client] received HUP");
            self.connected = false;
            return Ok(None);
        }

        if revents.contains(PollFlags::POLLOUT) && !outgoing.is_empty() {
            let message = outgoing.pop_front().unwrap();
            message.send(sock, None)?;
        }

        if revents.contains(PollFlags::POLLIN) {
            return Ok(Some(Message::receive(sock)?));
        }

        Ok(None)
    }

    fn handle_stream_poll(&mut self, poll_fd: &PollFd) -> nix::Result<Option<Message>> {
        self.handle_poll(poll_fd, true)
    }

    fn handle_dgram_poll(&mut self, poll_fd: &PollFd) -> nix::Result<Option<Message>> {
        self.handle_poll(poll_fd, false)
    }

    fn send(&mut self, message: Message) {
        self.outgoing_stream.push_back(message);
    }

    fn send_dgram(&mut self, message: Message) {
        self.outgoing_dgram.push_back(message);
    }
}

impl Drop for Client {
    fn drop(&mut self) {
        let _ = close(self.stream_sock);
        let _ = close(self.dgram_sock);
    }
}

#[derive(Parser, Debug)]
struct Arguments {
    ip: String,

    #[clap(short, long, default_value_t=PORT_SERVER_TCP)]
    port: u16,

    #[clap(short, long, default_value_t=PORT_SERVER_UDP)]
    udp_port: u16,

    nickname: String,
}

fn main() {
    let args = Arguments::parse();
    println!("[client] {args:?}");

    let mut client = Client::new().expect("New client");

    let ip: net::IpAddr = args.ip.parse().expect("Unable to parse address");
    let ip = IpAddr::from_std(&ip);

    client.connect((ip, args.port), (ip, args.udp_port)).expect("Unable to connect");

    client.send(Message::Handshake {
        nickname: args.nickname.clone(),
    });

    print!("> ");
    while client.connected {
        let stdin = std::io::stdin();
        let mut poll_fds = [
            client.stream_poll_fd(),
            client.dgram_poll_fd(),
            PollFd::new(stdin.as_raw_fd(), PollFlags::POLLIN),
        ];

        let _ = std::io::stdout().flush();
        if poll(&mut poll_fds, i32::MAX).expect("poll failed")  == 0 {
            continue;
        }

        if poll_fds[2].revents().unwrap_or(PollFlags::empty()).contains(PollFlags::POLLIN) {
            let mut content = String::new();
            stdin.read_line(&mut content)
                .expect("Unable to read from Stdin");

            let mut content = content.trim();
            if content.is_empty() {
                print!("> ");
                continue;
            }

            let mut dgram = false;
            if content.len() > 6 && &content[..6] == "\\dgram" {
                dgram = true;
                content = &content[6..];
            }

            let nickname = args.nickname.clone();
            if !dgram {
                println!("[L] <{nickname}>: {content}");
                client.send(Message::Content { nickname, content: content.into() });
            } else {
                client.send_dgram(Message::Content { nickname, content: content.into() });
            }
        } else {
            print!("\x08\x08");
        }

        let message_stream: Option<Message> = client.handle_stream_poll(&poll_fds[0])
            .expect("Unable to handle poll");

        let message_dgram: Option<Message> = client.handle_dgram_poll(&poll_fds[1])
            .expect("Unable to handle poll");

        for message in [message_stream, message_dgram] {
            let message = if let Some(message) = message {
                message
            } else {
                continue;
            };

            match message {
                Message::Handshake { nickname } 
                    => println!("[S] Welcome {nickname} to the server"),

                Message::Content { nickname, content } 
                    => println!("[C] <{nickname}>: {content}"),

                Message::Disconnect { nickname } 
                    => println!("[S] {nickname} has disconnected from the server"),
            }
        }

        print!("> ");
    }
}

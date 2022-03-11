use std::os::unix::io::RawFd;
use nix::sys::socket::*;

pub const IP_ALL_INTERFACES: IpAddr = IpAddr::V4(Ipv4Addr::any());

pub const PORT_SERVER_TCP: u16 = 9000;
pub const PORT_SERVER_UDP: u16 = 9001;

macro_rules! message_fourcc {
    ($a: literal $b: literal $c: literal $d: literal) => {
        ($a as char as u32)
            | (($b as char as u32) << 8)
            | (($c as char as u32) << 16)
            | (($d as char as u32) << 24)
    };
}

#[repr(u32)]
#[derive(Debug, Clone)]
pub enum MessageType {
    /// 'Message::content' becomes nickname
    Handshake = message_fourcc!('H' 'A' 'N' 'D'),
    BroadcastMessage = message_fourcc!('B' 'C' 'A' 'S'),
    Disconnected  = message_fourcc!('D' 'C' 'O' 'N'),
}

pub const MESSAGE_CONTENT_LEN: usize = 128;
pub type RawContent = [u8; MESSAGE_CONTENT_LEN];

pub const MESSAGE_NICKNAME_LEN: usize = 16;
pub type RawNickname = [u8; MESSAGE_NICKNAME_LEN];

#[repr(C)]
#[derive(Debug)]
struct RawMessage {
    // TODO(bgrzesik): add magic packet number
    pub message_type: MessageType,

    pub nickname_len: u16,
    pub nickname: RawNickname,

    pub content_len: u16,
    pub content: RawContent,
}

const MESSAGE_SIZE: usize = std::mem::size_of::<RawMessage>();

// TODO(bgrzesik): use enum
#[derive(Debug, Clone)]
pub enum Message {
    Handshake {
        nickname: String,
    },
    Content {
        nickname: String,
        content: String,
    },
    Disconnect {
        nickname: String,
    }
}

fn string_to_raw<const SIZE: usize>(s: &String) -> nix::Result<([u8; SIZE], u16)> {
    let mut raw = [0u8; SIZE];

    let bytes = s.as_bytes();
    // Left one for null pointer
    if bytes.len() >= SIZE - 1 {
        return Err(nix::Error::EINVAL);
    }

    for i in 0..bytes.len() {
        raw[i] = bytes[i];
    }

    Ok((raw, bytes.len() as u16))
}

fn raw_to_string<const SIZE: usize>(raw: [u8; SIZE], len: u16) -> nix::Result<String> {
    let len = len as usize;
    assert!(len < MESSAGE_CONTENT_LEN);

    String::from_utf8((&raw[..len]).into())
        .map_err(|_| nix::Error::EIO)
}

impl TryInto<RawMessage> for Message {
    type Error = nix::Error;

    fn try_into(self) -> nix::Result<RawMessage> {
        match self {
            Message::Handshake { nickname } => {
                let (nickname, nickname_len) = string_to_raw(&nickname)?;

                Ok(RawMessage {
                    message_type: MessageType::Handshake,
                    nickname_len, nickname,
                    content: [0; MESSAGE_CONTENT_LEN],
                    content_len: 0,
                })
            }
            Message::Content { nickname, content } => {
                let (nickname, nickname_len) = string_to_raw(&nickname)?;
                let (content, content_len) = string_to_raw(&content)?;

                Ok(RawMessage {
                    message_type: MessageType::BroadcastMessage,
                    nickname_len, nickname,
                    content_len, content,
                })
            }
            Message::Disconnect { nickname } => {
                let (nickname, nickname_len) = string_to_raw(&nickname)?;

                Ok(RawMessage {
                    message_type: MessageType::Disconnected,
                    nickname_len, nickname,
                    content: [0; MESSAGE_CONTENT_LEN],
                    content_len: 0,
                })
            }
        }

    }
}

impl TryFrom<RawMessage> for Message {
    type Error = nix::Error;

    fn try_from(raw: RawMessage) -> nix::Result<Self> {
        let content = raw_to_string(raw.content, raw.content_len)?;
        let nickname = raw_to_string(raw.nickname, raw.nickname_len)?;

        match raw.message_type {
            MessageType::Handshake => {
                Ok(Message::Handshake { nickname })
            }
            MessageType::BroadcastMessage  => {
                Ok(Message::Content { nickname, content })
            }
            MessageType::Disconnected => {
                Ok(Message::Disconnect { nickname })
            }
            _ => Err(nix::Error::EINVAL),
        }
    }
}

impl Message {
    pub fn receive(fd: RawFd) -> nix::Result<Self> {
        let mut buf = [0xffu8; MESSAGE_SIZE];

        let size = recv(fd, &mut buf, MsgFlags::empty())?;
        // TODO(bgrzesik): handle partial packets
        if size != MESSAGE_SIZE {
            return Err(nix::Error::EWOULDBLOCK);
        }

        unsafe { std::mem::transmute::<[u8; MESSAGE_SIZE], RawMessage>(buf) }.try_into()
    }

    pub fn receive_from(fd: RawFd, out_addr: &mut Option<SockAddr>) -> nix::Result<Self> {
        let mut buf = [0xffu8; MESSAGE_SIZE];

        let (size, addr) = recvfrom(fd, &mut buf)?;
        // TODO(bgrzesik): handle partial packets
        if size != MESSAGE_SIZE {
            return Err(nix::Error::EWOULDBLOCK);
        }

        *out_addr = addr;

        unsafe { std::mem::transmute::<[u8; MESSAGE_SIZE], RawMessage>(buf) }.try_into()
    }

    pub fn send(self, fd: RawFd, to: Option<&SockAddr>) -> nix::Result<()> {
        let raw: RawMessage = self.try_into()?;
        let buf =
            unsafe { std::slice::from_raw_parts((&raw as *const RawMessage) as *const u8, MESSAGE_SIZE) };

        let size = if let Some(to) = to {
            sendto(fd, buf, to, MsgFlags::empty())?
        } else {
            send(fd, buf, MsgFlags::empty())?
        };

        // TODO(bgrzesik): partial send?
        assert_eq!(size, MESSAGE_SIZE);
        Ok(())
    }
}

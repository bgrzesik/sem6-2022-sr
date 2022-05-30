import sys
import signal
from time import sleep
import typing
import threading
import tempfile

# python -m grpc_tools.protoc -I./proto --python_out=./gen/ --grpc_python_out=./gen/ ./proto/chat.proto

from concurrent import futures

import grpc
import chat_pb2_grpc as rpc
import chat_pb2 as protos

from PySide2 import QtWidgets, QtCore, QtGui

class ChatPoller(threading.Thread):

    def __init__(self, group_manager: rpc.GroupManagerStub, group: str, recipientId: str):
        super().__init__()

        self.group_manager = group_manager
        self.onMessage = None
        self.onDone = None
        self.group = group
        self.recipientId = recipientId
        self.setDaemon(True)

    def run(self):
        req = protos.GroupManagerGetMessages(
                group=protos.Group(name=self.group), 
                recipientId=self.recipientId)

        print("Chat poller thread started")
        try:
            for msg in self.group_manager.GetMessages(req):
                print("Got message", msg.content)

                if self.onMessage:
                    self.onMessage(msg)
        except BaseException as ex:
            print("Chat poller exception", ex)

        print("Chat poller thread finished")

        if self.onDone:
            self.onDone()

class ChatClient(object):

    def __init__(self):
        self.executor = futures.ThreadPoolExecutor(1)
        self.onMessage = None

        self.channel: typing.Optional[grpc.Channel] = None

        self.group_manager: typing.Optional[rpc.GroupManagerStub] = None
        self.group_repo: typing.Optional[rpc.GroupRepositoryStub] = None

        self.is_connected = False
        self.been_connected = False
        self.address: typing.Optional[str] = None

        self.connect_pending = futures.Future()
        self.connect_pending.set_result(False)

        self.poller: typing.Optional[ChatPoller] = None

        self.group = 'default'
        self.nickname = 'nickname'

    def onTextEnterAsync(self, text: str):
        if not text:
            return

        elif text.startswith('/connect'):
            args = text.split()
            if len(args) != 2 or not args[1]:
                self.textOutput('usage: /connect <address>')
                return

            self.postTask(self.connect, args[1])
            return

        elif text.startswith('/group'):
            args = text.split()
            if len(args) == 1:
                self.textOutput(f'Group: {self.group}')
                return
            elif len(args) != 2 or not args[1]:
                self.textOutput('usage: /group <group>')
                return

            if not self.poller:
                self.postTask(self.setGroup, args[1])
            else:
                self.textOutput('Already connected')

        elif text.startswith('/nickname'):
            args = text.split()
            if len(args) == 1:
                self.textOutput(f'Nickname: {self.nickname}')
                return
            elif len(args) != 2 or not args[1]:
                self.textOutput('usage: /nickname <nickname>')
                return

            if not self.poller:
                self.postTask(self.setNickname, args[1])
            else:
                self.textOutput('Already connected')

        elif text.startswith('/'):
            self.textOutput('invalid command')

        else:
            self.postTask(self.sendMessage, text)

    def replyTo(self, message_id: str, content: str):
        self.postTask(self.doReplyTo, message_id, content)

    def doReplyTo(self, message_id: str, content: str):
        if (not content) or (not message_id):
            return

        print("sending")
        msg = protos.ChatMessage(group=protos.Group(name=self.group),
                                 author=self.nickname,
                                 repliesTo=protos.ChatMessage(messageId=message_id),
                                 content=content)

        self.group_manager.SendMessage(msg)
        print("sent")

    def sendMessage(self, content: str):
        if not self.group_manager:
            self.textOutput('Not connected. Use /connect <address> to connect')
            return

        msg = protos.ChatMessage(group=protos.Group(name=self.group),
                                 author=self.nickname,
                                 content=content)

        self.group_manager.SendMessage(msg)

    def sendFile(self, url):
        self.postTask(self.doSendFile, url)

    def doSendFile(self, url):
        if not self.group_manager:
            self.textOutput('Not connected. Use /connect <address> to connect')
            return

        byte_array = open(url.toLocalFile(), 'rb').read()

        msg = protos.ChatMessage(
                group=protos.Group(name=self.group),
                author=self.nickname,
                file=protos.FileAttachment(
                    mime='application/gif',
                    content=byte_array))

        self.group_manager.SendMessage(msg)

    def connect(self, address: str, sleep_time=0):
        self.address = address

        if self.connect_pending.done():
            self.connect_pending = self.postTask(self.doConnect, sleep_time=sleep_time)

    def setGroup(self, group: str):
        self.group = group
        self.textOutput(f'Group: {self.group}')


    def setNickname(self, nickname: str):
        self.nickname = nickname
        self.textOutput(f'Nickname: {self.nickname}')

    def doConnect(self, sleep_time=0):
        if sleep_time != 0:
            self.textOutput(f'Sleeping for {sleep_time}')
            sleep(sleep_time)

        self.textOutput(f'Connecting {self.address}')

        if self.channel:
            self.channel.unsubscribe(self.onChannelConnectivity)
        else:
            self.channel = grpc.insecure_channel(self.address)

        self.channel.subscribe(self.onChannelConnectivity, try_to_connect=True)

    def onChannelConnectivity(self, connectivity):
        self.postTask(self.handleChannelConnectivity, connectivity)

    def handleChannelConnectivity(self, connectivity):
        print(connectivity)
        self.is_connected = connectivity == grpc.ChannelConnectivity.READY

        if connectivity == grpc.ChannelConnectivity.SHUTDOWN or connectivity == grpc.ChannelConnectivity.IDLE:
            if self.been_connected and self.address:
                self.textOutput(f'Lost connection will attempt to reconnect in 5')
                self.reconnect()

        elif connectivity == grpc.ChannelConnectivity.READY:
            self.textOutput(f'Connected {self.address}')
            self.been_connected = True
            self.connect_pending.cancel()
            self.postTask(self.onConnected)

    def reconnect(self):
        self.shutdown()
        self.connect(self.address, sleep_time=5)
        self.textOutput(f'Connection shutdown')

    def onConnected(self):
        self.group_manager = rpc.GroupManagerStub(self.channel)
        self.group_repo = rpc.GroupRepositoryStub(self.channel)

        if self.group_manager:
            self.poller = ChatPoller(self.group_manager, self.group, self.nickname)
            self.poller.onMessage = self.onMessage
            self.poller.onDone = self.onPollerDone
            self.poller.start()

    def onPollerDone(self):
        self.postTask(self.handlePollerDone)

    def handlePollerDone(self):
        if self.poller:
            self.poller.join()
            self.poller = None
            self.textOutput(f'Poller done')
            self.reconnect()

    def shutdown(self):
        if self.channel:
            self.been_connected = False

            self.channel.unsubscribe(self.onChannelConnectivity)
            self.channel.close()
            self.channel = None

            self.group_repo = None
            self.group_manager = None

    def textOutput(self, text: str):
        msg = protos.ChatMessage(content=text)
        if self.onMessage:
            self.onMessage(msg)


    def postTask(self, fn, *args, **kwargs):
        def wrapper(fn, args, kwargs):
            try:
                fn(*args, **kwargs)
            except BaseException as ex:
                print('=' * 10, 'Error', '=' * 10)
                print(type(ex), ex)

        return self.executor.submit(wrapper, fn, args, kwargs)

class MessageWidget(QtWidgets.QWidget):

    def __init__(self, message: protos.ChatMessage, replyTo):
        super().__init__()

        self.message = message
        self.replyTo = replyTo

        self.setSizePolicy(QtWidgets.QSizePolicy.Fixed, QtWidgets.QSizePolicy.Fixed)
        self.list = QtWidgets.QGridLayout(self)

        if message.messageId:
            message_id = QtWidgets.QLabel(message.messageId)
            message_id.setStyleSheet('background-color: blue; color: yellow;');
            self.list.addWidget(message_id, 1, 0)

        if message.HasField('group') and message.group and message.group.name:
            group = QtWidgets.QLabel(message.group.name)
            group.setStyleSheet('background-color: green; color: white;');
            self.list.addWidget(group, 1, 1)

        if message.author:
            author = QtWidgets.QLabel(message.author)
            author.setStyleSheet('background-color: red; color: blue;');
            self.list.addWidget(author, 1, 2)

        if message.content:
            content = QtWidgets.QLabel(message.content)
            content.setStyleSheet('background-color: white; color: black;');
            self.list.addWidget(content, 1, 3)

        if message.HasField('attachment') and message.WhichOneof('attachment') == 'file':
            self.tmp_file = tempfile.NamedTemporaryFile('wb')
            self.tmp_file.write(message.file.content)

            movie = QtGui.QMovie(self.tmp_file.name)
            movie.setScaledSize(QtCore.QSize(400, 400))
            movie.start()

            label = QtWidgets.QLabel(self)
            label.setMovie(movie)
            label.setMaximumSize(400, 400)

            self.list.addWidget(label, 2, 0, 1, 4, QtCore.Qt.AlignCenter)

        if message.HasField('repliesTo'):
            repliesTo = QtWidgets.QLabel('Replies to')
            self.list.addWidget(repliesTo, 0, 2)

            repliesToId = QtWidgets.QLabel(message.repliesTo.messageId)
            repliesToId.setStyleSheet('background-color: blue; color: yellow;');
            self.list.addWidget(repliesToId, 0, 3)

            repliesToAuthor = QtWidgets.QLabel(message.repliesTo.author)
            repliesToAuthor.setStyleSheet('background-color: red; color: blue;');
            self.list.addWidget(repliesToAuthor, 0, 4)

            repliesToContent = QtWidgets.QLabel(message.repliesTo.content)
            repliesToContent.setStyleSheet('background-color: white; color: black;');
            self.list.addWidget(repliesToContent, 0, 5)

        self.setLayout(self.list)

    def mousePressEvent(self, event: QtGui.QMouseEvent):
        print(event)

        if self.message.messageId:
            self.replyTo(self.message.messageId)

        super().mousePressEvent(event)


class MessagesWidget(QtWidgets.QScrollArea):

    class Signals(QtCore.QObject):
        message = QtCore.Signal(protos.ChatMessage)

    def __init__(self, client: ChatClient, replyTo):
        super().__init__()

        self.client = client
        self.client.onMessage = self.onMessage
        self.replyTo = replyTo

        self.signals = MessagesWidget.Signals()
        self.signals.message.connect(self.onMessageQt)

        widget = QtWidgets.QWidget()

        self.list = QtWidgets.QVBoxLayout(self)
        self.list.setAlignment(QtCore.Qt.AlignTop)
        self.list.setSpacing(0)
        self.list.setMargin(0)

        widget.setLayout(self.list)
        self.setWidget(widget)
        self.setAlignment(QtCore.Qt.AlignTop)
        self.setSizePolicy(QtWidgets.QSizePolicy.Minimum, QtWidgets.QSizePolicy.Minimum)
        self.setWidgetResizable(True)

    def onMessage(self, message: protos.ChatMessage):
        self.signals.message.emit(message)

    def onMessageQt(self, message: protos.ChatMessage):
        msg_widget = MessageWidget(message, self.replyTo)
        self.list.addWidget(msg_widget, QtCore.Qt.AlignTop)

        self.verticalScrollBar().setSliderPosition(self.verticalScrollBar().maximum())
        self.show()

class MainWidget(QtWidgets.QWidget):

    def __init__(self, client: ChatClient):
        super().__init__()

        self.client = client

        layout = QtWidgets.QGridLayout(self)

        self.messages = MessagesWidget(client, self.replyTo)

        #self.messages_scroll = QtWidgets.QScrollArea(self)
        #self.messages_scroll.setWidget(self.messages)
        #layout.addWidget(self.messages_scroll, 0, 0)
        layout.addWidget(self.messages, 0, 0)

        self.send_btn = QtWidgets.QPushButton('Send', self)
        self.send_btn.clicked.connect(self.send)
        layout.addWidget(self.send_btn, 1, 1)

        self.message_edit = QtWidgets.QLineEdit(self)
        self.message_edit.returnPressed.connect(self.send_btn.click)
        layout.addWidget(self.message_edit, 1, 0)

        self.setLayout(layout)
        self.setAcceptDrops(True)

    def send(self):
        text = self.message_edit.text()
        self.client.onTextEnterAsync(text)
        self.message_edit.setText('')

    def replyTo(self, messageId: str):
        text = self.message_edit.text()
        self.client.replyTo(messageId, text)
        self.message_edit.setText('')

    def dropEvent(self, drop: QtGui.QDropEvent) -> None:
        for url in drop.mimeData().urls():
            self.client.sendFile(url)

        super().dropEvent(drop)

    def dragEnterEvent(self, event: QtGui.QDragEnterEvent) -> None:
        if event.mimeData().hasUrls():
            event.setDropAction(QtCore.Qt.CopyAction)
            event.accept()
        else:
            super().dragEnterEvent(event)
            
    def dragMoveEvent(self, event: QtGui.QDragMoveEvent) -> None:
        if event.mimeData().hasUrls():
            event.setDropAction(QtCore.Qt.CopyAction)
            event.accept()
        else:
            super().dragMoveEvent(event)



def main():
    app = QtWidgets.QApplication(sys.argv)

    window = QtWidgets.QMainWindow()

    client = ChatClient()

    widget = MainWidget(client)
    window.setMinimumSize(1000, 700)
    window.setCentralWidget(widget)
    window.setWindowTitle("Chat client")
    window.show()

    signal.signal(signal.SIGINT, signal.SIG_DFL)

    app.exec_()


if __name__ == '__main__':
    main()

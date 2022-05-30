package pl.edu.agh.student.sr.lab5.server.grpc.service;

import com.google.protobuf.Empty;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.edu.agh.student.sr.lab5.server.grpc.MessageRouter;
import pl.edu.agh.student.sr.lab5.server.service.GroupRepository;
import pl.edu.agh.student.sr.lab5.proto.Chat;
import pl.edu.agh.student.sr.lab5.proto.GroupManagerGrpc;

@Service
public class GroupManagerRpcImpl extends GroupManagerGrpc.GroupManagerImplBase {
    private static final Logger log = LoggerFactory.getLogger(GroupManagerRpcImpl.class);

    @Autowired
    private GroupRepository groupRepo;

    @Autowired
    private MessageRouter messageRouter;

    @Override
    public void sendMessage(Chat.ChatMessage request, StreamObserver<Chat.ChatMessage> responseObserver) {
        String groupName = request.getGroup().getName();
        log.debug("sendMessage: request={}", request);

        groupRepo.getManagerAsync(groupName, manager -> {
            manager.sendMessageAsync(request, message -> {
                log.debug("sendMessage: request={}, response={}", request, message);
                responseObserver.onNext(message);
                responseObserver.onCompleted();
            });
        });
    }

    @Override
    public void getMessages(Chat.GroupManagerGetMessages request, StreamObserver<Chat.ChatMessage> responseObserver) {
        ServerCallStreamObserver<Chat.ChatMessage> streamObserver = (ServerCallStreamObserver<Chat.ChatMessage>) responseObserver;
        String groupName = request.getGroup().getName();
        String recipientId = request.getRecipientId();

        streamObserver.setOnCancelHandler(() -> {
            messageRouter.cancelRecipient(groupName, recipientId, streamObserver);
        });

        log.debug("getMessages: groupName={} recipientId={}", groupName, recipientId);
        messageRouter.addObserver(groupName, recipientId, streamObserver);
    }

}

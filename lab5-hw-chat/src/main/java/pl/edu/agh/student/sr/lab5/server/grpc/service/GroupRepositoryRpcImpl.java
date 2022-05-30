package pl.edu.agh.student.sr.lab5.server.grpc.service;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.edu.agh.student.sr.lab5.server.service.GroupRepository;
import pl.edu.agh.student.sr.lab5.proto.Chat;
import pl.edu.agh.student.sr.lab5.proto.GroupRepositoryGrpc;

@Service
public class GroupRepositoryRpcImpl extends GroupRepositoryGrpc.GroupRepositoryImplBase {
    private static final Logger log = LoggerFactory.getLogger(GroupRepositoryRpcImpl.class);

    @Autowired
    private GroupRepository groupRepo;

    @Override
    public void getGroup(Chat.GroupRepositoryGetGroup request, StreamObserver<Chat.Group> responseObserver) {
        String groupName = request.getName();
        log.debug("getGroup request={}", request);

        groupRepo.getManagerAsync(groupName, groupManager -> {
            Chat.Group group = Chat.Group.newBuilder()
                    .setName(groupName)
                    .build();

            log.debug("getGroup request={} response={}", request, group);
            responseObserver.onNext(group);
            responseObserver.onCompleted();
        });
    }

    @Override
    public void getGroups(Empty ignored, StreamObserver<Chat.GroupRepositoryGetGroups> responseObserver) {
        log.debug("getGroups");

        groupRepo.getGroupsAsync(groupNames -> {
            Chat.GroupRepositoryGetGroups response = Chat.GroupRepositoryGetGroups.newBuilder()
                    .addAllName(groupNames)
                    .build();

            log.debug("getGroups response={}", response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        });
    }
}
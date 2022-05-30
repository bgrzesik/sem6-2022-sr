package pl.edu.agh.student.sr.lab5.client;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import pl.edu.agh.student.sr.lab5.proto.Chat;
import pl.edu.agh.student.sr.lab5.proto.GroupManagerGrpc;

import java.util.Scanner;


public class ClientMain {
    private static final Empty EMPTY = Empty.newBuilder().build();

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        String author = scanner.nextLine();
        Chat.GroupManagerGetMessages getMessages = Chat.GroupManagerGetMessages.newBuilder()
                .setRecipientId(author)
                .setGroup(Chat.Group.newBuilder()
                        .setName("default"))
                .build();

        GroupManagerGrpc.newStub(channel).getMessages(getMessages, new StreamObserver<>() {
            @Override
            public void onNext(Chat.ChatMessage value) {
                System.out.printf("%s# <%s> %s\n", "aa", value.getAuthor(), value.getContent());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        });

        while (scanner.hasNext()) {
            String msg = scanner.nextLine();

            GroupManagerGrpc.GroupManagerBlockingStub groupManager = GroupManagerGrpc.newBlockingStub(channel);
            groupManager.sendMessage(Chat.ChatMessage.newBuilder()
                    .setGroup(Chat.Group.newBuilder()
                            .setName("default"))
                    .setAuthor(author)
                    .setContent(msg)
                    .build());
        }
    }
}

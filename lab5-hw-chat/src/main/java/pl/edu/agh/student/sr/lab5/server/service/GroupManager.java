package pl.edu.agh.student.sr.lab5.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import pl.edu.agh.student.sr.lab5.proto.Chat;
import pl.edu.agh.student.sr.lab5.server.model.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class GroupManager implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(GroupManager.class);
    private static final AtomicInteger MESSAGE_ID = new AtomicInteger(0);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Chat.ChatMessage> messages = new ArrayList<>();
    private final Group group;

    private  Consumer<Chat.ChatMessage> messageListener = null;

    public GroupManager(String groupName) {
        this.group = null;
    }

    public Future<?> sendMessageAsync(Chat.ChatMessage message, Consumer<Chat.ChatMessage> onDone) {
        return this.executor.submit(() -> this.sendMessage(message, onDone));
    }

    public void sendMessage(Chat.ChatMessage message, Consumer<Chat.ChatMessage> onDone) {
        log.debug("sendMassage: message={}", message.getContent());

        String messageId = Integer.toString(MESSAGE_ID.incrementAndGet(), 10 + 'Z' - 'A');
        Chat.ChatMessage.Builder builder = Chat.ChatMessage.newBuilder(message)
                .setMessageId(messageId);

        if (message.hasRepliesTo()) {
            String repliesToId = message.getRepliesTo().getMessageId();
            Optional<Chat.ChatMessage> optionalRepliesTo = messages.stream()
                    .filter(m -> Objects.equals(m.getMessageId(), repliesToId))
                    .findAny();

            optionalRepliesTo.ifPresent(m -> builder.setRepliesTo(Chat.ChatMessage.newBuilder(m)));
        }

        Chat.ChatMessage response = builder.build();
        log.debug("sendMassage: message={} response={}", message.getContent(), response.getMessageId());

        messages.add(response);
        if (messageListener != null) {
            messageListener.accept(response);
        }

        if (onDone != null) {
            onDone.accept(response);
        }
    }

    public Future<?> setMessageListenerAsync(Consumer<Chat.ChatMessage> messageListener) {
        return this.executor.submit(() -> this.setMessageListener(messageListener));
    }

    private void setMessageListener(Consumer<Chat.ChatMessage> messageListener) {
        log.debug("setMessageListener: message={}", messageListener);
        this.messageListener = messageListener;
    }

    @Override
    public void destroy() throws Exception {
        log.debug("destroy");
        log.info("Destroying group manager");
        executor.shutdown();
        assert executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}

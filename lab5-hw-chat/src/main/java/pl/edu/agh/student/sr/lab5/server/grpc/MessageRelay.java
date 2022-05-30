package pl.edu.agh.student.sr.lab5.server.grpc;

import io.grpc.stub.ServerCallStreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.student.sr.lab5.proto.Chat;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

public class MessageRelay {
    private static final Logger log = LoggerFactory.getLogger(MessageRelay.class);
    public static final int RELAY_REMOVAL_TIMEOUT = 30;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Queue<Chat.ChatMessage> pendingMessages = new ArrayDeque<>();
    private final String groupName;
    private final String recipientId;

    private ServerCallStreamObserver<Chat.ChatMessage> streamObserver = null;
    private Future<?> pendingPump = null;
    private ScheduledFuture<?> pendingRemoval = null;

    public MessageRelay(String groupName, String recipientId) {
        this.groupName = groupName;
        this.recipientId = recipientId;
    }

    public Future<?> onMessageAsync(Chat.ChatMessage message) {
        return this.executor.submit(() -> this.onMessage(message));
    }

    private void onMessage(Chat.ChatMessage message) {
        log.debug("onMessage: Queue message to {}@{}", recipientId, groupName);
        pendingMessages.add(message);
        schedulePumpPendingMessage();
    }

    private void schedulePumpPendingMessage() {
        log.debug("schedulePumpPendingMessage");
        if (pendingPump == null || pendingPump.isDone()) {
            pendingPump = this.executor.submit(this::pumpPendingMessages);
        }
    }

    private void pumpPendingMessages() {
        log.debug("pumpPendingMessages");
        if (streamObserver == null) {
            return;
        }

        while (!pendingMessages.isEmpty()) {
            Chat.ChatMessage message = pendingMessages.peek();
            log.debug("pumpPendingMessages: Sending message to {}@{}", recipientId, groupName);

            try {
                streamObserver.onNext(message);
            } catch (Throwable th) {
                log.error("Error while sending message to client {}@{}", recipientId, groupName, th);
                onTransportError(th);
                break;
            }

            pendingMessages.poll();
            cancelRemoval();
        }
    }

    public Future<?> setStreamObserverAsync(ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        return this.executor.submit(() -> this.setStreamObserver(streamObserver));
    }

    private void setStreamObserver(ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        log.debug("setStreamObserver streamObserver={}", streamObserver);
        removeStreamObserver();

        this.streamObserver = streamObserver;
        log.debug("setStreamObserver new streamObserver={}", this.streamObserver);

        if (!this.pendingMessages.isEmpty()) {
            schedulePumpPendingMessage();
        }
    }

    public Future<?> cancelObserverAsync(ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        return this.executor.submit(() -> this.cancelObserver(streamObserver));
    }

    private void cancelObserver(ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        log.debug("onCancel: recipientId={}", recipientId);
        if (this.streamObserver != streamObserver) {
            return;
        }

        removeStreamObserver();
    }

    private void removeStreamObserver() {
        log.debug("removeStreamObserver: streamObserver={}", streamObserver);
        if (streamObserver == null) {
            return;
        }

        try {
            streamObserver.onCompleted();
            streamObserver = null;
        } catch (Exception ex) {
            log.error("removeStreamObserver: onCompleted failed", ex);
        }

        scheduleRemoval();
    }

    private void cancelRemoval() {
        log.debug("cancelRemoval");
        if (pendingRemoval != null) {
            pendingRemoval.cancel(false);
            pendingRemoval = null;
        }
    }

    private void scheduleRemoval() {
        cancelRemoval();
        log.debug("scheduleRemoval: schedule");
        pendingRemoval = executor.schedule(this::removeRelay, RELAY_REMOVAL_TIMEOUT, TimeUnit.SECONDS);
    }

    private void removeRelay() {
        log.debug("removeRelay");
    }

    private void onTransportError(Throwable th) {
        log.debug("onTransportError");
        removeStreamObserver();
    }

}

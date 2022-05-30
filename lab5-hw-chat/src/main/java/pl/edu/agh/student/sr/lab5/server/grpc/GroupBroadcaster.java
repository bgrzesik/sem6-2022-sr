package pl.edu.agh.student.sr.lab5.server.grpc;

import io.grpc.stub.ServerCallStreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.student.sr.lab5.proto.Chat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GroupBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(GroupBroadcaster.class);
    private final Map<String, MessageRelay> relays = new HashMap<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final String groupName;

    public GroupBroadcaster(String groupName) {
        this.groupName = groupName;
    }

    private MessageRelay createRelay(String recipientId) {
        log.debug("createRelay: recipientId={}", recipientId);
        Lock lock = rwLock.writeLock();
        lock.lock();
        MessageRelay relay = relays.get(recipientId);
        if (relay == null) {
            relay = new MessageRelay(groupName, recipientId);
            relays.put(recipientId, relay);
            log.info("createRelay: new relay recipientId={}", recipientId);
        }
        lock.unlock();
        return relay;
    }

    public void addObserver(String recipientId, ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        log.debug("addObserver: recipientId={}", recipientId);
        Lock lock = rwLock.readLock();
        lock.lock();
        MessageRelay relay = relays.get(recipientId);
        if (relay == null) {
            lock.unlock();
            relay = createRelay(recipientId);
            lock.lock();
        }
        lock.unlock();

        relay.setStreamObserverAsync(streamObserver);
    }

    public void cancelRecipient(String recipientId, ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        log.debug("cancelRecipient: recipientId={}", recipientId);
        Lock lock = rwLock.readLock();
        lock.lock();
        MessageRelay relay = relays.get(recipientId);
        lock.unlock();

        relay.cancelObserverAsync(streamObserver);
    }

    public void broadcastMessageAsync(Chat.ChatMessage message) {
        log.debug("broadcastMessageAsync: groupName={}, message={}", groupName, message);
        Lock lock = rwLock.readLock();
        lock.lock();
        for (MessageRelay relay : relays.values()) {
            relay.onMessageAsync(message);
        }
        lock.unlock();
    }

    public void removeRecipient(String recipientId) {
        log.debug("removeRecipient: recipientId={}", recipientId);
        Lock lock = rwLock.writeLock();
        lock.lock();
        relays.remove(recipientId);
        lock.unlock();
    }

    public int getRecipientCount() {
        return relays.size();
    }

}

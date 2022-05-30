package pl.edu.agh.student.sr.lab5.server.grpc;

import io.grpc.stub.ServerCallStreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.edu.agh.student.sr.lab5.proto.Chat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class MessageRouter {
    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, GroupBroadcaster> groupBroadcasters = new HashMap<>();

    private GroupBroadcaster createGroupBroadcaster(String groupName) {
        GroupBroadcaster groupBroadcaster;

        Lock lock = rwLock.writeLock();
        lock.lock();
        groupBroadcaster = groupBroadcasters.get(groupName);
        if (groupBroadcaster == null) {
            groupBroadcaster = new GroupBroadcaster(groupName);
            groupBroadcasters.put(groupName, groupBroadcaster);
            log.info("createGroupBroadcaster: groupName={}", groupName);
        }
        lock.unlock();

        return groupBroadcaster;
    }

    public void addObserver(String groupName, String recipientId, ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        log.debug("addObserver: groupName={} recipientId={}", groupName, recipientId);
        Lock lock = rwLock.readLock();
        lock.lock();
        GroupBroadcaster groupBroadcaster = getGroupBroadcasterLocked(groupName, lock);
        groupBroadcaster.addObserver(recipientId, streamObserver);
        lock.unlock();
    }

    public GroupBroadcaster getGroupBroadcaster(String groupName) {
        Lock lock = rwLock.readLock();
        lock.lock();
        GroupBroadcaster groupBroadcaster = getGroupBroadcasterLocked(groupName, lock);
        lock.unlock();
        return groupBroadcaster;
    }

    private GroupBroadcaster getGroupBroadcasterLocked(String groupName, Lock lock) {
        GroupBroadcaster groupBroadcaster = groupBroadcasters.get(groupName);
        if (groupBroadcaster == null) {
            lock.unlock();
            groupBroadcaster = createGroupBroadcaster(groupName);
            lock.lock();
        }
        return groupBroadcaster;
    }

    public void removeRecipient(String groupName, String recipientId) {
        log.debug("removeRecipient: groupName={} recipientId={}", groupName, recipientId);
        Lock lock = rwLock.readLock();
        lock.lock();
        GroupBroadcaster groupBroadcaster = groupBroadcasters.get(groupName);
        if (groupBroadcaster != null) {
            groupBroadcaster.removeRecipient(recipientId);
        }
        lock.unlock();

        if (groupBroadcaster != null && groupBroadcaster.getRecipientCount() == 0) {
            lock = rwLock.writeLock();
            lock.lock();
            this.groupBroadcasters.remove(groupName);
            lock.unlock();
        }
    }

    public void cancelRecipient(String groupName, String recipientId, ServerCallStreamObserver<Chat.ChatMessage> streamObserver) {
        log.debug("cancelRecipient: groupName={} recipientId={}", groupName, recipientId);
        Lock lock = rwLock.readLock();
        lock.lock();
        GroupBroadcaster groupBroadcaster = groupBroadcasters.get(groupName);
        if (groupBroadcaster != null) {
            groupBroadcaster.cancelRecipient(recipientId, streamObserver);
        }
        lock.unlock();
    }

}

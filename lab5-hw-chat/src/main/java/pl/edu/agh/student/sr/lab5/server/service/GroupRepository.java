package pl.edu.agh.student.sr.lab5.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.edu.agh.student.sr.lab5.server.grpc.GroupBroadcaster;
import pl.edu.agh.student.sr.lab5.server.grpc.MessageRouter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class GroupRepository implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(GroupRepository.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, GroupManager> groupManagers = new HashMap<>();

    @Autowired
    private MessageRouter messageRouter;

    public Future<GroupManager> getManagerAsync(String groupName, Consumer<GroupManager> managerConsumer) {
        return executor.submit(() -> this.getManager(groupName, managerConsumer));
    }

    private GroupManager getManager(String groupName, Consumer<GroupManager> managerConsumer) {
        log.debug("getManager: groupName={}", groupName);
        GroupManager manager = groupManagers.get(groupName);
        if (manager == null) {
            log.info("getManager: Creating new group manager groupName={}", groupName);

            GroupBroadcaster broadcaster = messageRouter.getGroupBroadcaster(groupName);

            manager = new GroupManager(groupName);
            manager.setMessageListenerAsync(broadcaster::broadcastMessageAsync);

            groupManagers.put(groupName, manager);
        }
        if (managerConsumer != null) {
            managerConsumer.accept(manager);
        }
        return manager;
    }

    public Future<Set<String>> getGroupsAsync(Consumer<Set<String>> namesConsumer) {
        return executor.submit(() -> this.getGroups(namesConsumer));
    }

    private Set<String> getGroups(Consumer<Set<String>> namesConsumer) {
        Set<String> names = Collections.unmodifiableSet(groupManagers.keySet());
        if (namesConsumer != null) {
            namesConsumer.accept(names);
        }
        return names;
    }

    @Override
    public void destroy() throws Exception {
        log.info("Destroying group repository");
        executor.shutdown();
        assert executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        for (GroupManager manager : groupManagers.values()) {
            manager.destroy();
        }
    }

}

package pl.edu.agh.student.sr.lab5.server.grpc;

import java.util.Objects;

public class MessageRecipient {
    private final String groupName;
    private final String recipientId;

    public MessageRecipient(String groupName, String recipientId) {
        this.groupName = groupName;
        this.recipientId = recipientId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getRecipientId() {
        return recipientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageRecipient that = (MessageRecipient) o;
        return Objects.equals(groupName, that.groupName) && Objects.equals(recipientId, that.recipientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName, recipientId);
    }
}

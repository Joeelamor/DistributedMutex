import java.io.Serializable;

public class Message implements Serializable {
    public enum Type {
        REQ, RPY, REL, INI
    }

    private int senderId;
    private Type type;


    public Message(int senderId, Type type) {
        this.senderId = senderId;
        this.type = type;
    }

    public int getSenderId() {
        return senderId;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "senderId=" + senderId +
                ", type=" + type +
                '}';
    }
}

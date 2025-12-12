package core.model;

/**
 * Represents a message (method call) in a sequence diagram
 */
public class SequenceMessage {

    /**
     * Types of messages in sequence diagrams
     */
	// Note - Following the documentations for the library. Make sure everyone follows it.
    public enum MessageType {
        SYNC_CALL,      // Synchronous method call (solid arrow)
        ASYNC_CALL,     // Asynchronous method call (open arrow)
        RETURN,         // Return message (dashed arrow)
        CREATE,         // Object creation (dashed arrow to object)
        DESTROY         // Object destruction
    }

    private int sequence;        // Order of the message
    private String from;         // Sender (caller)
    private String to;           // Receiver (callee)
    private String label;        // Message label (method name + args)
    private MessageType type;    // Type of message

    public SequenceMessage(int sequence, String from, String to, String label, MessageType type) {
        this.sequence = sequence;
        this.from = from;
        this.to = to;
        this.label = label;
        this.type = type;
    }

    public int getSequence() {
        return sequence;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getLabel() {
        return label;
    }

    public MessageType getType() {
        return type;
    }

    /**
     * Check if this is a self-call (from and to are the same)
     */
    public boolean isSelfCall() {
        return from != null && from.equals(to);
    }

    @Override
    public String toString() {
        return "Message[" + sequence + ": " + from + " -> " + to + " : " + label + "]";
    }
}

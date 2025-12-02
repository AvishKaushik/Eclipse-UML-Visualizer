package core.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a sequence diagram showing method call interactions
 */
public class SequenceDiagram {

    private String className;
    private String methodName;
    private Set<String> participants; // Ordered set of participants (classes/objects)
    private List<SequenceMessage> messages; // Ordered list of messages

    public SequenceDiagram(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
        this.participants = new LinkedHashSet<>();
        this.messages = new ArrayList<>();
    }

    /**
     * Add a participant (class/object) to the sequence diagram
     * Normalizes participant name to avoid duplicates
     */
    public void addParticipant(String participant) {
        if (participant != null && !participant.trim().isEmpty()) {
            // Normalize participant name
            String normalized = normalizeParticipantName(participant);
            // Only add if normalization didn't result in an empty string
            if (normalized != null && !normalized.trim().isEmpty()) {
                participants.add(normalized);
            }
        }
    }

    /**
     * Normalize participant name to avoid duplicates from case differences
     */
    private String normalizeParticipantName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Trim whitespace
        name = name.trim();

        if (name.isEmpty()) {
            return null;
        }

        // Remove surrounding quotes if present (can happen with certain AST node types)
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 1) {
            name = name.substring(1, name.length() - 1);
            name = name.trim();
        }

        // If empty after removing quotes, return null
        if (name.isEmpty()) {
            return null;
        }

        // Handle method calls like "source.text()" -> extract base object "source"
        if (name.contains("(")) {
            // Remove everything from the first parenthesis onwards
            name = name.substring(0, name.indexOf("("));
            name = name.trim();
        }

        // Handle chained calls like "source.text" -> extract base object "source"
        if (name.contains(".")) {
            // Get the first part before any dots (the base object)
            name = name.substring(0, name.indexOf("."));
            name = name.trim();
        }

        // If empty after all processing, return null
        if (name.isEmpty()) {
            return null;
        }

        // Convert common patterns to class names
        // e.g., "this" -> use the current class name
        if (name.equals("this")) {
            return className;
        }

        // If it's a simple variable name (starts with lowercase), capitalize it
        // This helps consolidate "myClass" and "MyClass" into "MyClass"
        if (name.length() > 0 && Character.isLowerCase(name.charAt(0))) {
            // Capitalize first letter to match class naming convention
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        return name;
    }

    /**
     * Add a message (method call) to the sequence diagram
     * Normalizes participant names in the message
     */
    public void addMessage(SequenceMessage message) {
        if (message != null) {
            // Normalize the from and to participants in the message
            String normalizedFrom = normalizeParticipantName(message.getFrom());
            String normalizedTo = normalizeParticipantName(message.getTo());

            // Skip messages where normalization resulted in null (invalid participants)
            if (normalizedFrom == null || normalizedTo == null) {
                return; // Don't add invalid messages
            }

            // Create a new message with normalized names if they changed
            if (!normalizedFrom.equals(message.getFrom()) || !normalizedTo.equals(message.getTo())) {
                message = new SequenceMessage(
                    message.getSequence(),
                    normalizedFrom,
                    normalizedTo,
                    message.getLabel(),
                    message.getType()
                );
            }

            messages.add(message);
        }
    }

    /**
     * Get all participants ordered intelligently for better diagram flow
     * Uses a smarter algorithm that considers call patterns
     */
    public List<String> getParticipants() {
        if (messages.isEmpty()) {
            return new ArrayList<>(participants);
        }

        // Count outgoing calls for each participant
        java.util.Map<String, Integer> callCounts = new java.util.HashMap<>();
        for (SequenceMessage message : messages) {
            callCounts.put(message.getFrom(),
                callCounts.getOrDefault(message.getFrom(), 0) + 1);
        }

        // Start with the main class (makes the most calls, likely the initiator)
        String mainClass = className; // Start with diagram's main class

        // Order: main class first, then breadth-first by call order
        List<String> ordered = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        // Add main class first if it exists
        if (participants.contains(mainClass)) {
            ordered.add(mainClass);
            seen.add(mainClass);
        }

        // Add participants in order of first appearance, but prioritize callees
        for (SequenceMessage message : messages) {
            // Add caller if not seen
            if (!seen.contains(message.getFrom())) {
                ordered.add(message.getFrom());
                seen.add(message.getFrom());
            }
            // Add callee right after (creates natural left-to-right flow)
            if (!seen.contains(message.getTo())) {
                ordered.add(message.getTo());
                seen.add(message.getTo());
            }
        }

        // Add any remaining participants
        for (String participant : participants) {
            if (!seen.contains(participant)) {
                ordered.add(participant);
            }
        }

        return ordered;
    }

    /**
     * Get all messages in order
     */
    public List<SequenceMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getTitle() {
        return className + "." + methodName;
    }

    /**
     * Check if diagram has any content
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /**
     * Get the number of messages
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Get the number of participants
     */
    public int getParticipantCount() {
        return participants.size();
    }

    @Override
    public String toString() {
        return "SequenceDiagram[" + getTitle() + ", participants=" + participants.size()
                + ", messages=" + messages.size() + "]";
    }
}

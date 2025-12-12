package core.visualization;

import core.model.SequenceDiagram;
import core.model.SequenceMessage;
import core.model.SequenceMessage.MessageType;

/**
 * Generates PlantUML syntax from SequenceDiagram model
 */
public class PlantUMLGenerator {

    /**
     * Generate PlantUML syntax for a sequence diagram
     */
    public static String generatePlantUML(SequenceDiagram diagram) {
        if (diagram == null || diagram.isEmpty()) {
            return "@startuml\ntitle Empty Diagram\n@enduml";
        }

        StringBuilder uml = new StringBuilder();

        // Start UML
        uml.append("@startuml\n");

        // Add title
        uml.append("title ").append(escapeText(diagram.getTitle())).append("\n\n");

        // Add styling for better appearance
        uml.append("skinparam sequenceMessageAlign center\n");
        uml.append("skinparam responseMessageBelowArrow true\n");
        uml.append("skinparam shadowing false\n");
        uml.append("skinparam ParticipantPadding 20\n");
        uml.append("skinparam BoxPadding 10\n");

        // Show participant boxes at both top AND bottom for better readability
        uml.append("skinparam ParticipantFontStyle bold\n");
        uml.append("skinparam ParticipantFontSize 12\n");

        // Declare participants explicitly in the order they appear
        // This ensures left-to-right flow matches the call sequence
        java.util.List<String> participants = diagram.getParticipants();
        for (int i = 0; i < participants.size(); i++) {
            String participant = participants.get(i);
            String safeParticipant = sanitizeParticipantName(participant);

            // Use the safe participant name for display if original is empty/invalid
            String displayName = escapeText(participant);
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = safeParticipant;
            }

            uml.append("participant \"").append(displayName).append("\" as ")
                .append(safeParticipant);

            // Add explicit order hint to ALL participants (including last one!)
            uml.append(" order ").append(i);
            uml.append("\n");
        }
        uml.append("\n");

        // Add messages
        for (SequenceMessage message : diagram.getMessages()) {
            String fromSafe = sanitizeParticipantName(message.getFrom());
            String toSafe = sanitizeParticipantName(message.getTo());
            String label = escapeText(message.getLabel());

            switch (message.getType()) {
                case SYNC_CALL:
                    // Synchronous call: solid arrow
                    uml.append(fromSafe).append(" -> ").append(toSafe)
                        .append(" : ").append(label).append("\n");
                    break;

                case ASYNC_CALL:
                    // Asynchronous call: open arrow
                    uml.append(fromSafe).append(" ->> ").append(toSafe)
                        .append(" : ").append(label).append("\n");
                    break;

                case RETURN:
                    // Return message: dashed arrow
                    uml.append(fromSafe).append(" --> ").append(toSafe)
                        .append(" : ").append(label).append("\n");
                    break;

                case CREATE:
                    // Object creation - use regular arrow instead of ** to keep box at top
                    // The participant is already declared at the top, so just show the creation call
                    uml.append(fromSafe).append(" -> ").append(toSafe)
                        .append(" : <<create>> ").append(label).append("\n");
                    break;

                case DESTROY:
                    // Object destruction
                    uml.append("destroy ").append(toSafe).append("\n");
                    break;
            }
        }

        // End UML
        uml.append("\n@enduml");

        return uml.toString();
    }

    /**
     * Sanitize participant name to be a valid PlantUML identifier
     */
    private static String sanitizeParticipantName(String name) {
        if (name == null || name.isEmpty()) {
            return "Unknown";
        }

        // First, remove any surrounding quotes that might be in the name
        name = name.trim();
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 1) {
            name = name.substring(1, name.length() - 1);
        }

        // Remove method calls like "source.text()" -> just keep alphanumeric and dots
        // This is a safety check in case the normalization didn't catch everything
        name = name.replaceAll("\\(.*?\\)", "");

        // Replace special characters with underscores (dots, parentheses, etc.)
        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_");

        // Remove consecutive underscores
        sanitized = sanitized.replaceAll("_+", "_");

        // Remove leading/trailing underscores
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        // Ensure it doesn't start with a number
        if (sanitized.matches("^[0-9].*")) {
            sanitized = "_" + sanitized;
        }

        // Ensure we have something left
        if (sanitized.isEmpty()) {
            sanitized = "Unknown";
        }

        return sanitized;
    }

    /**
     * Escape special characters in text for PlantUML
     */
    private static String escapeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Unknown";
        }

        // First, remove any surrounding quotes that might be in the text
        text = text.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
            text = text.substring(1, text.length() - 1);
            text = text.trim();
        }

        // If we're left with an empty string after cleaning, return a default
        if (text.isEmpty()) {
            return "Unknown";
        }

        // Escape special PlantUML characters
        // Only escape quotes that are inside the text, not at the edges
        return text.replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\t", " ");
    }

    /**
     * Generate PlantUML for multiple methods in a class
     */
    public static String generateClassSequenceDiagram(SequenceDiagram diagram) {
        return generatePlantUML(diagram);
    }
}

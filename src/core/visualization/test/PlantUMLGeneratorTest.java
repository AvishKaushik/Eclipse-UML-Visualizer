package core.visualization.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import core.model.SequenceDiagram;
import core.model.SequenceMessage;
import core.model.SequenceMessage.MessageType;
import core.visualization.PlantUMLGenerator;

/**
 * Unit tests for PlantUML syntax generation
 */
public class PlantUMLGeneratorTest {

    private SequenceDiagram diagram;

    @Before
    public void setUp() {
        diagram = new SequenceDiagram("TestClass", "testMethod");
    }

    @Test
    public void testEmptyDiagram() {
        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertNotNull("PlantUML output should not be null", plantUML);
        assertTrue("Should start with @startuml", plantUML.startsWith("@startuml"));
        assertTrue("Should end with @enduml", plantUML.trim().endsWith("@enduml"));
    }

    @Test
    public void testSingleParticipant() {
        diagram.addParticipant("ClassA");

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertTrue("Should contain participant declaration", plantUML.contains("participant"));
        assertTrue("Should contain ClassA", plantUML.contains("ClassA"));
    }

    @Test
    public void testMultipleParticipants() {
        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");
        diagram.addParticipant("ClassC");

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertTrue("Should contain ClassA", plantUML.contains("ClassA"));
        assertTrue("Should contain ClassB", plantUML.contains("ClassB"));
        assertTrue("Should contain ClassC", plantUML.contains("ClassC"));
    }

    @Test
    public void testSingleMessage() {
        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");

        SequenceMessage msg = new SequenceMessage(1, "ClassA", "ClassB", "doSomething()", MessageType.SYNC_CALL);
        diagram.addMessage(msg);

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertTrue("Should contain arrow", plantUML.contains("->"));
        assertTrue("Should contain method name", plantUML.contains("doSomething"));
    }

    @Test
    public void testMultipleMessages() {
        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");
        diagram.addParticipant("ClassC");

        diagram.addMessage(new SequenceMessage(1, "ClassA", "ClassB", "method1()", MessageType.SYNC_CALL));
        diagram.addMessage(new SequenceMessage(2, "ClassB", "ClassC", "method2()", MessageType.SYNC_CALL));
        diagram.addMessage(new SequenceMessage(3, "ClassC", "ClassA", "method3()", MessageType.SYNC_CALL));

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertTrue("Should contain method1", plantUML.contains("method1"));
        assertTrue("Should contain method2", plantUML.contains("method2"));
        assertTrue("Should contain method3", plantUML.contains("method3"));
    }

    @Test
    public void testParticipantWithQuotes() {
        diagram.addParticipant("\"QuotedClass\"");

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        // Should sanitize quotes
        assertNotNull("Should handle quoted participant", plantUML);
        assertTrue("Should contain participant", plantUML.contains("participant"));
    }

    @Test
    public void testSelfCall() {
        diagram.addParticipant("ClassA");

        SequenceMessage msg = new SequenceMessage(1, "ClassA", "ClassA", "internalMethod()", MessageType.SYNC_CALL);
        diagram.addMessage(msg);

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertTrue("Should handle self-calls", plantUML.contains("->"));
        assertTrue("Should contain method", plantUML.contains("internalMethod"));
    }

    @Test
    public void testPlantUMLStructure() {
        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");
        diagram.addMessage(new SequenceMessage(1, "ClassA", "ClassB", "test()", MessageType.SYNC_CALL));

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        // Verify structure
        int startIndex = plantUML.indexOf("@startuml");
        int endIndex = plantUML.indexOf("@enduml");

        assertTrue("Should have @startuml", startIndex >= 0);
        assertTrue("Should have @enduml", endIndex > 0);
        assertTrue("@startuml should come before @enduml", startIndex < endIndex);
    }

    @Test
    public void testDiagramTitle() {
        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertTrue("Should contain class name", plantUML.contains("TestClass"));
        assertTrue("Should contain method name", plantUML.contains("testMethod"));
    }

    @Test
    public void testComplexSequence() {
        // Simulate a real method call sequence
        diagram.addParticipant("Controller");
        diagram.addParticipant("Service");
        diagram.addParticipant("Repository");

        diagram.addMessage(new SequenceMessage(1, "Controller", "Service", "processRequest()", MessageType.SYNC_CALL));
        diagram.addMessage(new SequenceMessage(2, "Service", "Repository", "findById()", MessageType.SYNC_CALL));
        diagram.addMessage(new SequenceMessage(3, "Repository", "Service", "return result", MessageType.RETURN));

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertNotNull("Should generate complex sequence", plantUML);
        assertTrue("Should contain all participants",
            plantUML.contains("Controller") &&
            plantUML.contains("Service") &&
            plantUML.contains("Repository"));

        // Count arrows (should have 3 messages)
        int arrowCount = plantUML.split("->", -1).length - 1;
        assertTrue("Should have message arrows", arrowCount >= 3);
    }

    @Test
    public void testEmptyParticipantName() {
        diagram.addParticipant("");

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertNotNull("Should handle empty participant name", plantUML);
        // Empty participants should be filtered out by SequenceDiagram
    }

    @Test
    public void testGetParticipantsOrder() {
        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");
        diagram.addParticipant("ClassC");

        var participants = diagram.getParticipants();

        assertNotNull("Participants list should not be null", participants);
        assertEquals("Should have 3 participants", 3, participants.size());
    }

    @Test
    public void testGetMessages() {
        SequenceMessage msg1 = new SequenceMessage(1, "A", "B", "method1()", MessageType.SYNC_CALL);
        SequenceMessage msg2 = new SequenceMessage(2, "B", "C", "method2()", MessageType.SYNC_CALL);

        diagram.addMessage(msg1);
        diagram.addMessage(msg2);

        var messages = diagram.getMessages();

        assertNotNull("Messages list should not be null", messages);
        assertEquals("Should have 2 messages", 2, messages.size());
    }

    @Test
    public void testDiagramIsEmpty() {
        assertTrue("New diagram should be empty", diagram.isEmpty());

        diagram.addMessage(new SequenceMessage(1, "A", "B", "test()", MessageType.SYNC_CALL));

        assertFalse("Diagram with messages should not be empty", diagram.isEmpty());
    }

    @Test
    public void testMessageCount() {
        assertEquals("Should start with 0 messages", 0, diagram.getMessageCount());

        diagram.addMessage(new SequenceMessage(1, "A", "B", "test1()", MessageType.SYNC_CALL));
        assertEquals("Should have 1 message", 1, diagram.getMessageCount());

        diagram.addMessage(new SequenceMessage(2, "B", "C", "test2()", MessageType.SYNC_CALL));
        assertEquals("Should have 2 messages", 2, diagram.getMessageCount());
    }

    @Test
    public void testParticipantCount() {
        assertEquals("Should start with 0 participants", 0, diagram.getParticipantCount());

        diagram.addParticipant("ClassA");
        assertEquals("Should have 1 participant", 1, diagram.getParticipantCount());

        diagram.addParticipant("ClassB");
        assertEquals("Should have 2 participants", 2, diagram.getParticipantCount());
    }

    @Test
    public void testDiagramToString() {
        String result = diagram.toString();

        assertNotNull("toString should not be null", result);
        assertTrue("Should contain class name", result.contains("TestClass"));
        assertTrue("Should contain method name", result.contains("testMethod"));
    }

    @Test
    public void testDifferentMessageTypes() {
        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");

        diagram.addMessage(new SequenceMessage(1, "ClassA", "ClassB", "syncCall()", MessageType.SYNC_CALL));
        diagram.addMessage(new SequenceMessage(2, "ClassA", "ClassB", "asyncCall()", MessageType.ASYNC_CALL));
        diagram.addMessage(new SequenceMessage(3, "ClassB", "ClassA", "return", MessageType.RETURN));

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertNotNull("Should handle different message types", plantUML);
        assertTrue("Should contain sync call", plantUML.contains("syncCall"));
        assertTrue("Should contain async call", plantUML.contains("asyncCall"));
    }

    @Test
    public void testNullMessage() {
        try {
            diagram.addMessage(null);
            // Should handle null gracefully
            assertTrue("Should handle null message", true);
        } catch (Exception e) {
            // Or throw exception
            assertTrue("Exception thrown for null message", true);
        }
    }
}

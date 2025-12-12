package core.parser.test;

import org.junit.Test;
import static org.junit.Assert.*;

import core.model.SequenceDiagram;
import core.model.SequenceMessage;
import core.visualization.PlantUMLGenerator;

/**
 * Test for sequence diagram generation
 */
public class SequenceDiagramTest {

    @Test
    public void testSequenceDiagramCreation() {
        SequenceDiagram diagram = new SequenceDiagram("TestClass", "testMethod");

        assertNotNull(diagram);
        assertEquals("TestClass", diagram.getClassName());
        assertEquals("testMethod", diagram.getMethodName());
        assertTrue(diagram.isEmpty());
    }

    @Test
    public void testAddParticipants() {
        SequenceDiagram diagram = new SequenceDiagram("TestClass", "testMethod");

        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");
        diagram.addParticipant("ClassC");

        assertEquals(3, diagram.getParticipantCount());
        assertTrue(diagram.getParticipants().contains("ClassA"));
        assertTrue(diagram.getParticipants().contains("ClassB"));
        assertTrue(diagram.getParticipants().contains("ClassC"));
    }

    @Test
    public void testAddMessages() {
        SequenceDiagram diagram = new SequenceDiagram("TestClass", "testMethod");

        diagram.addParticipant("ClassA");
        diagram.addParticipant("ClassB");

        SequenceMessage msg1 = new SequenceMessage(
            1, "ClassA", "ClassB", "doSomething()", SequenceMessage.MessageType.SYNC_CALL
        );

        diagram.addMessage(msg1);

        assertEquals(1, diagram.getMessageCount());
        assertFalse(diagram.isEmpty());
    }

    @Test
    public void testPlantUMLGeneration() {
        SequenceDiagram diagram = new SequenceDiagram("Student", "enrollInCourse");

        diagram.addParticipant("Student");
        diagram.addParticipant("Course");

        SequenceMessage msg1 = new SequenceMessage(
            1, "Student", "enrolledCourses", "contains(course)", SequenceMessage.MessageType.SYNC_CALL
        );
        SequenceMessage msg2 = new SequenceMessage(
            2, "Student", "enrolledCourses", "add(course)", SequenceMessage.MessageType.SYNC_CALL
        );

        diagram.addMessage(msg1);
        diagram.addMessage(msg2);

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertNotNull(plantUML);
        assertTrue(plantUML.contains("@startuml"));
        assertTrue(plantUML.contains("@enduml"));
        assertTrue(plantUML.contains("Student"));
        assertTrue(plantUML.contains("Course"));
        assertTrue(plantUML.contains("->"));
    }

    @Test
    public void testEmptyDiagramGeneration() {
        SequenceDiagram diagram = new SequenceDiagram("EmptyClass", "emptyMethod");

        String plantUML = PlantUMLGenerator.generatePlantUML(diagram);

        assertNotNull(plantUML);
        assertTrue(plantUML.contains("@startuml"));
        assertTrue(plantUML.contains("Empty Diagram"));
    }

    @Test
    public void testSelfCall() {
        SequenceMessage selfCall = new SequenceMessage(
            1, "ClassA", "ClassA", "internalMethod()", SequenceMessage.MessageType.SYNC_CALL
        );

        assertTrue(selfCall.isSelfCall());

        SequenceMessage normalCall = new SequenceMessage(
            2, "ClassA", "ClassB", "externalMethod()", SequenceMessage.MessageType.SYNC_CALL
        );

        assertFalse(normalCall.isSelfCall());
    }

    @Test
    public void testMessageTypes() {
        SequenceMessage syncCall = new SequenceMessage(
            1, "A", "B", "sync()", SequenceMessage.MessageType.SYNC_CALL
        );
        assertEquals(SequenceMessage.MessageType.SYNC_CALL, syncCall.getType());

        SequenceMessage asyncCall = new SequenceMessage(
            2, "A", "B", "async()", SequenceMessage.MessageType.ASYNC_CALL
        );
        assertEquals(SequenceMessage.MessageType.ASYNC_CALL, asyncCall.getType());

        SequenceMessage returnMsg = new SequenceMessage(
            3, "B", "A", "result", SequenceMessage.MessageType.RETURN
        );
        assertEquals(SequenceMessage.MessageType.RETURN, returnMsg.getType());

        SequenceMessage createMsg = new SequenceMessage(
            4, "A", "B", "new B()", SequenceMessage.MessageType.CREATE
        );
        assertEquals(SequenceMessage.MessageType.CREATE, createMsg.getType());
    }
}

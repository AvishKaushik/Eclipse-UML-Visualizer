package core.model.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import core.model.Note;
import core.model.NoteReply;

/**
 * Unit tests for Note and NoteReply classes
 */
public class NoteTest {

    private Note note;
    private String testContent;
    private String testAuthor;
    private String testElementId;

    @Before
    public void setUp() {
        testContent = "This is a test note";
        testAuthor = "testUser";
        testElementId = "com.example.TestClass";
        note = new Note(testContent, testAuthor, testElementId);
    }

    @Test
    public void testNoteCreation() {
        assertNotNull("Note should be created", note);
        assertNotNull("Note should have an ID", note.getId());
        assertEquals("Content should match", testContent, note.getContent());
        assertEquals("Author should match", testAuthor, note.getAuthor());
        assertEquals("Element ID should match", testElementId, note.getLinkedElementId());
        assertTrue("Note should be active by default", note.isActive());
        assertNotNull("Note should have replies list", note.getReplies());
        assertEquals("Note should have no replies initially", 0, note.getReplies().size());
    }

    @Test
    public void testNoteIdUniqueness() {
        Note note1 = new Note("Content 1", "User1", "Element1");
        Note note2 = new Note("Content 2", "User2", "Element2");

        assertNotEquals("Note IDs should be unique", note1.getId(), note2.getId());
    }

    @Test
    public void testSetContent() {
        String newContent = "Updated content";
        note.setContent(newContent);
        assertEquals("Content should be updated", newContent, note.getContent());
    }

    @Test
    public void testSetActive() {
        assertTrue("Note should be active initially", note.isActive());

        note.setActive(false);
        assertFalse("Note should be inactive", note.isActive());

        note.setActive(true);
        assertTrue("Note should be active again", note.isActive());
    }

    @Test
    public void testAddReply() {
        assertEquals("Should have no replies initially", 0, note.getReplies().size());

        NoteReply reply1 = new NoteReply("First reply", "user1");
        note.addReply(reply1);

        assertEquals("Should have 1 reply", 1, note.getReplies().size());
        assertEquals("Reply should match", reply1, note.getReplies().get(0));

        NoteReply reply2 = new NoteReply("Second reply", "user2");
        note.addReply(reply2);

        assertEquals("Should have 2 replies", 2, note.getReplies().size());
    }

    @Test
    public void testAddMultipleReplies() {
        for (int i = 0; i < 5; i++) {
            NoteReply reply = new NoteReply("Reply " + i, "user" + i);
            note.addReply(reply);
        }

        assertEquals("Should have 5 replies", 5, note.getReplies().size());

        // Verify order is maintained
        for (int i = 0; i < 5; i++) {
            assertEquals("Reply content should match order",
                "Reply " + i, note.getReplies().get(i).getContent());
        }
    }

    @Test
    public void testGetFormattedDate() {
        String formattedDate = note.getFormattedDate();

        assertNotNull("Formatted date should not be null", formattedDate);
        assertTrue("Formatted date should contain year", formattedDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    public void testToString() {
        String noteString = note.toString();

        assertNotNull("toString should not return null", noteString);
        assertTrue("toString should contain content", noteString.contains(testContent));
        assertTrue("toString should contain author", noteString.contains(testAuthor));
        assertTrue("toString should contain reply count", noteString.contains("0 replies"));
    }

    @Test
    public void testToStringWithReplies() {
        note.addReply(new NoteReply("Reply 1", "user1"));
        note.addReply(new NoteReply("Reply 2", "user2"));

        String noteString = note.toString();
        assertTrue("toString should show reply count", noteString.contains("2 replies"));
    }

    @Test
    public void testToStringInactive() {
        note.setActive(false);
        String noteString = note.toString();

        assertTrue("toString should indicate inactive status", noteString.contains("INACTIVE"));
    }

    @Test
    public void testNoteReplyCreation() {
        String replyContent = "Test reply";
        String replyAuthor = "replyUser";

        NoteReply reply = new NoteReply(replyContent, replyAuthor);

        assertNotNull("Reply should be created", reply);
        assertNotNull("Reply should have an ID", reply.getId());
        assertEquals("Reply content should match", replyContent, reply.getContent());
        assertEquals("Reply author should match", replyAuthor, reply.getAuthor());
        assertNotNull("Reply should have creation timestamp", reply.getCreated());
    }

    @Test
    public void testNoteReplyIdUniqueness() {
        NoteReply reply1 = new NoteReply("Content 1", "User1");
        NoteReply reply2 = new NoteReply("Content 2", "User2");

        assertNotEquals("Reply IDs should be unique", reply1.getId(), reply2.getId());
    }

    @Test
    public void testNoteReplyFormattedDate() {
        NoteReply reply = new NoteReply("Test", "User");
        String formattedDate = reply.getFormattedDate();

        assertNotNull("Reply formatted date should not be null", formattedDate);
        assertTrue("Reply formatted date should match pattern",
            formattedDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    public void testNoteReplyToString() {
        NoteReply reply = new NoteReply("Test reply content", "testUser");
        String replyString = reply.toString();

        assertNotNull("Reply toString should not be null", replyString);
        assertTrue("Reply toString should contain content", replyString.contains("Test reply content"));
        assertTrue("Reply toString should contain author", replyString.contains("testUser"));
    }

    @Test
    public void testNoteWithEmptyContent() {
        Note emptyNote = new Note("", "user", "element");
        assertEquals("Empty content should be allowed", "", emptyNote.getContent());
    }

    @Test
    public void testNoteWithSpecialCharacters() {
        String specialContent = "Note with special chars: @#$%^&*()[]{}|\\\"'<>?/";
        Note specialNote = new Note(specialContent, "user", "element");
        assertEquals("Special characters should be preserved", specialContent, specialNote.getContent());
    }

    @Test
    public void testNoteWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("This is a very long note. ");
        }

        Note longNote = new Note(longContent.toString(), "user", "element");
        assertEquals("Long content should be preserved", longContent.toString(), longNote.getContent());
    }

    @Test
    public void testNoteLinkedToClass() {
        String classId = "com.example.MyClass";
        Note classNote = new Note("Class note", "user", classId);
        assertEquals("Should link to class", classId, classNote.getLinkedElementId());
    }

    @Test
    public void testNoteLinkedToRelation() {
        String relationId = "ClassA->ClassB";
        Note relationNote = new Note("Relation note", "user", relationId);
        assertEquals("Should link to relation", relationId, relationNote.getLinkedElementId());
    }
}

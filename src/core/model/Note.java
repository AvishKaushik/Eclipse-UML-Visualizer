package core.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a note attached to a diagram element with reply support
 */

// Story 4-01 Note Data Model & Storage
public class Note {
    private String id;
    private String content;
    private String author;
    private LocalDateTime created;
    private String linkedElementId; // Class ID or Relation ID
    private boolean active;
    private List<NoteReply> replies;
    
    public Note(String content, String author, String linkedElementId) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.author = author;
        this.created = LocalDateTime.now();
        this.linkedElementId = linkedElementId;
        this.active = true;
        this.replies = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public LocalDateTime getCreated() {
        return created;
    }
    
    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return created.format(formatter);
    }
    
    public String getLinkedElementId() {
        return linkedElementId;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public List<NoteReply> getReplies() {
        return replies;
    }
    
    public void addReply(NoteReply reply) {
        this.replies.add(reply);
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s by %s%s (%d replies)", 
            getFormattedDate(), 
            content, 
            author,
            active ? "" : " (INACTIVE)",
            replies.size());
    }
}


package core.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents a reply to a note
 */
// Story 4-05
public class NoteReply {
    private String id;
    private String content;
    private String author;
    // Note to teammates : adding this because it was part of requirements. Saw it later on.
    private LocalDateTime created; 
    
    public NoteReply(String content, String author) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.author = author;
        this.created = LocalDateTime.now();
    }
    
    public String getId() {
        return id;
    }
    
    public String getContent() {
        return content;
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
    
    @Override
    public String toString() {
        return String.format("[%s] %s by %s", 
            getFormattedDate(), content, author);
    }
}

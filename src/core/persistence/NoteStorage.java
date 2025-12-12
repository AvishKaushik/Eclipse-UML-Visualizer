package core.persistence;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import core.model.Note;

/**
 * Persists notes to JSON in project directory (Git-shareable)
 * Phase 4: Notes System
 */
public class NoteStorage {

    private static final String NOTES_DIR = ".uml-notes";
    private static final String NOTES_FILE = "notes.json"; // Check if notes are getting stored once integrated.
    private Gson gson;
    private IProject project;

    public NoteStorage(IProject project) {
        this.project = project;
        gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    }
    
    /**
     * Save notes to project directory (.uml-notes/notes.json)
     * This file should be committed to Git for team collaboration
     */
    public void saveNotes(List<Note> notes) throws IOException {
        File file = getNotesFile();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(notes, writer);
        }
    }
    
    /**
     * Load notes from project directory (.uml-notes/notes.json)
     */
    public List<Note> loadNotes() throws IOException {
        File file = getNotesFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Note>>(){}.getType();
            List<Note> notes = gson.fromJson(reader, listType);
            return notes != null ? notes : new ArrayList<>();
        }
    }
    
    private File getNotesFile() {
        if (project == null) {
            // Fallback to temp directory if no project
            return new File(System.getProperty("java.io.tmpdir"), NOTES_FILE);
        }

        // Store in project directory under .uml-notes/
        File projectLocation = project.getLocation().toFile();
        File notesDir = new File(projectLocation, NOTES_DIR);

        // Create directory if it doesn't exist
        if (!notesDir.exists()) {
            notesDir.mkdirs();
        }

        return new File(notesDir, NOTES_FILE);
    }
    
    /**
     * Custom adapter for LocalDateTime
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, 
                                                         JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, 
                                    JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, 
                                        JsonDeserializationContext context) 
                                        throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}
package core.model;

import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Serializer to convert IR to JSON for debugging.
 * Story 2-02: Provide a way to serialize/deserialize to JSON
 */
public class IRJsonSerializer {
    
    private Gson gson;
    
    public IRJsonSerializer() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Convert IR to JSON string
     */
    public String toJson(IntermediateRepresentation ir) {
        return gson.toJson(ir.getAllClasses());
    }
    
    /**
     * Convert IR with relations to JSON string
     */
    public String toJsonWithRelations(IntermediateRepresentation ir) {
        IRSnapshot snapshot = new IRSnapshot(ir.getAllClasses(), ir.getAllRelations());
        return gson.toJson(snapshot);
    }
    
    /**
     * Save IR to JSON file
     */
    public void saveToFile(IntermediateRepresentation ir, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(ir.getAllClasses(), writer);
        }
    }
    
    /**
     * Save IR with relations to JSON file
     */
    public void saveToFileWithRelations(IntermediateRepresentation ir, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            IRSnapshot snapshot = new IRSnapshot(ir.getAllClasses(), ir.getAllRelations());
            gson.toJson(snapshot, writer);
        }
    }
    
    /**
     * Print to console
     */
    public void printToConsole(IntermediateRepresentation ir) {
        System.out.println("=== Intermediate Representation (JSON) ===");
        System.out.println(toJsonWithRelations(ir));
    }
    
    /**
     * Snapshot class for JSON serialization
     */
    public static class IRSnapshot {
        public java.util.Map<String, ClassNode> classes;
        public java.util.List<Relation> relations;
        
        public IRSnapshot(java.util.Map<String, ClassNode> classes, java.util.List<Relation> relations) {
            this.classes = classes;
            this.relations = relations;
        }
    }
}
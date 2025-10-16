package core.parser;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utility to output parsed results as JSON.
 * Story 2-01: Sample JSON dump of parsed results
 */
public class ParserJsonOutput {
    
    private Gson gson;
    
    public ParserJsonOutput() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Convert list of ClassInfo to JSON string
     */
    public String toJson(List<ClassInfo> classes) {
        return gson.toJson(classes);
    }
    
    /**
     * Save parsed results to JSON file
     */
    public void saveToFile(List<ClassInfo> classes, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(classes, writer);
        }
    }
    
    /**
     * Pretty print to console
     */
    public void printToConsole(List<ClassInfo> classes) {
        System.out.println("=== Parsed Classes ===");
        System.out.println(toJson(classes));
    }
}
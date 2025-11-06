package core.persistence;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Persists user configurations and preferences
 */
public class ConfigurationStorage {
    
    private static final String CONFIG_FILE = "uml_config.json";
    private Gson gson;
    
    public ConfigurationStorage() {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Save configuration
     */
    public void saveConfiguration(Map<String, String> config) throws IOException {
        File file = getConfigFile();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(config, writer);
        }
    }
    
    /**
     * Load configuration
     */
    public Map<String, String> loadConfiguration() throws IOException {
        File file = getConfigFile();
        if (!file.exists()) {
            return new HashMap<>();
        }
        
        try (FileReader reader = new FileReader(file)) {
            java.lang.reflect.Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> config = gson.fromJson(reader, type);
            return config != null ? config : new HashMap<>();
        }
    }
    
    /**
     * Save a single setting
     */
    public void saveSetting(String key, String value) throws IOException {
        Map<String, String> config = loadConfiguration();
        config.put(key, value);
        saveConfiguration(config);
    }
    
    /**
     * Get a single setting
     */
    public String getSetting(String key, String defaultValue) {
        try {
            Map<String, String> config = loadConfiguration();
            return config.getOrDefault(key, defaultValue);
        } catch (IOException e) {
            return defaultValue;
        }
    }
    
    private File getConfigFile() {
        File stateLocation = Platform.getStateLocation(
            Platform.getBundle("Eclipse-UML-Visualizer")).toFile();
        return new File(stateLocation, CONFIG_FILE);
    }
}
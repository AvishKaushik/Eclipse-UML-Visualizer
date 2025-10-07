package core.parser;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo {
    private String name;
    private String type; // "class", "interface", "enum"
    private List<FieldInfo> fields;
    private List<MethodInfo> methods;
    
    public ClassInfo(String name) {
        this(name, "class");
    }
    
    public ClassInfo(String name, String type) {
        this.name = name;
        this.type = type;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }
    
    public List<FieldInfo> getFields() {
        return fields;
    }
    
    public void addField(FieldInfo field) {
        this.fields.add(field);
    }
    
    public List<MethodInfo> getMethods() {
        return methods;
    }
    
    public void addMethod(MethodInfo method) {
        this.methods.add(method);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s (fields=%d, methods=%d)", 
            type, name, fields.size(), methods.size());
    }
}
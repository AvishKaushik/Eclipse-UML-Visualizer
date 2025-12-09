package core.parser;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo {
    private String name;
    private String type; // "class", "interface", "enum"
    private String packageName;
    private List<FieldInfo> fields;
    private List<MethodInfo> methods;
    private List<String> imports; // Import statements

    public ClassInfo(String name) {
        this(name, "class", "");
    }

    public ClassInfo(String name, String type) {
        this(name, type, "");
    }

    public ClassInfo(String name, String type, String packageName) {
        this.name = name;
        this.type = type;
        this.packageName = packageName != null ? packageName : "";
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.imports = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPackageName() {
        return packageName;
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

    public List<String> getImports() {
        return imports;
    }

    public void addImport(String importStatement) {
        if (importStatement != null && !importStatement.isEmpty()) {
            this.imports.add(importStatement);
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s (fields=%d, methods=%d, imports=%d)",
            type, name, fields.size(), methods.size(), imports.size());
    }
}
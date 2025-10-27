package core.model;

import java.util.ArrayList;
import java.util.List;

public class ClassNode {
    private String id;
    private String name;
    private String type; // "class", "interface", "enum"
    private List<Field> fields;
    private List<Method> methods;
    private String packageName;
    
    public ClassNode(String name) {
        this(name, "class", "");
    }
    
    public ClassNode(String name, String type, String packageName) {
        this.id = generateId(packageName, name);
        this.name = name;
        this.type = type;
        this.packageName = packageName;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }
    
    private String generateId(String pkg, String name) {
        return pkg.isEmpty() ? name : pkg + "." + name;
    }
    
    public String getId() {
        return id;
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
    
    public List<Field> getFields() {
        return fields;
    }
    
    public void addField(Field field) {
        this.fields.add(field);
    }
    
    public List<Method> getMethods() {
        return methods;
    }
    
    public void addMethod(Method method) {
        this.methods.add(method);
    }
    
    @Override
    public String toString() {
        return String.format("ClassNode[id=%s, type=%s, fields=%d, methods=%d]", 
            id, type, fields.size(), methods.size());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClassNode that = (ClassNode) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
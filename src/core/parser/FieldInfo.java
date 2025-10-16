package core.parser;


public class FieldInfo {
    private String name;
    private String type;
    private String visibility; // "public", "private", "protected", "package"
    private boolean isStatic;
    
    public FieldInfo(String name, String type) {
        this(name, type, "package", false);
    }
    
    public FieldInfo(String name, String type, String visibility, boolean isStatic) {
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.isStatic = isStatic;
    }
    
    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }
    
    public String getVisibility() {
        return visibility;
    }
    
    public boolean isStatic() {
        return isStatic;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %s", visibility, type, name);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FieldInfo that = (FieldInfo) obj;
        return name.equals(that.name) && type.equals(that.type);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() + type.hashCode();
    }
}
package core.parser;

import java.util.ArrayList;
import java.util.List;


public class MethodInfo {
    private String name;
    private String returnType;
    private List<ParameterInfo> parameters;
    private String visibility; // "public", "private", "protected", "package"
    private boolean isStatic;
    
    public MethodInfo(String name, String returnType) {
        this(name, returnType, "package", false);
    }
    
    public MethodInfo(String name, String returnType, String visibility, boolean isStatic) {
        this.name = name;
        this.returnType = returnType;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.parameters = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getReturnType() {
        return returnType;
    }
    
    public String getVisibility() {
        return visibility;
    }
    
    public boolean isStatic() {
        return isStatic;
    }
    
    public List<ParameterInfo> getParameters() {
        return parameters;
    }
    
    public void addParameter(ParameterInfo param) {
        this.parameters.add(param);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %s()", visibility, returnType, name);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MethodInfo that = (MethodInfo) obj;
        return name.equals(that.name) && returnType.equals(that.returnType);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() + returnType.hashCode();
    }
}

/**
 * Represents a method parameter
 */
class ParameterInfo {
    private String name;
    private String type;
    
    public ParameterInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }
}
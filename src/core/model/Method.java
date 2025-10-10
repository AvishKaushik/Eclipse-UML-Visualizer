package core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal Representation of a method.
 * Story 2-02: Build IR for classes/relations
 */
public class Method {
    private String name;
    private String returnType;
    private List<Parameter> parameters;
    private String visibility; // "public", "private", "protected", "package"
    private boolean isStatic;
    
    public Method(String name, String returnType) {
        this(name, returnType, "package", false);
    }
    
    public Method(String name, String returnType, String visibility, boolean isStatic) {
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
    
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    public void addParameter(Parameter param) {
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
        Method that = (Method) obj;
        return name.equals(that.name) && returnType.equals(that.returnType);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() + returnType.hashCode();
    }
}

/**
 * Internal Representation of a method parameter
 */
class Parameter {
    private String name;
    private String type;
    
    public Parameter(String name, String type) {
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
package core.model;

import java.util.List;

import core.parser.ClassInfo;
import core.parser.FieldInfo;
import core.parser.MethodInfo;

/**
 * Builder to construct IR from parsed ClassInfo objects.
 * Story 2-02: Build IR for classes/relations
 */
public class IRBuilder {
    
    private IntermediateRepresentation ir;
    
    public IRBuilder() {
        this.ir = new IntermediateRepresentation();
    }
    
    /**
     * Build IR from a list of parsed classes
     */
    public IntermediateRepresentation build(List<ClassInfo> parsedClasses) {
        // Convert parsed classes to IR ClassNodes
        for (ClassInfo classInfo : parsedClasses) {
            ClassNode classNode = convertToClassNode(classInfo);
            ir.addClass(classNode);
        }
        return ir;
    }
    
    /**
     * Convert a parsed ClassInfo to IR ClassNode
     */
    private ClassNode convertToClassNode(ClassInfo classInfo) {
        ClassNode classNode = new ClassNode(classInfo.getName(), classInfo.getType(), "");
        
        // Add fields
        for (FieldInfo fieldInfo : classInfo.getFields()) {
            Field field = new Field(
                fieldInfo.getName(),
                fieldInfo.getType(),
                fieldInfo.getVisibility(),
                fieldInfo.isStatic()
            );
            classNode.addField(field);
        }
        
        // Add methods
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            Method method = new Method(
                methodInfo.getName(),
                methodInfo.getReturnType(),
                methodInfo.getVisibility(),
                methodInfo.isStatic()
            );
            classNode.addMethod(method);
        }
        
        return classNode;
    }
    
    /**
     * Get the built IR
     */
    public IntermediateRepresentation getIR() {
        return ir;
    }
}
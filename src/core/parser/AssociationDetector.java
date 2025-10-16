package core.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import core.model.ClassNode;
import core.model.IntermediateRepresentation;
import core.model.Relation;

/**
 * Detects associations, aggregations, and compositions from field declarations.
 * Story 2-04: Detect associations/aggregation/composition
 */
public class AssociationDetector {
    
    private IntermediateRepresentation ir;
    private List<ClassNode> projectClasses;
    
    public AssociationDetector(IntermediateRepresentation ir, List<ClassNode> projectClasses) {
        this.ir = ir;
        this.projectClasses = projectClasses;
    }
    
    /**
     * Detect associations for all types
     */
    public void detectAssociations(List<IType> types) throws JavaModelException {
        for (IType type : types) {
            detectFieldAssociations(type);
        }
    }
    
    /**
     * Detect associations from fields of a type
     */
    private void detectFieldAssociations(IType type) throws JavaModelException {
        for (IField field : type.getFields()) {
            String fieldType = Signature.toString(field.getTypeSignature());
            
            // Check if it's a collection
            if (isCollection(fieldType)) {
                detectAggregation(type, field, fieldType);
            } else {
                // Simple field type
                detectSimpleAssociation(type, field, fieldType);
            }
        }
    }
    
    /**
     * Detect simple associations or composition
     */
    private void detectSimpleAssociation(IType sourceType, IField field, String fieldType) 
            throws JavaModelException {
        // Extract simple class name
        String className = extractClassName(fieldType);
        
        // Find if it's in project
        ClassNode targetClass = findClassInProject(className);
        if (targetClass == null) {
            return; // Not found in project, likely external
        }
        
        // Determine if it's composition or association
        String relationType = determineRelationType(sourceType, field);
        
        Relation relation = new Relation(
            sourceType.getFullyQualifiedName(),
            targetClass.getId(),
            relationType,
            false
        );
        
        ir.addRelation(relation);
    }
    
    /**
     * Detect aggregation from collection fields
     */
    private void detectAggregation(IType sourceType, IField field, String fieldType) 
            throws JavaModelException {
        // Extract element type from collection (e.g., List<Foo> → Foo)
        String elementType = extractCollectionElementType(fieldType);
        
        if (elementType == null) {
            return; // Could not extract element type
        }
        
        String className = extractClassName(elementType);
        
        // Find if it's in project
        ClassNode targetClass = findClassInProject(className);
        if (targetClass == null) {
            return; // Not found in project
        }
        
        Relation relation = new Relation(
            sourceType.getFullyQualifiedName(),
            targetClass.getId(),
            "aggregation",
            false
        );
        
        ir.addRelation(relation);
    }
    
    /**
     * Determine if relation is composition or association
     * Composition: private field initialized in constructor/initializer
     * Association: otherwise
     */
    private String determineRelationType(IType type, IField field) throws JavaModelException {
        // Get field initialization
        // If field is initialized with 'new', it's likely composition
        // For simplicity, check field modifiers
        
        // If field is private and not static, likely composition
        if (org.eclipse.jdt.core.Flags.isPrivate(field.getFlags())) {
            return "composition";
        }
        
        return "association";
    }
    
    /**
     * Check if a type is a collection (List, Set, Map, etc.)
     */
    private boolean isCollection(String fieldType) {
        return fieldType.contains("List") || 
               fieldType.contains("Set") || 
               fieldType.contains("Collection") ||
               fieldType.contains("Map") ||
               fieldType.contains("Queue") ||
               fieldType.contains("Deque");
    }
    
    /**
     * Extract element type from collection (e.g., List<Foo> → Foo)
     */
    private String extractCollectionElementType(String fieldType) {
        Pattern pattern = Pattern.compile("<(.+?)>");
        Matcher matcher = pattern.matcher(fieldType);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extract simple class name from type string
     * e.g., "com.example.Foo" → "Foo"
     */
    private String extractClassName(String fieldType) {
        // Remove generic parameters
        String cleanType = fieldType.split("<")[0];
        
        // Get last part (simple name)
        String[] parts = cleanType.split("\\.");
        return parts[parts.length - 1];
    }
    
    /**
     * Find a class in the project by simple name
     */
    private ClassNode findClassInProject(String className) {
        for (ClassNode classNode : projectClasses) {
            if (classNode.getName().equals(className)) {
                return classNode;
            }
        }
        return null;
    }
}
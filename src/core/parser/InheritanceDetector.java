package core.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import core.model.IntermediateRepresentation;
import core.model.Relation;

/**
 * Detects inheritance and interface implementations.
 * Story 2-03: Detect inheritance & interfaces
 */
public class InheritanceDetector {
    
    private IntermediateRepresentation ir;
    
    public InheritanceDetector(IntermediateRepresentation ir) {
        this.ir = ir;
    }
    
    /**
     * Detect inheritance relationships for a list of types
     */
    public void detectInheritance(List<IType> types) throws JavaModelException {
        for (IType type : types) {
            detectInheritanceForType(type);
            detectImplementedInterfaces(type);
        }
    }
    
    /**
     * Detect inheritance (extends) for a single type
     */
    private void detectInheritanceForType(IType type) throws JavaModelException {
        String superclassName = type.getSuperclassName();
        
        if (superclassName != null && !superclassName.equals("Object")) {
            // Resolve fully qualified name
            String fullyQualifiedSuper = resolveType(type, superclassName);
            
            // Determine if external
            boolean isExternal = isExternalClass(fullyQualifiedSuper, type);
            
            // Create relation
            Relation relation = new Relation(
                type.getFullyQualifiedName(),
                fullyQualifiedSuper,
                "inheritance",
                isExternal
            );
            
            ir.addRelation(relation);
        }
    }
    
    /**
     * Detect interface implementations for a single type
     */
    private void detectImplementedInterfaces(IType type) throws JavaModelException {
        String[] interfaceNames = type.getSuperInterfaceNames();
        
        for (String interfaceName : interfaceNames) {
            // Resolve fully qualified name
            String fullyQualifiedInterface = resolveType(type, interfaceName);
            
            // Determine if external
            boolean isExternal = isExternalClass(fullyQualifiedInterface, type);
            
            // Create relation
            Relation relation = new Relation(
                type.getFullyQualifiedName(),
                fullyQualifiedInterface,
                "implements",
                isExternal
            );
            
            ir.addRelation(relation);
        }
    }
    
    /**
     * Resolve a type name to its fully qualified name
     */
    private String resolveType(IType type, String typeName) throws JavaModelException {
        String[][] resolvedTypes = type.resolveType(typeName);
        
        if (resolvedTypes != null && resolvedTypes.length > 0) {
            // resolvedTypes[0][0] = package, resolvedTypes[0][1] = simple name
            String pkg = resolvedTypes[0][0];
            String simpleName = resolvedTypes[0][1];
            
            if (pkg.isEmpty()) {
                return simpleName;
            } else {
                return pkg + "." + simpleName;
            }
        }
        
        // If not resolved, return as-is (likely external)
        return typeName;
    }
    
    /**
     * Check if a class is external (not in current project)
     */
    private boolean isExternalClass(String fullyQualifiedName, IType sourceType) {
        // External if it starts with java.*, javax.*, etc. or not in IR
        if (fullyQualifiedName.startsWith("java.") || 
            fullyQualifiedName.startsWith("javax.") ||
            fullyQualifiedName.startsWith("sun.")) {
            return true;
        }
        
        // Check if class exists in IR
        return ir.getClass(fullyQualifiedName) == null;
    }
}
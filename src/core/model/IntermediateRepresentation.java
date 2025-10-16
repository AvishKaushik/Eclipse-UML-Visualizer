package core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal Representation (IR) container for all parsed classes and relations.
 * Story 2-02: Build IR for classes/relations
 */
public class IntermediateRepresentation {
    private Map<String, ClassNode> classes;
    private List<Relation> relations;
    
    public IntermediateRepresentation() {
        this.classes = new HashMap<>();
        this.relations = new ArrayList<>();
    }
    
    /**
     * Add a class to the IR
     */
    public void addClass(ClassNode classNode) {
        this.classes.put(classNode.getId(), classNode);
    }
    
    /**
     * Get a class by ID
     */
    public ClassNode getClass(String id) {
        return classes.get(id);
    }
    
    /**
     * Get all classes
     */
    public Map<String, ClassNode> getAllClasses() {
        return classes;
    }
    
    /**
     * Add a relation between two classes
     */
    public void addRelation(Relation relation) {
        if (!relations.contains(relation)) {
            this.relations.add(relation);
        }
    }
    
    /**
     * Get all relations
     */
    public List<Relation> getAllRelations() {
        return relations;
    }
    
    /**
     * Get relations for a specific class
     */
    public List<Relation> getRelationsForClass(String classId) {
        List<Relation> result = new ArrayList<>();
        for (Relation rel : relations) {
            if (rel.getSourceId().equals(classId) || rel.getTargetId().equals(classId)) {
                result.add(rel);
            }
        }
        return result;
    }
    
    /**
     * Get outgoing relations from a class (source relations)
     */
    public List<Relation> getOutgoingRelations(String classId) {
        List<Relation> result = new ArrayList<>();
        for (Relation rel : relations) {
            if (rel.getSourceId().equals(classId)) {
                result.add(rel);
            }
        }
        return result;
    }
    
    /**
     * Get incoming relations to a class (target relations)
     */
    public List<Relation> getIncomingRelations(String classId) {
        List<Relation> result = new ArrayList<>();
        for (Relation rel : relations) {
            if (rel.getTargetId().equals(classId)) {
                result.add(rel);
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("IR[classes=%d, relations=%d]", classes.size(), relations.size());
    }
}
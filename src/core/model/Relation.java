package core.model;

/**
 * Internal Representation of a relationship between two classes.
 * Story 2-02: Build IR for classes/relations
 * Story 2-03: Detect inheritance & interfaces
 * Story 2-04: Detect associations/aggregation/composition
 */
public class Relation {
    private String id;
    private String sourceId;      // ID of source class
    private String targetId;      // ID of target class
    private String type;          // "inheritance", "implements", "association", "aggregation", "composition"
    private boolean targetExternal; // true if target is external class
    
    public Relation(String sourceId, String targetId, String type) {
        this(sourceId, targetId, type, false);
    }
    
    public Relation(String sourceId, String targetId, String type, boolean targetExternal) {
        this.id = sourceId + "_" + type + "_" + targetId;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.targetExternal = targetExternal;
    }
    
    public String getId() {
        return id;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public String getTargetId() {
        return targetId;
    }
    
    public String getType() {
        return type;
    }
    
    public boolean isTargetExternal() {
        return targetExternal;
    }
    
    public void setTargetExternal(boolean external) {
        this.targetExternal = external;
    }
    
    @Override
    public String toString() {
        return String.format("Relation[%s --%s--> %s%s]", 
            sourceId, type, targetId, targetExternal ? " (external)" : "");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Relation that = (Relation) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
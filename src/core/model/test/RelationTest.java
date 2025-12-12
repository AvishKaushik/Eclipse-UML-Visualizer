package core.model.test;

import static org.junit.Assert.*;

import org.junit.Test;

import core.model.Relation;

/**
 * Unit tests for Relation model
 */
public class RelationTest {

    @Test
    public void testInheritanceRelation() {
        Relation relation = new Relation(
            "com.example.ChildClass",
            "com.example.ParentClass",
            "inheritance"
        );

        assertEquals("Source should match", "com.example.ChildClass", relation.getSourceId());
        assertEquals("Target should match", "com.example.ParentClass", relation.getTargetId());
        assertEquals("Type should be inheritance", "inheritance", relation.getType());
        assertFalse("Should not be external by default", relation.isTargetExternal());
    }

    @Test
    public void testRealizationRelation() {
        Relation relation = new Relation(
            "com.example.MyClass",
            "com.example.MyInterface",
            "implements"
        );

        assertEquals("Type should be implements", "implements", relation.getType());
    }

    @Test
    public void testAssociationRelation() {
        Relation relation = new Relation(
            "com.example.ClassA",
            "com.example.ClassB",
            "association"
        );

        assertEquals("Type should be association", "association", relation.getType());
    }

    @Test
    public void testAggregationRelation() {
        Relation relation = new Relation(
            "com.example.Container",
            "com.example.Item",
            "aggregation"
        );

        assertEquals("Type should be aggregation", "aggregation", relation.getType());
    }

    @Test
    public void testCompositionRelation() {
        Relation relation = new Relation(
            "com.example.Whole",
            "com.example.Part",
            "composition"
        );

        assertEquals("Type should be composition", "composition", relation.getType());
    }

    @Test
    public void testDependencyRelation() {
        Relation relation = new Relation(
            "com.example.Client",
            "com.example.Service",
            "dependency"
        );

        assertEquals("Type should be dependency", "dependency", relation.getType());
    }

    @Test
    public void testRelationWithExternalTarget() {
        Relation relation = new Relation(
            "com.example.MyClass",
            "java.util.List",
            "association",
            true  // External target
        );

        assertTrue("Should be external", relation.isTargetExternal());
    }

    @Test
    public void testSetTargetExternal() {
        Relation relation = new Relation("ClassA", "ClassB", "association");

        assertFalse("Should not be external initially", relation.isTargetExternal());

        relation.setTargetExternal(true);
        assertTrue("Should be external", relation.isTargetExternal());

        relation.setTargetExternal(false);
        assertFalse("Should not be external", relation.isTargetExternal());
    }

    @Test
    public void testRelationId() {
        Relation relation = new Relation("ClassA", "ClassB", "inheritance");

        String expectedId = "ClassA_inheritance_ClassB";
        assertEquals("ID should be generated correctly", expectedId, relation.getId());
    }

    @Test
    public void testRelationToString() {
        Relation relation = new Relation("ClassA", "ClassB", "inheritance");

        String result = relation.toString();

        assertNotNull("toString should not be null", result);
        assertTrue("toString should contain source", result.contains("ClassA"));
        assertTrue("toString should contain target", result.contains("ClassB"));
        assertTrue("toString should contain type", result.contains("inheritance"));
    }

    @Test
    public void testRelationToStringWithExternal() {
        Relation relation = new Relation("ClassA", "ExternalLib", "association", true);

        String result = relation.toString();

        assertTrue("toString should indicate external", result.contains("external"));
    }

    @Test
    public void testAllRelationTypes() {
        String[] types = {"inheritance", "implements", "association", "aggregation", "composition", "dependency"};

        for (String type : types) {
            Relation relation = new Relation("ClassA", "ClassB", type);
            assertEquals("Type should match", type, relation.getType());
        }
    }

    @Test
    public void testComplexRelation() {
        Relation relation = new Relation(
            "com.example.Order",
            "com.example.OrderItem",
            "composition",
            false
        );

        assertEquals("Source should match", "com.example.Order", relation.getSourceId());
        assertEquals("Target should match", "com.example.OrderItem", relation.getTargetId());
        assertEquals("Type should be composition", "composition", relation.getType());
        assertFalse("Should not be external", relation.isTargetExternal());
    }

    @Test
    public void testSelfRelation() {
        // A class can have a relation to itself
        Relation relation = new Relation(
            "com.example.Node",
            "com.example.Node",
            "association"
        );

        assertEquals("Source should equal target for self-relation",
            relation.getSourceId(), relation.getTargetId());
    }

    @Test
    public void testRelationEquality() {
        Relation relation1 = new Relation("ClassA", "ClassB", "inheritance");
        Relation relation2 = new Relation("ClassA", "ClassB", "inheritance");

        // Check if relations with same properties are considered equal
        assertEquals("IDs should be equal", relation1.getId(), relation2.getId());
        assertEquals("Relations with same ID should be equal", relation1, relation2);
    }

    @Test
    public void testRelationEqualityWithSelf() {
        Relation relation = new Relation("ClassA", "ClassB", "inheritance");
        assertEquals("Relation should equal itself", relation, relation);
    }

    @Test
    public void testRelationEqualityWithNull() {
        Relation relation = new Relation("ClassA", "ClassB", "inheritance");
        assertFalse("Relation should not equal null", relation.equals(null));
    }

    @Test
    public void testDifferentRelations() {
        Relation relation1 = new Relation("ClassA", "ClassB", "inheritance");
        Relation relation2 = new Relation("ClassA", "ClassC", "inheritance");

        assertNotEquals("IDs should be different", relation1.getId(), relation2.getId());
        assertNotEquals("Relations should not be equal", relation1, relation2);
    }

    @Test
    public void testDifferentTypes() {
        Relation relation1 = new Relation("ClassA", "ClassB", "inheritance");
        Relation relation2 = new Relation("ClassA", "ClassB", "association");

        assertNotEquals("IDs should be different for different types",
            relation1.getId(), relation2.getId());
        assertNotEquals("Relations should not be equal", relation1, relation2);
    }

    @Test
    public void testHashCode() {
        Relation relation1 = new Relation("ClassA", "ClassB", "inheritance");
        Relation relation2 = new Relation("ClassA", "ClassB", "inheritance");

        assertEquals("Hash codes should be equal for equal objects",
            relation1.hashCode(), relation2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        Relation relation = new Relation("ClassA", "ClassB", "inheritance");

        int hashCode1 = relation.hashCode();
        int hashCode2 = relation.hashCode();

        assertEquals("Hash code should be consistent", hashCode1, hashCode2);
    }

    @Test
    public void testIdGeneration() {
        Relation relation1 = new Relation("ClassA", "ClassB", "inheritance");
        Relation relation2 = new Relation("ClassB", "ClassA", "inheritance");

        assertNotEquals("IDs should be different for reversed source/target",
            relation1.getId(), relation2.getId());
    }

    @Test
    public void testExternalPackageRelation() {
        Relation relation = new Relation(
            "com.example.MyClass",
            "java.util.List",
            "association",
            true
        );

        assertEquals("Source should be internal class", "com.example.MyClass", relation.getSourceId());
        assertEquals("Target should be external class", "java.util.List", relation.getTargetId());
        assertTrue("Should mark as external", relation.isTargetExternal());
    }
}

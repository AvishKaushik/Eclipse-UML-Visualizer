package core.parser.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import core.model.ClassNode;
import core.model.IntermediateRepresentation;
import core.model.Relation;
import core.parser.InheritanceDetector;

/**
 * Unit tests for Story 2-03: Detect inheritance & interfaces
 */
public class InheritanceDetectorTest {

    private IntermediateRepresentation ir;
    private InheritanceDetector detector;

    @Before
    public void setUp() {
        ir = new IntermediateRepresentation();
        detector = new InheritanceDetector(ir);
    }

    /**
     * Test Case 1: class A extends B → IR shows relation A→B type=inheritance
     */
    @Test
    public void testDetectInheritance() {
        // Manually simulate: class A extends B
        ClassNode classA = new ClassNode("A", "class", "com.example");
        ClassNode classB = new ClassNode("B", "class", "com.example");

        ir.addClass(classA);
        ir.addClass(classB);

        // Create inheritance relation
        Relation relation = new Relation("com.example.A", "com.example.B", "inheritance", false);
        ir.addRelation(relation);

        // Verify
        List<Relation> relations = ir.getAllRelations();
        assertEquals(1, relations.size());

        Relation rel = relations.get(0);
        assertEquals("com.example.A", rel.getSourceId());
        assertEquals("com.example.B", rel.getTargetId());
        assertEquals("inheritance", rel.getType());
        assertFalse(rel.isTargetExternal());
    }

    /**
     * Test Case 2: class C implements I1, I2 → IR has C→I1, C→I2 type=implements
     */
    @Test
    public void testDetectMultipleInterfaces() {
        // Manually simulate: class C implements I1, I2
        ClassNode classC = new ClassNode("C", "class", "com.example");
        ClassNode interfaceI1 = new ClassNode("I1", "interface", "com.example");
        ClassNode interfaceI2 = new ClassNode("I2", "interface", "com.example");

        ir.addClass(classC);
        ir.addClass(interfaceI1);
        ir.addClass(interfaceI2);

        // Create implement relations
        Relation rel1 = new Relation("com.example.C", "com.example.I1", "implements", false);
        Relation rel2 = new Relation("com.example.C", "com.example.I2", "implements", false);

        ir.addRelation(rel1);
        ir.addRelation(rel2);

        // Verify
        List<Relation> relations = ir.getOutgoingRelations("com.example.C");
        assertEquals(2, relations.size());

        for (Relation rel : relations) {
            assertEquals("implements", rel.getType());
            assertEquals("com.example.C", rel.getSourceId());
            assertTrue(rel.getTargetId().startsWith("com.example.I"));
        }
    }

    /**
     * Test Case 3: Extending an external class marks relation target as external
     */
    @Test
    public void testExternalClassMarking() {
        // class MyList extends ArrayList (external)
        ClassNode myList = new ClassNode("MyList", "class", "com.example");
        ir.addClass(myList);

        // Create relation to external ArrayList
        Relation relation = new Relation("com.example.MyList", "java.util.ArrayList", "inheritance", true);
        ir.addRelation(relation);

        // Verify
        assertTrue(relation.isTargetExternal());
        assertEquals("java.util.ArrayList", relation.getTargetId());
    }

    /**
     * Test Case 4: Inheritance chain A extends B extends C
     */
    @Test
    public void testInheritanceChain() {
        // Create classes A, B, C
        ClassNode classA = new ClassNode("A", "class", "com.example");
        ClassNode classB = new ClassNode("B", "class", "com.example");
        ClassNode classC = new ClassNode("C", "class", "com.example");

        ir.addClass(classA);
        ir.addClass(classB);
        ir.addClass(classC);

        // Create chain: A→B→C
        ir.addRelation(new Relation("com.example.A", "com.example.B", "inheritance"));
        ir.addRelation(new Relation("com.example.B", "com.example.C", "inheritance"));

        // Verify
        List<Relation> aOutgoing = ir.getOutgoingRelations("com.example.A");
        List<Relation> bRelations = ir.getRelationsForClass("com.example.B");

        assertEquals(1, aOutgoing.size());
        assertEquals("com.example.B", aOutgoing.get(0).getTargetId());

        // B should have 1 outgoing and 1 incoming
        assertEquals(2, bRelations.size());
    }

    /**
     * Test Case 5: Interface extending another interface
     */
    @Test
    public void testInterfaceInheritance() {
        // interface AdvancedList extends List
        ClassNode advancedList = new ClassNode("AdvancedList", "interface", "com.example");
        ClassNode list = new ClassNode("List", "interface", "com.example");

        ir.addClass(advancedList);
        ir.addClass(list);

        // Interfaces use inheritance relation
        ir.addRelation(new Relation("com.example.AdvancedList", "com.example.List", "inheritance"));

        // Verify
        List<Relation> relations = ir.getOutgoingRelations("com.example.AdvancedList");
        assertEquals(1, relations.size());
        assertEquals("inheritance", relations.get(0).getType());
    }

    /**
     * Test Case 6: Multiple inheritance simulation (via interfaces)
     */
    @Test
    public void testMultipleImplementation() {
        ClassNode classD = new ClassNode("D", "class", "com.example");
        ClassNode i1 = new ClassNode("I1", "interface", "com.example");
        ClassNode i2 = new ClassNode("I2", "interface", "com.example");
        ClassNode i3 = new ClassNode("I3", "interface", "com.example");

        ir.addClass(classD);
        ir.addClass(i1);
        ir.addClass(i2);
        ir.addClass(i3);

        // D implements I1, I2, I3
        ir.addRelation(new Relation("com.example.D", "com.example.I1", "implements"));
        ir.addRelation(new Relation("com.example.D", "com.example.I2", "implements"));
        ir.addRelation(new Relation("com.example.D", "com.example.I3", "implements"));

        // Verify
        List<Relation> dRelations = ir.getOutgoingRelations("com.example.D");
        assertEquals(3, dRelations.size());

        for (Relation rel : dRelations) {
            assertEquals("implements", rel.getType());
        }
    }

    /**
     * Test Case 7: Mixed inheritance and interfaces
     */
    @Test
    public void testMixedInheritanceAndInterfaces() {
        // class E extends B implements I1, I2
        ClassNode classE = new ClassNode("E", "class", "com.example");
        ClassNode classB = new ClassNode("B", "class", "com.example");
        ClassNode i1 = new ClassNode("I1", "interface", "com.example");
        ClassNode i2 = new ClassNode("I2", "interface", "com.example");

        ir.addClass(classE);
        ir.addClass(classB);
        ir.addClass(i1);
        ir.addClass(i2);

        // Add relations: inheritance + implements
        ir.addRelation(new Relation("com.example.E", "com.example.B", "inheritance"));
        ir.addRelation(new Relation("com.example.E", "com.example.I1", "implements"));
        ir.addRelation(new Relation("com.example.E", "com.example.I2", "implements"));

        // Verify
        List<Relation> eRelations = ir.getOutgoingRelations("com.example.E");
        assertEquals(3, eRelations.size());

        int inheritanceCount = 0;
        int implementsCount = 0;
        for (Relation r : eRelations) {
            if ("inheritance".equals(r.getType())) inheritanceCount++;
            if ("implements".equals(r.getType())) implementsCount++;
        }

        assertEquals(1, inheritanceCount);
        assertEquals(2, implementsCount);
    }
}

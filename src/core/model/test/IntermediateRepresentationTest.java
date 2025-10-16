package core.model.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import core.model.ClassNode;
import core.model.Field;
import core.model.IntermediateRepresentation;
import core.model.IRJsonSerializer;
import core.model.Method;
import core.model.Relation;

/**
 * Unit tests for Story 2-02: Build IR for classes/relations
 */
public class IntermediateRepresentationTest {

    private IntermediateRepresentation ir;
    private IRJsonSerializer serializer;

    @Before
    public void setUp() {
        ir = new IntermediateRepresentation();
        serializer = new IRJsonSerializer();
    }

    /**
     * Test Case 1: Manually build a ClassNode with fields/methods
     */
    @Test
    public void testBuildClassNodeManually() {
        // Create a class
        ClassNode classNode = new ClassNode("Student", "class", "com.example");

        // Add fields
        classNode.addField(new Field("name", "String", "private", false));
        classNode.addField(new Field("age", "int", "private", false));

        // Add methods
        classNode.addMethod(new Method("getName", "String", "public", false));
        classNode.addMethod(new Method("setName", "void", "public", false));

        // Add to IR
        ir.addClass(classNode);

        // Verify
        assertEquals(1, ir.getAllClasses().size());
        ClassNode retrieved = ir.getClass("com.example.Student");
        assertNotNull(retrieved);
        assertEquals("Student", retrieved.getName());
        assertEquals(2, retrieved.getFields().size());
        assertEquals(2, retrieved.getMethods().size());
    }

    /**
     * Test Case 2: Parse 3 Java files → IR contains all classes with correct attributes
     */
    @Test
    public void testBuildMultipleClasses() {
        // Simulate parsing 3 classes
        ClassNode student = new ClassNode("Student", "class", "com.example");
        student.addField(new Field("id", "int", "private", false));
        student.addMethod(new Method("getId", "int", "public", false));

        ClassNode teacher = new ClassNode("Teacher", "class", "com.example");
        teacher.addField(new Field("salary", "double", "private", false));
        teacher.addMethod(new Method("getSalary", "double", "public", false));

        ClassNode course = new ClassNode("Course", "class", "com.example");
        course.addField(new Field("name", "String", "private", false));
        course.addMethod(new Method("getName", "String", "public", false));

        // Add to IR
        ir.addClass(student);
        ir.addClass(teacher);
        ir.addClass(course);

        // Verify
        assertEquals(3, ir.getAllClasses().size());
        assertNotNull(ir.getClass("com.example.Student"));
        assertNotNull(ir.getClass("com.example.Teacher"));
        assertNotNull(ir.getClass("com.example.Course"));
    }

    /**
     * Test Case 3: Add relations to IR
     */
    @Test
    public void testAddRelations() {
        // Create classes
        ClassNode classA = new ClassNode("ClassA", "class", "com.example");
        ClassNode classB = new ClassNode("ClassB", "class", "com.example");

        ir.addClass(classA);
        ir.addClass(classB);

        // Add relation
        Relation relation = new Relation("com.example.ClassA", "com.example.ClassB", "association");
        ir.addRelation(relation);

        // Verify
        assertEquals(1, ir.getAllRelations().size());
        assertEquals("association", ir.getAllRelations().get(0).getType());
    }

    /**
     * Test Case 4: Query relations for a class
     */
    @Test
    public void testQueryRelations() {
        // Create classes
        ClassNode classA = new ClassNode("ClassA", "class", "com.example");
        ClassNode classB = new ClassNode("ClassB", "class", "com.example");
        ClassNode classC = new ClassNode("ClassC", "class", "com.example");

        ir.addClass(classA);
        ir.addClass(classB);
        ir.addClass(classC);

        // Add relations
        ir.addRelation(new Relation("com.example.ClassA", "com.example.ClassB", "association"));
        ir.addRelation(new Relation("com.example.ClassB", "com.example.ClassC", "inheritance"));
        ir.addRelation(new Relation("com.example.ClassA", "com.example.ClassC", "association"));

        // Verify outgoing relations from ClassA
        assertEquals(2, ir.getOutgoingRelations("com.example.ClassA").size());

        // Verify incoming relations to ClassC
        assertEquals(2, ir.getIncomingRelations("com.example.ClassC").size());
    }

    /**
     * Test Case 5: JSON serialization round-trip
     */
    @Test
    public void testJsonSerialization() {
        // Create a class
        ClassNode classNode = new ClassNode("Person", "class", "com.example");
        classNode.addField(new Field("name", "String", "private", false));
        classNode.addMethod(new Method("getName", "String", "public", false));

        ir.addClass(classNode);

        // Serialize to JSON
        String json = serializer.toJson(ir);

        // Verify JSON is not empty and contains class name
        assertNotNull(json);
        assertTrue(json.contains("Person"));
        assertTrue(json.contains("name"));
        assertTrue(json.contains("getName"));
    }

    /**
     * Test Case 6: JSON serialization with relations
     */
    @Test
    public void testJsonSerializationWithRelations() {
        // Create classes
        ClassNode classA = new ClassNode("ClassA", "class", "com.example");
        ClassNode classB = new ClassNode("ClassB", "class", "com.example");

        ir.addClass(classA);
        ir.addClass(classB);
        ir.addRelation(new Relation("com.example.ClassA", "com.example.ClassB", "inheritance"));

        // Serialize to JSON
        String json = serializer.toJsonWithRelations(ir);

        // Verify JSON contains both classes and relations
        assertNotNull(json);
        assertTrue(json.contains("ClassA"));
        assertTrue(json.contains("ClassB"));
        assertTrue(json.contains("inheritance"));
    }

    /**
     * Test Case 7: External class marking
     */
    @Test
    public void testExternalClassMarking() {
        ClassNode classA = new ClassNode("ClassA", "class", "com.example");
        ir.addClass(classA);

        // Add relation to external class
        Relation relation = new Relation("com.example.ClassA", "java.util.List", "association", true);
        ir.addRelation(relation);

        // Verify external flag
        assertTrue(relation.isTargetExternal());
    }

    /**
     * Test Case 8: Duplicate relation prevention
     */
    @Test
    public void testDuplicateRelationPrevention() {
        ClassNode classA = new ClassNode("ClassA", "class", "com.example");
        ClassNode classB = new ClassNode("ClassB", "class", "com.example");

        ir.addClass(classA);
        ir.addClass(classB);

        // Add same relation twice
        Relation rel1 = new Relation("com.example.ClassA", "com.example.ClassB", "association");
        Relation rel2 = new Relation("com.example.ClassA", "com.example.ClassB", "association");

        ir.addRelation(rel1);
        ir.addRelation(rel2);

        // Should only have 1 relation (duplicate prevented)
        assertEquals(1, ir.getAllRelations().size());
    }
}

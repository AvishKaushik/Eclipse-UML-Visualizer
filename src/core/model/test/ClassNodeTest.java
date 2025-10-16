package core.model.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import core.model.ClassNode;
import core.model.Field;
import core.model.Method;

/**
 * Unit tests for ClassNode model
 */
public class ClassNodeTest {

    private ClassNode classNode;

    @Before
    public void setUp() {
        classNode = new ClassNode("TestClass", "class", "com.example");
    }

    @Test
    public void testClassNodeCreation() {
        assertNotNull("ClassNode should be created", classNode);
        assertEquals("Name should match", "TestClass", classNode.getName());
        assertEquals("Package should match", "com.example", classNode.getPackageName());
        assertEquals("Type should match", "class", classNode.getType());
        assertNotNull("Should have ID", classNode.getId());
        assertEquals("ID should be packageName.className",
            "com.example.TestClass", classNode.getId());
    }

    @Test
    public void testClassNodeWithSingleArgConstructor() {
        ClassNode node = new ClassNode("SimpleClass");

        assertEquals("Name should match", "SimpleClass", node.getName());
        assertEquals("Type should default to class", "class", node.getType());
        assertEquals("Package should be empty", "", node.getPackageName());
        assertEquals("ID should be just class name", "SimpleClass", node.getId());
    }

    @Test
    public void testClassNodeWithoutPackage() {
        ClassNode node = new ClassNode("SimpleClass", "class", "");
        assertEquals("Should handle empty package", "SimpleClass", node.getId());
    }

    @Test
    public void testInterfaceType() {
        ClassNode interfaceNode = new ClassNode("MyInterface", "interface", "com.example");
        assertEquals("Type should be interface", "interface", interfaceNode.getType());
    }

    @Test
    public void testEnumType() {
        ClassNode enumNode = new ClassNode("MyEnum", "enum", "com.example");
        assertEquals("Type should be enum", "enum", enumNode.getType());
    }

    @Test
    public void testAddField() {
        Field field = new Field("fieldName", "String", "private", false);
        classNode.addField(field);

        assertNotNull("Fields list should not be null", classNode.getFields());
        assertEquals("Should have 1 field", 1, classNode.getFields().size());
        assertEquals("Field should match", field, classNode.getFields().get(0));
    }

    @Test
    public void testAddMultipleFields() {
        Field field1 = new Field("name", "String", "private", false);
        Field field2 = new Field("age", "int", "private", false);
        Field field3 = new Field("address", "String", "public", false);

        classNode.addField(field1);
        classNode.addField(field2);
        classNode.addField(field3);

        assertEquals("Should have 3 fields", 3, classNode.getFields().size());
    }

    @Test
    public void testAddMethod() {
        Method method = new Method("doSomething", "void", "public", false);
        classNode.addMethod(method);

        assertNotNull("Methods list should not be null", classNode.getMethods());
        assertEquals("Should have 1 method", 1, classNode.getMethods().size());
        assertEquals("Method should match", method, classNode.getMethods().get(0));
    }

    @Test
    public void testAddMultipleMethods() {
        Method method1 = new Method("getName", "String", "public", false);
        Method method2 = new Method("setName", "void", "public", false);
        Method method3 = new Method("toString", "String", "public", false);

        classNode.addMethod(method1);
        classNode.addMethod(method2);
        classNode.addMethod(method3);

        assertEquals("Should have 3 methods", 3, classNode.getMethods().size());
    }

    @Test
    public void testToString() {
        String result = classNode.toString();

        assertNotNull("toString should not be null", result);
        assertTrue("toString should contain ID", result.contains("com.example.TestClass"));
        assertTrue("toString should contain type", result.contains("class"));
    }

    @Test
    public void testToStringWithFieldsAndMethods() {
        classNode.addField(new Field("field1", "String", "private", false));
        classNode.addMethod(new Method("method1", "void", "public", false));

        String result = classNode.toString();

        assertTrue("toString should show field count", result.contains("fields=1"));
        assertTrue("toString should show method count", result.contains("methods=1"));
    }

    @Test
    public void testFieldsListInitialization() {
        ClassNode newNode = new ClassNode("NewClass", "class", "com.test");

        assertNotNull("Fields list should be initialized", newNode.getFields());
        assertEquals("Fields list should be empty", 0, newNode.getFields().size());
    }

    @Test
    public void testMethodsListInitialization() {
        ClassNode newNode = new ClassNode("NewClass", "class", "com.test");

        assertNotNull("Methods list should be initialized", newNode.getMethods());
        assertEquals("Methods list should be empty", 0, newNode.getMethods().size());
    }

    @Test
    public void testComplexClassNode() {
        // Create a complex class with multiple elements
        ClassNode complexClass = new ClassNode("ComplexClass", "class", "com.example.complex");

        complexClass.addField(new Field("field1", "String", "private", false));
        complexClass.addField(new Field("field2", "int", "protected", false));

        complexClass.addMethod(new Method("method1", "void", "public", false));
        complexClass.addMethod(new Method("method2", "String", "private", false));

        assertEquals("Should have 2 fields", 2, complexClass.getFields().size());
        assertEquals("Should have 2 methods", 2, complexClass.getMethods().size());
        assertEquals("Should have correct package", "com.example.complex", complexClass.getPackageName());
        assertEquals("Should have correct ID", "com.example.complex.ComplexClass", complexClass.getId());
    }

    @Test
    public void testEquality() {
        ClassNode node1 = new ClassNode("TestClass", "class", "com.example");
        ClassNode node2 = new ClassNode("TestClass", "class", "com.example");

        // IDs should be the same
        assertEquals("IDs should be equal", node1.getId(), node2.getId());
        assertEquals("Nodes with same ID should be equal", node1, node2);
    }

    @Test
    public void testEqualityWithSameObject() {
        assertEquals("Node should equal itself", classNode, classNode);
    }

    @Test
    public void testEqualityWithNull() {
        assertFalse("Node should not equal null", classNode.equals(null));
    }

    @Test
    public void testDifferentPackages() {
        ClassNode node1 = new ClassNode("TestClass", "class", "com.example");
        ClassNode node2 = new ClassNode("TestClass", "class", "com.other");

        assertNotEquals("IDs should be different", node1.getId(), node2.getId());
        assertNotEquals("Nodes with different IDs should not be equal", node1, node2);
    }

    @Test
    public void testHashCode() {
        ClassNode node1 = new ClassNode("TestClass", "class", "com.example");
        ClassNode node2 = new ClassNode("TestClass", "class", "com.example");

        assertEquals("Hash codes should be equal for equal objects",
            node1.hashCode(), node2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        int hashCode1 = classNode.hashCode();
        int hashCode2 = classNode.hashCode();

        assertEquals("Hash code should be consistent", hashCode1, hashCode2);
    }

    @Test
    public void testDifferentTypes() {
        ClassNode classNode = new ClassNode("MyClass", "class", "com.example");
        ClassNode interfaceNode = new ClassNode("MyClass", "interface", "com.example");

        // IDs are the same (based on package and name, not type)
        assertEquals("IDs should be equal even with different types",
            classNode.getId(), interfaceNode.getId());
        assertEquals("Should be equal based on ID only", classNode, interfaceNode);
    }

    @Test
    public void testIdGeneration() {
        ClassNode node1 = new ClassNode("Class1", "class", "com.example");
        ClassNode node2 = new ClassNode("Class2", "class", "com.example");
        ClassNode node3 = new ClassNode("Class1", "class", "");

        assertEquals("Should generate correct ID with package",
            "com.example.Class1", node1.getId());
        assertEquals("Should generate correct ID with package",
            "com.example.Class2", node2.getId());
        assertEquals("Should generate correct ID without package",
            "Class1", node3.getId());
    }

    @Test
    public void testGetters() {
        assertEquals("getName should return name", "TestClass", classNode.getName());
        assertEquals("getType should return type", "class", classNode.getType());
        assertEquals("getPackageName should return package", "com.example", classNode.getPackageName());
        assertEquals("getId should return ID", "com.example.TestClass", classNode.getId());
    }

    @Test
    public void testEmptyFieldsAndMethods() {
        ClassNode emptyNode = new ClassNode("EmptyClass");

        assertTrue("Fields list should be empty", emptyNode.getFields().isEmpty());
        assertTrue("Methods list should be empty", emptyNode.getMethods().isEmpty());
    }
}

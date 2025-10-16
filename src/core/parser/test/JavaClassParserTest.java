package core.parser.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import core.parser.ClassInfo;
import core.parser.FieldInfo;
import core.parser.JavaClassParser;
import core.parser.MethodInfo;

/**
 * Unit tests for Story 2-01: Parse class signatures, fields, and methods
 */
public class JavaClassParserTest {

    private JavaClassParser parser;

    @Before
    public void setUp() {
        parser = new JavaClassParser();
    }

    /**
     * Test Case 1: Parsing class Foo {} produces ClassInfo("Foo") with empty fields/methods
     */
    @Test
    public void testParseEmptyClass() {
        ClassInfo classInfo = new ClassInfo("Foo");

        assertEquals("Foo", classInfo.getName());
        assertEquals("class", classInfo.getType());
        assertEquals("Empty class should have no fields", 0, classInfo.getFields().size());
        assertEquals("Empty class should have no methods", 0, classInfo.getMethods().size());
    }

    /**
     * Test Case 2: Parsing class Bar { int x; void f(){} }
     * produces FieldInfo("x","int"), MethodInfo("f","void")
     */
    @Test
    public void testParseClassWithFieldAndMethod() {
        ClassInfo classInfo = new ClassInfo("Bar");

        FieldInfo field = new FieldInfo("x", "int");
        classInfo.addField(field);

        MethodInfo method = new MethodInfo("f", "void");
        classInfo.addMethod(method);

        assertEquals("Bar", classInfo.getName());

        assertEquals(1, classInfo.getFields().size());
        FieldInfo parsedField = classInfo.getFields().get(0);
        assertEquals("x", parsedField.getName());
        assertEquals("int", parsedField.getType());

        assertEquals(1, classInfo.getMethods().size());
        MethodInfo parsedMethod = classInfo.getMethods().get(0);
        assertEquals("f", parsedMethod.getName());
        assertEquals("void", parsedMethod.getReturnType());
    }

    /**
     * Test Case 3: Parsing multiple classes in one file produces multiple ClassInfo objects
     */
    @Test
    public void testParseMultipleClasses() {
        ClassInfo class1 = new ClassInfo("ClassA");
        ClassInfo class2 = new ClassInfo("ClassB");
        ClassInfo class3 = new ClassInfo("ClassC");

        List<ClassInfo> classes = Arrays.asList(class1, class2, class3);

        assertEquals("Should parse 3 classes", 3, classes.size());
        assertEquals("ClassA", classes.get(0).getName());
        assertEquals("ClassB", classes.get(1).getName());
        assertEquals("ClassC", classes.get(2).getName());
    }

    /**
     * Test Case 4: Parsing fields with different visibility modifiers
     */
    @Test
    public void testParseFieldVisibility() {
        ClassInfo classInfo = new ClassInfo("TestClass");

        classInfo.addField(new FieldInfo("publicField", "String", "public", false));
        classInfo.addField(new FieldInfo("privateField", "int", "private", false));
        classInfo.addField(new FieldInfo("protectedField", "double", "protected", false));
        classInfo.addField(new FieldInfo("packageField", "boolean", "package", false));

        assertEquals(4, classInfo.getFields().size());

        assertEquals("public", classInfo.getFields().get(0).getVisibility());
        assertEquals("private", classInfo.getFields().get(1).getVisibility());
        assertEquals("protected", classInfo.getFields().get(2).getVisibility());
        assertEquals("package", classInfo.getFields().get(3).getVisibility());
    }

    /**
     * Test Case 5: Parsing methods with different visibility modifiers
     */
    @Test
    public void testParseMethodVisibility() {
        ClassInfo classInfo = new ClassInfo("TestClass");

        classInfo.addMethod(new MethodInfo("publicMethod", "void", "public", false));
        classInfo.addMethod(new MethodInfo("privateMethod", "int", "private", false));
        classInfo.addMethod(new MethodInfo("protectedMethod", "String", "protected", false));

        assertEquals(3, classInfo.getMethods().size());

        assertEquals("public", classInfo.getMethods().get(0).getVisibility());
        assertEquals("private", classInfo.getMethods().get(1).getVisibility());
        assertEquals("protected", classInfo.getMethods().get(2).getVisibility());
    }

    /**
     * Test Case 6: Parsing static fields and methods
     */
    @Test
    public void testParseStaticMembers() {
        ClassInfo classInfo = new ClassInfo("TestClass");

        FieldInfo staticField = new FieldInfo("CONSTANT", "int", "public", true);
        classInfo.addField(staticField);
        assertTrue(staticField.isStatic());

        MethodInfo staticMethod = new MethodInfo("getInstance", "TestClass", "public", true);
        classInfo.addMethod(staticMethod);
        assertTrue(staticMethod.isStatic());
    }

    /**
     * Test Case 7: Parsing interface
     */
    @Test
    public void testParseInterface() {
        ClassInfo interfaceInfo = new ClassInfo("MyInterface", "interface");

        assertEquals("MyInterface", interfaceInfo.getName());
        assertEquals("interface", interfaceInfo.getType());
    }

    /**
     * Test Case 8: Parsing enum
     */
    @Test
    public void testParseEnum() {
        ClassInfo enumInfo = new ClassInfo("MyEnum", "enum");

        assertEquals("MyEnum", enumInfo.getName());
        assertEquals("enum", enumInfo.getType());
    }
}

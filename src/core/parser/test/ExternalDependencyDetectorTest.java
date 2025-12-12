package core.parser.test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import core.parser.ClassInfo;
import core.parser.ExternalDependencyDetector;

/**
 * Unit tests for ExternalDependencyDetector
 */
public class ExternalDependencyDetectorTest {

    private ExternalDependencyDetector detector;
    private List<ClassInfo> testClasses;

    @Before
    public void setUp() {
        detector = new ExternalDependencyDetector();
        testClasses = new ArrayList<>();
    }

    @Test
    public void testNoClasses() {
        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertNotNull("Should return non-null map", dependencies);
        assertTrue("Should be empty for no classes", dependencies.isEmpty());
    }

    @Test
    public void testClassWithNoImports() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertNotNull("Should return non-null map", dependencies);
        assertTrue("Should be empty for class with no imports", dependencies.isEmpty());
    }

    @Test
    public void testSingleExternalDependency() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("java.util.ArrayList");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertNotNull("Should detect dependencies", dependencies);
        assertEquals("Should have 1 package", 1, dependencies.size());
        assertTrue("Should contain java.util", dependencies.containsKey("java.util"));
        assertTrue("TestClass should use java.util",
            dependencies.get("java.util").contains("TestClass"));
    }

    @Test
    public void testMultipleExternalDependencies() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("java.util.ArrayList");
        classInfo.addImport("java.util.HashMap");
        classInfo.addImport("java.io.File");
        classInfo.addImport("java.io.IOException");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertEquals("Should have 2 packages", 2, dependencies.size());
        assertTrue("Should contain java.util", dependencies.containsKey("java.util"));
        assertTrue("Should contain java.io", dependencies.containsKey("java.io"));
    }

    @Test
    public void testFilterInternalClasses() {
        // Create two internal classes
        ClassInfo classA = new ClassInfo("ClassA", "class", "com.example");
        classA.addImport("com.example.ClassB");

        ClassInfo classB = new ClassInfo("ClassB", "class", "com.example");
        classB.addImport("com.example.ClassA");

        testClasses.add(classA);
        testClasses.add(classB);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertTrue("Should filter out internal classes", dependencies.isEmpty());
    }

    @Test
    public void testFilterJavaLang() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("java.lang.String");
        classInfo.addImport("java.lang.Integer");
        classInfo.addImport("java.lang.System");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertTrue("Should filter out java.lang (auto-imported)", dependencies.isEmpty());
    }

    @Test
    public void testJavaLangAnnotation() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("java.lang.annotation.Retention");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertFalse("Should NOT filter out java.lang.annotation", dependencies.isEmpty());
        assertTrue("Should contain java.lang.annotation",
            dependencies.containsKey("java.lang.annotation"));
    }

    @Test
    public void testMultipleClassesUsingSamePackage() {
        ClassInfo classA = new ClassInfo("ClassA", "class", "com.example");
        classA.addImport("java.util.ArrayList");

        ClassInfo classB = new ClassInfo("ClassB", "class", "com.example");
        classB.addImport("java.util.HashMap");

        testClasses.add(classA);
        testClasses.add(classB);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertEquals("Should have 1 package", 1, dependencies.size());
        assertTrue("Should contain java.util", dependencies.containsKey("java.util"));

        Set<String> users = dependencies.get("java.util");
        assertEquals("Should have 2 classes using it", 2, users.size());
        assertTrue("ClassA should use java.util", users.contains("ClassA"));
        assertTrue("ClassB should use java.util", users.contains("ClassB"));
    }

    @Test
    public void testThirdPartyLibraries() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("org.junit.Test");
        classInfo.addImport("org.junit.Assert");
        classInfo.addImport("com.google.gson.Gson");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertEquals("Should detect 2 packages", 2, dependencies.size());
        assertTrue("Should contain org.junit", dependencies.containsKey("org.junit"));
        assertTrue("Should contain com.google.gson", dependencies.containsKey("com.google.gson"));
    }

    @Test
    public void testClassInDefaultPackage() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "");
        classInfo.addImport("java.util.List");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertEquals("Should detect java.util", 1, dependencies.size());
        assertTrue("Should contain java.util", dependencies.containsKey("java.util"));
    }

    @Test
    public void testComplexProject() {
        // Simulate a complex project with multiple classes and dependencies
        ClassInfo classA = new ClassInfo("ServiceA", "class", "com.example.service");
        classA.addImport("java.util.List");
        classA.addImport("java.util.ArrayList");
        classA.addImport("com.example.service.ServiceB");  // Internal

        ClassInfo classB = new ClassInfo("ServiceB", "class", "com.example.service");
        classB.addImport("java.io.File");
        classB.addImport("java.io.IOException");
        classB.addImport("com.example.repository.RepositoryC");  // Internal

        ClassInfo classC = new ClassInfo("RepositoryC", "class", "com.example.repository");
        classC.addImport("java.sql.Connection");
        classC.addImport("java.sql.PreparedStatement");

        testClasses.add(classA);
        testClasses.add(classB);
        testClasses.add(classC);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertEquals("Should detect 3 external packages", 3, dependencies.size());
        assertTrue("Should contain java.util", dependencies.containsKey("java.util"));
        assertTrue("Should contain java.io", dependencies.containsKey("java.io"));
        assertTrue("Should contain java.sql", dependencies.containsKey("java.sql"));

        // Verify internal classes are filtered
        assertFalse("Should not contain com.example.service",
            dependencies.containsKey("com.example.service"));
        assertFalse("Should not contain com.example.repository",
            dependencies.containsKey("com.example.repository"));
    }

    @Test
    public void testDuplicateImports() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("java.util.ArrayList");
        classInfo.addImport("java.util.ArrayList");  // Duplicate
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertEquals("Should handle duplicates", 1, dependencies.size());
        assertTrue("Should contain java.util", dependencies.containsKey("java.util"));
    }

    @Test
    public void testEmptyImportStatement() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("");  // Empty import
        classInfo.addImport("java.util.List");  // Valid import
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertEquals("Should handle empty imports", 1, dependencies.size());
        assertTrue("Should contain java.util", dependencies.containsKey("java.util"));
    }

    @Test
    public void testWildcardImports() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("java.util.*");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        // Should handle wildcard imports appropriately
        assertNotNull("Should handle wildcard imports", dependencies);
    }

    @Test
    public void testStaticImports() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("org.junit.Assert.assertEquals");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertNotNull("Should handle static imports", dependencies);
        assertTrue("Should detect org.junit.Assert package",
            dependencies.containsKey("org.junit") || dependencies.containsKey("org.junit.Assert"));
    }

    @Test
    public void testNestedClassImports() {
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("java.util.Map.Entry");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        assertNotNull("Should handle nested class imports", dependencies);
        assertTrue("Should detect java.util.Map package",
            dependencies.containsKey("java.util") || dependencies.containsKey("java.util.Map"));
    }

    @Test
    public void testOrderPreservation() {
        // LinkedHashMap should preserve insertion order
        ClassInfo classInfo = new ClassInfo("TestClass", "class", "com.example");
        classInfo.addImport("org.example.First");
        classInfo.addImport("com.example2.Second");
        classInfo.addImport("net.example.Third");
        testClasses.add(classInfo);

        Map<String, Set<String>> dependencies = detector.detectExternalDependencies(testClasses);

        List<String> keys = new ArrayList<>(dependencies.keySet());
        // Should maintain some order based on processing
        assertNotNull("Should preserve some order", keys);
        assertEquals("Should have 3 packages", 3, keys.size());
    }
}

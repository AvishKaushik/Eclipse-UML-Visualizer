package core.parser.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import core.model.ClassNode;
import core.model.IntermediateRepresentation;
import core.model.Relation;
import core.parser.AssociationDetector;

/**
 * Unit tests for Story 2-04: Detect associations/aggregation/composition
 */
public class AssociationDetectorTest {
    
    private IntermediateRepresentation ir;
    private List<ClassNode> projectClasses;
    private AssociationDetector detector;
    
    @Before
    public void setUp() {
        ir = new IntermediateRepresentation();
        projectClasses = new ArrayList<>();
        detector = new AssociationDetector(ir, projectClasses);
    }
    
    /**
     * Test Case 1: class A { B b; } → IR shows A→B association
     */
    @Test
    public void testSimpleAssociation() {
        ClassNode classA = new ClassNode("A", "class", "com.example");
        ClassNode classB = new ClassNode("B", "class", "com.example");
        
        ir.addClass(classA);
        ir.addClass(classB);
        projectClasses.add(classA);
        projectClasses.add(classB);
        
        Relation relation = new Relation("com.example.A", "com.example.B", "association", false);
        ir.addRelation(relation);
        
        List<Relation> relations = ir.getOutgoingRelations("com.example.A");
        assertEquals(1, relations.size());
        assertEquals("association", relations.get(0).getType());
    }
    
    /**
     * Test Case 2: class C { private D d = new D(); } → IR shows C→D composition
     */
    @Test
    public void testComposition() {
        ClassNode classC = new ClassNode("C", "class", "com.example");
        ClassNode classD = new ClassNode("D", "class", "com.example");
        
        ir.addClass(classC);
        ir.addClass(classD);
        projectClasses.add(classC);
        projectClasses.add(classD);
        
        Relation relation = new Relation("com.example.C", "com.example.D", "composition", false);
        ir.addRelation(relation);
        
        List<Relation> relations = ir.getOutgoingRelations("com.example.C");
        assertEquals(1, relations.size());
        assertEquals("composition", relations.get(0).getType());
    }
    
    /**
     * Test Case 3: class E { List<F> list; } → IR shows E→F aggregation
     */
    @Test
    public void testAggregation() {
        ClassNode classE = new ClassNode("E", "class", "com.example");
        ClassNode classF = new ClassNode("F", "class", "com.example");
        
        ir.addClass(classE);
        ir.addClass(classF);
        projectClasses.add(classE);
        projectClasses.add(classF);
        
        Relation relation = new Relation("com.example.E", "com.example.F", "aggregation", false);
        ir.addRelation(relation);
        
        List<Relation> relations = ir.getOutgoingRelations("com.example.E");
        assertEquals(1, relations.size());
        assertEquals("aggregation", relations.get(0).getType());
    }
    
    /**
     * Test Case 4: Multiple associations from one class
     */
    @Test
    public void testMultipleAssociations() {
        ClassNode container = new ClassNode("Container", "class", "com.example");
        ClassNode classA = new ClassNode("A", "class", "com.example");
        ClassNode classB = new ClassNode("B", "class", "com.example");
        ClassNode classC = new ClassNode("C", "class", "com.example");
        
        ir.addClass(container);
        ir.addClass(classA);
        ir.addClass(classB);
        ir.addClass(classC);
        projectClasses.add(container);
        projectClasses.add(classA);
        projectClasses.add(classB);
        projectClasses.add(classC);
        
        ir.addRelation(new Relation("com.example.Container", "com.example.A", "association"));
        ir.addRelation(new Relation("com.example.Container", "com.example.B", "association"));
        ir.addRelation(new Relation("com.example.Container", "com.example.C", "association"));
        
        List<Relation> relations = ir.getOutgoingRelations("com.example.Container");
        assertEquals(3, relations.size());
    }
    
    /**
     * Test Case 5: Mixed relationships (association, composition, aggregation)
     */
    @Test
    public void testMixedRelationships() {
        ClassNode store = new ClassNode("Store", "class", "com.example");
        ClassNode manager = new ClassNode("Manager", "class", "com.example");
        ClassNode cash = new ClassNode("Cash", "class", "com.example");
        ClassNode item = new ClassNode("Item", "class", "com.example");
        
        ir.addClass(store);
        ir.addClass(manager);
        ir.addClass(cash);
        ir.addClass(item);
        projectClasses.add(store);
        projectClasses.add(manager);
        projectClasses.add(cash);
        projectClasses.add(item);
        
        ir.addRelation(new Relation("com.example.Store", "com.example.Manager", "association"));
        ir.addRelation(new Relation("com.example.Store", "com.example.Cash", "composition"));
        ir.addRelation(new Relation("com.example.Store", "com.example.Item", "aggregation"));
        
        List<Relation> relations = ir.getOutgoingRelations("com.example.Store");
        assertEquals(3, relations.size());
        
        int assocCount = 0, compCount = 0, aggCount = 0;
        for (Relation r : relations) {
            switch (r.getType()) {
                case "association": assocCount++; break;
                case "composition": compCount++; break;
                case "aggregation": aggCount++; break;
            }
        }
        
        assertEquals(1, assocCount);
        assertEquals(1, compCount);
        assertEquals(1, aggCount);
    }
    
    /**
     * Test Case 6: Collection aggregation (Set, Map, etc.)
     */
    @Test
    public void testCollectionAggregation() {
        ClassNode graph = new ClassNode("Graph", "class", "com.example");
        ClassNode node = new ClassNode("Node", "class", "com.example");
        
        ir.addClass(graph);
        ir.addClass(node);
        projectClasses.add(graph);
        projectClasses.add(node);
        
        ir.addRelation(new Relation("com.example.Graph", "com.example.Node", "aggregation"));
        
        assertEquals(1, ir.getOutgoingRelations("com.example.Graph").size());
    }
    
    /**
     * Test Case 7: Bidirectional associations
     */
    @Test
    public void testBidirectionalAssociation() {
        ClassNode author = new ClassNode("Author", "class", "com.example");
        ClassNode book = new ClassNode("Book", "class", "com.example");
        
        ir.addClass(author);
        ir.addClass(book);
        projectClasses.add(author);
        projectClasses.add(book);
        
        ir.addRelation(new Relation("com.example.Author", "com.example.Book", "aggregation"));
        ir.addRelation(new Relation("com.example.Book", "com.example.Author", "association"));
        
        assertEquals(1, ir.getOutgoingRelations("com.example.Author").size());
        assertEquals(1, ir.getOutgoingRelations("com.example.Book").size());
        assertEquals(1, ir.getIncomingRelations("com.example.Book").size());
        assertEquals(1, ir.getIncomingRelations("com.example.Author").size());
    }
    
    /**
     * Test Case 8: Self-referencing (recursive) associations
     */
    @Test
    public void testSelfReference() {
        ClassNode treeNode = new ClassNode("TreeNode", "class", "com.example");
        
        ir.addClass(treeNode);
        projectClasses.add(treeNode);
        
        ir.addRelation(new Relation("com.example.TreeNode", "com.example.TreeNode", "association"));
        ir.addRelation(new Relation("com.example.TreeNode", "com.example.TreeNode", "aggregation"));
        
        List<Relation> relations = ir.getOutgoingRelations("com.example.TreeNode");
        assertEquals(2, relations.size());
    }
}

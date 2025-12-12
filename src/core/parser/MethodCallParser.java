package core.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import core.model.SequenceDiagram;
import core.model.SequenceMessage;

/**
 * Parses method bodies to extract method calls for sequence diagrams
 */
// Note to everyone - please read the documentation for ASTParser and follow the standards before making changes in this file.
public class MethodCallParser {

    private Set<String> userClassNames = new HashSet<>();

    public void setUserClasses(List<IType> userClasses) {
        userClassNames.clear();
        for (IType type : userClasses) {
            userClassNames.add(type.getElementName());
        }
    }
    public SequenceDiagram parseMethod(IMethod method) throws JavaModelException {
        String className = method.getDeclaringType().getElementName();
        String methodName = method.getElementName();
        SequenceDiagram diagram = new SequenceDiagram(className, methodName);

        String source = method.getSource();
        if (source == null || source.isEmpty()) {
            return diagram;
        }

        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
        parser.setResolveBindings(true);

        ASTNode ast = parser.createAST(null);
        MethodCallVisitor visitor = new MethodCallVisitor(diagram, className);
        ast.accept(visitor);

        return diagram;
    }
    public SequenceDiagram parseClass(IType type) throws JavaModelException {
        String className = type.getElementName();
        SequenceDiagram diagram = new SequenceDiagram(className, "all methods");

        for (IMethod method : type.getMethods()) {
            String source = method.getSource();
            if (source == null || source.isEmpty()) {
                continue;
            }

            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(source.toCharArray());
            parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
            parser.setResolveBindings(true);

            ASTNode ast = parser.createAST(null);
            MethodCallVisitor visitor = new MethodCallVisitor(diagram, className);
            ast.accept(visitor);
        }

        return diagram;
    }
    private class MethodCallVisitor extends ASTVisitor {

        private SequenceDiagram diagram;
        private String currentClass;
        private String currentMethod;
        private int callSequence = 0;

        public MethodCallVisitor(SequenceDiagram diagram, String className) {
            this.diagram = diagram;
            this.currentClass = className;
        }

        private boolean isLibraryClass(String className) {
            if (className == null || className.isEmpty()) {
                return true;
            }

            className = className.replaceAll("<[^>]+>", "").trim();

            if (!userClassNames.isEmpty()) {
                String simpleClassName = className;
                if (className.contains(".")) {
                    simpleClassName = className.substring(className.lastIndexOf(".") + 1);
                }
                return !userClassNames.contains(simpleClassName);
            }
            String[] libraryPrefixes = {
                "java.", "javax.", "org.eclipse.", "org.junit.", "junit.",
                "org.springframework.", "com.google.", "com.sun.", "org.apache.",
                "org.w3c.", "org.xml.", "net.sf.", "org.hibernate.",
                "org.slf4j.", "org.log4j.", "net.sourceforge.",
                "com.fasterxml.jackson.", "org.json."
            };

            for (String prefix : libraryPrefixes) {
                if (className.startsWith(prefix)) return true;
            }

            if (className.endsWith("Exception") || className.endsWith("Error") || className.endsWith("Throwable")) {
                return true;
            }
            String[] commonTypes = {
                "String", "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short", "Number", "Void",
                "List", "Map", "Set", "Collection", "Iterator", "Iterable", "Enumeration",
                "ArrayList", "HashMap", "HashSet", "LinkedList", "TreeMap", "TreeSet",
                "Vector", "Stack", "Queue", "Deque", "LinkedHashMap", "LinkedHashSet",
                "PriorityQueue", "ArrayDeque", "ConcurrentHashMap", "WeakHashMap",
                "System", "Math", "Object", "Class", "Thread", "Runtime",
                "StringBuilder", "StringBuffer", "Arrays", "Collections", "Objects",
                "Scanner", "Random", "UUID", "Locale", "TimeZone",
                "File", "Path", "Files", "InputStream", "OutputStream", "Reader", "Writer",
                "BufferedReader", "BufferedWriter", "FileReader", "FileWriter", "PrintWriter",
                "ByteArrayInputStream", "ByteArrayOutputStream", "FileInputStream", "FileOutputStream",
                "Date", "Calendar", "LocalDate", "LocalDateTime", "LocalTime", "Instant",
                "Duration", "Period", "ZonedDateTime",
                "Pattern", "Matcher", "Optional", "Stream", "Collector",
                "E", "T", "K", "V", "N", "Unknown", "ChainedCall", "super", "this", "Null"
            };

            for (String type : commonTypes) {
                if (className.equals(type)) return true;
            }

            String[] frameworkClasses = {
                "Composite", "Button", "Label", "Text", "Canvas", "Shell", "Display",
                "Control", "Widget", "Viewer", "Table", "Tree", "Group", "Combo",
                "ScrolledComposite", "SashForm", "CLabel", "StyledText", "Browser",
                "ToolBar", "Menu", "MenuItem", "Dialog", "MessageBox", "FileDialog",
                "TabFolder", "TabItem", "ExpandBar", "ExpandItem", "CoolBar", "CoolItem",
                "Graph", "GraphNode", "GraphConnection", "Figure", "IFigure",
                "PolylineConnection", "ChopboxAnchor", "PolygonDecoration",
                "Action", "IAction", "ViewPart", "EditorPart", "IWorkbenchPage",
                "IWorkbenchPart", "IViewPart", "IEditorPart", "WorkbenchPart",
                "GC", "Image", "Color", "Font", "Rectangle", "Point", "Device",
                "ImageData", "ImageLoader", "RGB", "PaletteData", "Cursor",
                "IType", "IMethod", "IField", "ICompilationUnit", "IJavaProject",
                "IPackageFragment", "IPackageFragmentRoot", "IJavaElement",
                "AST", "ASTNode", "ASTParser", "ASTVisitor", "CompilationUnit",
                "MethodDeclaration", "TypeDeclaration", "FieldDeclaration",
                "IResource", "IProject", "IFile", "IFolder", "IWorkspace"
            };

            for (String framework : frameworkClasses) {
                if (className.equals(framework)) return true;
            }

            if (className.length() > 0 && Character.isLowerCase(className.charAt(0)) && !className.contains(".")) {
                return true;
            }

            if (className.length() == 1 && Character.isUpperCase(className.charAt(0))) {
                return true;
            }

            return false;
        }
        private boolean isLibraryMethod(String methodName) {
            if (methodName == null || methodName.isEmpty()) return true;

            String[] utilityMethods = {
                "add", "put", "get", "remove", "clear", "contains", "containsKey",
                "size", "isEmpty", "iterator", "stream", "forEach",
                "toString", "equals", "hashCode", "clone", "getClass",
                "substring", "charAt", "length", "trim", "split", "toLowerCase", "toUpperCase",
                "startsWith", "endsWith", "indexOf", "replace", "replaceAll",
                "println", "print", "printf", "close", "flush",
                "sleep", "wait", "notify", "notifyAll"
            };

            for (String utilMethod : utilityMethods) {
                if (methodName.equals(utilMethod)) return true;
            }

            if ((methodName.startsWith("get") || methodName.startsWith("set") || methodName.startsWith("is")) &&
                methodName.length() > 3 &&
                Character.isUpperCase(methodName.charAt(methodName.startsWith("is") ? 2 : 3))) {
                return true;
            }

            return false;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            currentMethod = node.getName().getIdentifier();
            diagram.addParticipant(currentClass);
            return true;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            String methodName = node.getName().getIdentifier();
            String calleeClass = extractCalleeClass(node.getExpression());

            if (isLibraryClass(calleeClass) || isLibraryMethod(methodName)) {
                return true;
            }

            callSequence++;
            diagram.addParticipant(currentClass);
            if (calleeClass != null && !calleeClass.isEmpty()) {
                diagram.addParticipant(calleeClass);
            }

            String label = methodName + "()";
            if (node.arguments() != null && node.arguments().size() > 0) {
                label = methodName + "(" + buildArgumentList(node.arguments()) + ")";
            }

            SequenceMessage message = new SequenceMessage(
                callSequence,
                currentClass,
                calleeClass != null ? calleeClass : "Unknown",
                label,
                SequenceMessage.MessageType.SYNC_CALL
            );

            diagram.addMessage(message);
            return true;
        }

        @Override
        public boolean visit(SuperMethodInvocation node) {
            callSequence++;

            String methodName = node.getName().getIdentifier();

            diagram.addParticipant(currentClass);
            diagram.addParticipant("super");

            String label = methodName + "()";
            if (node.arguments() != null && node.arguments().size() > 0) {
                label = methodName + "(" + buildArgumentList(node.arguments()) + ")";
            }

            SequenceMessage message = new SequenceMessage(
                callSequence,
                currentClass,
                "super",
                label,
                SequenceMessage.MessageType.SYNC_CALL
            );

            diagram.addMessage(message);

            return true;
        }

        @Override
        public boolean visit(ClassInstanceCreation node) {
            String className = node.getType().toString();

            if (isLibraryClass(className)) {
                return true;
            }

            callSequence++;
            diagram.addParticipant(currentClass);
            diagram.addParticipant(className);

            String label = "new " + className + "()";
            if (node.arguments() != null && node.arguments().size() > 0) {
                label = "new " + className + "(" + buildArgumentList(node.arguments()) + ")";
            }

            SequenceMessage message = new SequenceMessage(
                callSequence,
                currentClass,
                className,
                label,
                SequenceMessage.MessageType.CREATE
            );

            diagram.addMessage(message);
            return true;
        }
        private String extractCalleeClass(Expression expression) {
            if (expression == null) return currentClass;
            if (expression instanceof SimpleName) {
                return ((SimpleName) expression).getIdentifier();
            }
            if (expression instanceof MethodInvocation) {
                return "ChainedCall";
            }
            return expression.toString();
        }
        private String buildArgumentList(List arguments) {
            if (arguments.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                Object arg = arguments.get(i);
                if (arg instanceof Expression) {
                    String argStr = arg.toString().replaceAll("<[^>]+>", "");

                    if (argStr.contains("new ArrayList") || argStr.contains("new HashMap") ||
                        argStr.contains("new HashSet") || argStr.contains("new LinkedList")) {
                        argStr = "collection";
                    } else if (argStr.startsWith("\"") && argStr.endsWith("\"")) {
                        argStr = "string";
                    } else if (argStr.matches("\\d+")) {
                        argStr = "number";
                    } else if (argStr.equals("true") || argStr.equals("false")) {
                        argStr = "boolean";
                    } else if (argStr.equals("null")) {
                        argStr = "null";
                    }

                    if (argStr.length() > 15) {
                        argStr = argStr.substring(0, 12) + "...";
                    }

                    sb.append(argStr);
                }
            }
            return sb.toString();
        }
    }
}

package core.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;

import core.model.ClassNode;
import core.model.Field;
import core.model.IntermediateRepresentation;
import core.model.Method;
import core.model.Note;
import core.model.NoteReply;
import core.model.Relation;

/**
 * Generates class diagrams from IR using Zest
 * EPIC 3: Class Diagram Visualization
 */


//IMPORTANT NOTE - BE VERY CAREFUL WITH THIS FILE. RENDERING THE DIAGRAMS HERE SO WE ARE CLACULATING ALL THE MARGINS AND POSITIONS.
// ONE CHANGE CAN CAUSE OVERLAPPING OF STUFF. EVERY TEAM MEMBER SHOULD REVIEW THE CODE CAREFULLY!!
public class ClassDiagramGenerator {
    
    private Graph graph;
    private IntermediateRepresentation ir;
    private Map<String, GraphNode> nodeMap;
    private Map<String, List<GraphNode>> noteNodesMap; // Maps element ID to its note nodes
    private Map<GraphNode, Note> graphNodeToNoteMap; // Maps GraphNode to Note object
    private Map<GraphNode, String> graphNodeToClassIdMap; // Maps GraphNode to ClassNode ID
    private Map<GraphNode, GraphConnection> connectionNoteMap; // Maps connection note nodes to their associated connection
    private Map<GraphNode, Integer> connectionNoteIndexMap; // Maps connection note nodes to their index for stacking
    private Display display;
    private ConnectionNoteLinesLayer connectionLinesLayer; // Custom figure layer for drawing connection lines
    
    public ClassDiagramGenerator(Graph graph, Display display) {
        this.graph = graph;
        this.display = display;
        this.nodeMap = new HashMap<>();
        this.noteNodesMap = new HashMap<>();
        this.graphNodeToNoteMap = new HashMap<>();
        this.graphNodeToClassIdMap = new HashMap<>();
        this.connectionNoteMap = new HashMap<>();
        this.connectionNoteIndexMap = new HashMap<>();

        // Set up custom drawing for connection note lines
        setupCustomDrawing();
    }

    /**
     * Set up custom drawing to render lines from connection notes to association lines
     */
    private void setupCustomDrawing() {
        // Create a custom figure layer for drawing connection lines
        connectionLinesLayer = new ConnectionNoteLinesLayer();

        // Add it to the graph's figure hierarchy at the background
        // Get the graph's root figure and add our layer at index 0 (background)
        org.eclipse.draw2d.IFigure rootFigure = graph.getRootLayer();
        if (rootFigure != null) {
            // Add at the beginning so it's drawn BEFORE (behind) other elements
            rootFigure.add(connectionLinesLayer, 0);
        }
    }

    /**
     * Custom Draw2D figure that draws connection note lines in the background
     */
    private class ConnectionNoteLinesLayer extends org.eclipse.draw2d.Figure {

        public ConnectionNoteLinesLayer() {
            // Set bounds to cover the entire graph area
            setBounds(new org.eclipse.draw2d.geometry.Rectangle(0, 0, 5000, 5000));
            // Make it non-opaque so it doesn't block other elements
            setOpaque(false);
        }

        @Override
        protected void paintFigure(org.eclipse.draw2d.Graphics graphics) {
            super.paintFigure(graphics);

            // Set drawing style for connection note lines - solid like class notes
            graphics.setLineStyle(org.eclipse.draw2d.Graphics.LINE_SOLID);
            graphics.setLineWidth(1);
            graphics.setForegroundColor(org.eclipse.draw2d.ColorConstants.gray);

            // Draw orthogonal lines from each connection note to its association line's midpoint
            for (Map.Entry<GraphNode, GraphConnection> entry : connectionNoteMap.entrySet()) {
                GraphNode noteNode = entry.getKey();
                GraphConnection connection = entry.getValue();

                // Skip if note is not visible
                if (!noteNode.isVisible()) {
                    continue;
                }

                // Get note's center position
                int noteX = (int) (noteNode.getLocation().x + (noteNode.getSize().width / 2.0));
                int noteY = (int) (noteNode.getLocation().y + (noteNode.getSize().height / 2.0));

                // Get association line's source and destination positions
                GraphNode source = (GraphNode) connection.getSource();
                GraphNode dest = (GraphNode) connection.getDestination();

                if (source == null || dest == null) continue;

                // Calculate center points of source and destination
                double sourceX = source.getLocation().x + (source.getSize().width / 2.0);
                double sourceY = source.getLocation().y + (source.getSize().height / 2.0);
                double destX = dest.getLocation().x + (dest.getSize().width / 2.0);
                double destY = dest.getLocation().y + (dest.getSize().height / 2.0);

                // Calculate midpoint of the association line
                int midX = (int) ((sourceX + destX) / 2.0);
                int midY = (int) ((sourceY + destY) / 2.0);

                // Draw orthogonal (right-angle) line from note to association midpoint
                // Use 2-segment path: horizontal then vertical, or vertical then horizontal
                drawOrthogonalLine(graphics, noteX, noteY, midX, midY);
            }
        }

        /**
         * Draw an orthogonal (right-angle) line from (x1, y1) to (x2, y2)
         * Uses 2 segments with a midpoint to create right angles
         */
        private void drawOrthogonalLine(org.eclipse.draw2d.Graphics graphics,
                                       int x1, int y1, int x2, int y2) {
            // Calculate the intermediate point for orthogonal routing
            // Use horizontal-first routing: go horizontal, then vertical
            int midX = x2;  // Go to target X coordinate
            int midY = y1;  // Stay at source Y coordinate

            // Draw the two segments
            graphics.drawLine(x1, y1, midX, midY);  // Horizontal segment
            graphics.drawLine(midX, midY, x2, y2);  // Vertical segment
        }
    }

    /**
     * Generate complete class diagram from IR
     */
    public void generateDiagram(IntermediateRepresentation ir) {
        generateDiagram(ir, null);
    }
    
    /**
     * Generate complete class diagram from IR with note highlighting
     */
    public void generateDiagram(IntermediateRepresentation ir, List<Note> notes) {
        generateDiagram(ir, notes, null);
    }

    /**
     * Generate complete class diagram with external dependencies
     */
    public void generateDiagram(IntermediateRepresentation ir, List<Note> notes,
                                Map<String, java.util.Set<String>> externalDependencies) {
        this.ir = ir;

        // Clear existing graph
        clearGraph();

        // Create nodes for all classes
        for (ClassNode classNode : ir.getAllClasses().values()) {
            createClassNode(classNode, notes);
        }

        // Create package nodes for external dependencies
        if (externalDependencies != null && !externalDependencies.isEmpty()) {
            createExternalDependencyNodes(externalDependencies);
        }

        // Detect bidirectional connections to handle overlaps
        Map<String, List<Relation>> bidirectionalMap = detectBidirectionalConnections(ir.getAllRelations());

        // Create edges for all relations
        for (Relation relation : ir.getAllRelations()) {
            createRelationEdge(relation, notes, bidirectionalMap);
        }

        // Create dependency connections to external packages
        if (externalDependencies != null && !externalDependencies.isEmpty()) {
            createDependencyConnections(externalDependencies);
        }

        // Create visual note boxes
        if (notes != null && !notes.isEmpty()) {
            createNoteBoxes(notes);
        }
    }
    
    /**
     * Toggle visibility of note nodes without regenerating the diagram
     * Much more efficient than regenerating everything!
     */
    public void toggleNoteVisibility(boolean visible) {
        // Toggle visibility of all note nodes and their connections
        for (java.util.List<GraphNode> noteNodes : noteNodesMap.values()) {
            for (GraphNode noteNode : noteNodes) {
                noteNode.setVisible(visible);

                // Also toggle visibility of connections to/from this note
                // Use wildcard type to match the return type from getSourceConnections()
                java.util.List<? extends GraphConnection> connections = noteNode.getSourceConnections();
                for (GraphConnection conn : connections) {
                    conn.setVisible(visible);
                }

                java.util.List<? extends GraphConnection> targetConnections = noteNode.getTargetConnections();
                for (GraphConnection conn : targetConnections) {
                    conn.setVisible(visible);
                }
            }
        }

        // Redraw the graph (lightweight operation)
        graph.redraw();
    }

    /**
     * Add a single note to the diagram without regenerating everything
     */
    public void addSingleNote(Note note, String elementId) {
        if (note == null) return;

        // Check if this is a connection note (format: "ClassA->ClassB")
        if (elementId.contains("->")) {
            addNoteToConnection(note, elementId);
            return;
        }

        GraphNode classNode = nodeMap.get(elementId);
        if (classNode == null) {
            // Try with simple name
            String simpleName = getSimpleName(elementId);
            classNode = nodeMap.get(simpleName);
        }

        if (classNode != null) {
            // Get existing notes for this element
            java.util.List<GraphNode> existingNotes = noteNodesMap.get(elementId);
            int noteIndex = existingNotes != null ? existingNotes.size() : 0;

            // Create the note box
            GraphNode noteNode = createNoteBox(note, noteIndex);
            graphNodeToNoteMap.put(noteNode, note);

            // Position the note next to the class
            positionNoteNearClass(noteNode, classNode, noteIndex);

            // Connect note to class
            GraphConnection connection = new GraphConnection(graph, SWT.NONE, noteNode, classNode);
            connection.setLineStyle(SWT.LINE_DOT);
            connection.setLineWidth(1);
            connection.setLineColor(new Color(display, 150, 150, 150));

            // Apply orthogonal routing for right-angle connections
            applyOrthogonalRouting(connection);

            // Add to note nodes map
            if (existingNotes == null) {
                existingNotes = new ArrayList<>();
                noteNodesMap.put(elementId, existingNotes);
            }
            existingNotes.add(noteNode);

            // Just redraw - don't reapply layout!
            graph.redraw();
        }
    }

    /**
     * Add a note to an association line/connection
     */
    private void addNoteToConnection(Note note, String connectionId) {
        // Find the connection by its ID (format: "SourceClass->DestClass")
        String[] parts = connectionId.split("->");
        if (parts.length != 2) return;

        String sourceName = parts[0].trim();
        String destName = parts[1].trim();

        // Find the connection
        GraphConnection targetConnection = findConnection(sourceName, destName);
        if (targetConnection == null) return;

        // Get existing notes for this connection
        java.util.List<GraphNode> existingNotes = noteNodesMap.get(connectionId);
        int noteIndex = existingNotes != null ? existingNotes.size() : 0;

        // Create the note box
        GraphNode noteNode = createNoteBox(note, noteIndex);
        graphNodeToNoteMap.put(noteNode, note);

        // Track this as a connection note so we can reposition it after layout
        connectionNoteMap.put(noteNode, targetConnection);
        connectionNoteIndexMap.put(noteNode, noteIndex);

        // Add listener to repaint lines when this note is moved
        addConnectionNoteListener(noteNode);

        // Position the note near the connection midpoint
        positionNoteNearConnection(noteNode, targetConnection, noteIndex);

        // No connecting lines needed - the position alone makes it clear
        // the note is attached to the association line, not to the classes
        // (Zest doesn't support connecting nodes to connections directly)

        // Add to note nodes map
        if (existingNotes == null) {
            existingNotes = new ArrayList<>();
            noteNodesMap.put(connectionId, existingNotes);
        }
        existingNotes.add(noteNode);

        // Redraw
        graph.redraw();
    }

    /**
     * Find a connection between two nodes by their text
     */
    private GraphConnection findConnection(String sourceName, String destName) {
        // Use wildcard type to match the return type from getConnections()
        java.util.List<? extends GraphConnection> connections = graph.getConnections();

        for (GraphConnection conn : connections) {
            GraphNode source = (GraphNode) conn.getSource();
            GraphNode dest = (GraphNode) conn.getDestination();

            if (source != null && dest != null) {
                String srcText = source.getText().split("\n")[0].trim(); // Get first line (class name)
                String dstText = dest.getText().split("\n")[0].trim();

                // Try exact match first
                if (srcText.equals(sourceName) && dstText.equals(destName)) {
                    return conn;
                }

                // Try contains match for backward compatibility
                if (srcText.contains(sourceName) && dstText.contains(destName)) {
                    return conn;
                }

                // Also try reversed (in case connection direction is opposite)
                if (srcText.equals(destName) && dstText.equals(sourceName)) {
                    return conn;
                }
            }
        }

        // Debug: Log if connection not found
        System.err.println("Connection not found: " + sourceName + " -> " + destName);
        System.err.println("Available connections:");
        for (GraphConnection conn : connections) {
            GraphNode src = (GraphNode) conn.getSource();
            GraphNode dst = (GraphNode) conn.getDestination();
            if (src != null && dst != null) {
                System.err.println("  " + src.getText().split("\n")[0] + " -> " + dst.getText().split("\n")[0]);
            }
        }

        return null;
    }

    /**
     * Position a note near a connection/association line
     */
    private void positionNoteNearConnection(GraphNode noteNode, GraphConnection connection, int noteIndex) {
        GraphNode source = (GraphNode) connection.getSource();
        GraphNode dest = (GraphNode) connection.getDestination();

        if (source == null || dest == null) return;

        // Calculate midpoint of the connection
        double sourceX = source.getLocation().x + (source.getSize().width / 2.0);
        double sourceY = source.getLocation().y + (source.getSize().height / 2.0);
        double destX = dest.getLocation().x + (dest.getSize().width / 2.0);
        double destY = dest.getLocation().y + (dest.getSize().height / 2.0);

        double midX = (sourceX + destX) / 2.0;
        double midY = (sourceY + destY) / 2.0;

        // Offset the note perpendicular to the connection
        int perpendicularOffset = 80;
        int stackOffset = noteIndex * 100;

        // Calculate perpendicular direction
        double dx = destX - sourceX;
        double dy = destY - sourceY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {
            // Perpendicular vector (rotated 90 degrees)
            double perpX = -dy / length;
            double perpY = dx / length;

            // Position note perpendicular to the line
            double noteX = midX + (perpX * (perpendicularOffset + stackOffset));
            double noteY = midY + (perpY * (perpendicularOffset + stackOffset));

            noteNode.setLocation(noteX, noteY);
        } else {
            // Fallback if source and dest are at same location
            noteNode.setLocation(midX + 50, midY + 50);
        }
    }

    /**
     * Position a note node near its linked class
     */
    private void positionNoteNearClass(GraphNode noteNode, GraphNode classNode, int noteIndex) {
        // Get class node position and size
        double classX = classNode.getLocation().x;
        double classY = classNode.getLocation().y;
        double classWidth = classNode.getSize().width;
        double classHeight = classNode.getSize().height;

        // Calculate note position (to the right of the class, stacked vertically)
        int noteSpacing = 120; // Vertical spacing between multiple notes
        int horizontalOffset = 200; // Distance to the right of the class

        double noteX = classX + classWidth + horizontalOffset;
        double noteY = classY + (noteIndex * noteSpacing);

        // Set the note's location
        noteNode.setLocation(noteX, noteY);
    }

    /**
     * Update a note's display (e.g., when replies are added)
     */
    public void updateNoteDisplay(Note note) {
        // Find the note node
        for (GraphNode noteNode : graphNodeToNoteMap.keySet()) {
            if (graphNodeToNoteMap.get(noteNode) == note) {
                // Update the note text to show updated reply count
                String noteText = buildStickyNoteText(note);
                noteNode.setText(noteText);

                // Update tooltip
                String tooltip = buildNoteTooltip(note);
                noteNode.setTooltip(new Label(tooltip));

                // Redraw
                graph.redraw();
                return;
            }
        }
    }

    /**
     * Remove a single note from the diagram without regenerating everything
     */
    public void removeSingleNote(Note note) {
        // Find and remove the note node
        GraphNode nodeToRemove = null;
        for (GraphNode noteNode : graphNodeToNoteMap.keySet()) {
            if (graphNodeToNoteMap.get(noteNode) == note) {
                nodeToRemove = noteNode;
                break;
            }
        }

        if (nodeToRemove != null) {
            // Remove from all maps
            graphNodeToNoteMap.remove(nodeToRemove);

            // Remove from noteNodesMap
            for (java.util.List<GraphNode> noteList : noteNodesMap.values()) {
                noteList.remove(nodeToRemove);
            }

            // Remove from connection note maps (if this is a connection note)
            connectionNoteMap.remove(nodeToRemove);
            connectionNoteIndexMap.remove(nodeToRemove);

            // Dispose the node (this also removes its connections)
            nodeToRemove.dispose();

            // Redraw and repaint the connection lines layer
            if (connectionLinesLayer != null) {
                connectionLinesLayer.repaint();
            }
            graph.redraw();
        }
    }

    /**
     * Clear the graph
     */
    private void clearGraph() {
        // Remove all existing nodes and connections
        Object[] nodes = graph.getNodes().toArray();
        for (Object node : nodes) {
            ((GraphNode) node).dispose();
        }
        nodeMap.clear();
        noteNodesMap.clear();
        graphNodeToNoteMap.clear();
        graphNodeToClassIdMap.clear();
        connectionNoteMap.clear();
        connectionNoteIndexMap.clear();
    }
    
    /**
     * Create a graph node for a class
     */
    private void createClassNode(ClassNode classNode) {
        createClassNode(classNode, null);
    }
    
    private void createClassNode(ClassNode classNode, List<Note> notes) {
        GraphNode node = new GraphNode(graph, SWT.NONE);

        // Set node text with class details
        String nodeText = buildNodeText(classNode);
        node.setText(nodeText);

        // Set explicit size hints to help layout algorithm
        // Calculate size based on content
        int estimatedWidth = estimateNodeWidth(classNode);
        int estimatedHeight = estimateNodeHeight(classNode);
        node.setSize(estimatedWidth, estimatedHeight);

        // Check if node has notes
        boolean hasActiveNotes = false;
        boolean hasInactiveNotes = false;

        if (notes != null) {
            for (Note note : notes) {
                if (note.getLinkedElementId().equals(classNode.getName()) ||
                    note.getLinkedElementId().equals(classNode.getId())) {
                    if (note.isActive()) {
                        hasActiveNotes = true;
                    } else {
                        hasInactiveNotes = true;
                    }
                }
            }
        }

        // Set color based on type and notes
        Color color;
        if (hasInactiveNotes && !hasActiveNotes) {
            // Shadowed - gray out
            color = new Color(display, 200, 200, 200);
        } else if (hasActiveNotes) {
            // Has notes - add yellow tint
            color = getColorForTypeWithNotes(classNode.getType());
        } else {
            // Normal
            color = getColorForType(classNode.getType());
        }

        node.setBackgroundColor(color);

        // Add black border for visibility and professional appearance
        node.setBorderColor(new Color(display, 0, 0, 0)); // Black border
        node.setBorderWidth(2);

        // Create detailed tooltip
        String tooltip = buildTooltip(classNode);
        node.setTooltip(new Label(tooltip));

        // Store in maps for relation creation and ID lookup
        nodeMap.put(classNode.getId(), node);
        graphNodeToClassIdMap.put(node, classNode.getId());
    }

    /**
     * Estimate the width needed for a class node based on content
     */
    private int estimateNodeWidth(ClassNode classNode) {
        int maxLength = classNode.getName().length();

        // Check field names
        for (Field field : classNode.getFields()) {
            String fieldStr = field.getName() + " : " + getSimpleTypeName(field.getType());
            maxLength = Math.max(maxLength, fieldStr.length());
        }

        // Check method names (first 5 only)
        int methodCount = 0;
        for (Method method : classNode.getMethods()) {
            if (methodCount >= 5) break;
            String methodStr = method.getName() + "()";
            maxLength = Math.max(maxLength, methodStr.length());
            methodCount++;
        }

        // Approximate: 8 pixels per character, minimum 150, maximum 350
        int width = maxLength * 8;
        return Math.max(150, Math.min(width, 350));
    }

    /**
     * Estimate the height needed for a class node based on content
     */
    private int estimateNodeHeight(ClassNode classNode) {
        // Start with base height for class name and separators
        int lines = 4; // class name + 2 separators + empty indicators

        // Add lines for fields (each field is one line)
        lines += Math.max(1, classNode.getFields().size());

        // Add lines for methods (max 5 displayed + potential "more" line)
        int methodCount = Math.min(5, classNode.getMethods().size());
        lines += Math.max(1, methodCount);
        if (classNode.getMethods().size() > 5) {
            lines++; // "... more" line
        }

        // Approximate: 20 pixels per line, minimum 100, maximum 400
        int height = lines * 20;
        return Math.max(100, Math.min(height, 400));
    }
    
    /**
     * Build display text for node in classic UML format
     */
    private String buildNodeText(ClassNode classNode) {
        StringBuilder sb = new StringBuilder();

        // Add stereotype if interface or enum
        if (classNode.getType().equals("interface")) {
            sb.append("«interface»\n");
        } else if (classNode.getType().equals("enum")) {
            sb.append("«enum»\n");
        }

        // Class name (bold with larger context)
        sb.append(classNode.getName());
        sb.append("\n");

        // Separator line
        sb.append("─────────────────\n");

        // Attributes section
        if (!classNode.getFields().isEmpty()) {
            for (Field field : classNode.getFields()) {
                sb.append(getVisibilitySymbol(field.getVisibility()));
                sb.append(" ");
                sb.append(field.getName());
                sb.append(" : ");
                sb.append(getSimpleTypeName(field.getType()));
                if (field.isStatic()) {
                    sb.append(" {static}");
                }
                sb.append("\n");
            }
        } else {
            // Show empty section indicator
            sb.append("  (no attributes)\n");
        }

        // Separator line
        sb.append("─────────────────\n");

        // Methods section
        if (!classNode.getMethods().isEmpty()) {
            int methodCount = 0;
            for (Method method : classNode.getMethods()) {
                if (methodCount >= 5) {
                    sb.append("  ... (").append(classNode.getMethods().size() - 5).append(" more)\n");
                    break;
                }
                sb.append(getVisibilitySymbol(method.getVisibility()));
                sb.append(" ");
                sb.append(method.getName());
                sb.append("()");
                sb.append(" : ");
                sb.append(getSimpleTypeName(method.getReturnType()));
                if (method.isStatic()) {
                    sb.append(" {static}");
                }
                sb.append("\n");
                methodCount++;
            }
        } else {
            // Show empty section indicator
            sb.append("  (no methods)\n");
        }

        return sb.toString();
    }

    /**
     * Get UML visibility symbol
     */
    private String getVisibilitySymbol(String visibility) {
        switch (visibility) {
            case "public":
                return "+";
            case "private":
                return "-";
            case "protected":
                return "#";
            case "package":
                return "~";
            default:
                return " ";
        }
    }

    /**
     * Get simple type name (without package)
     */
    private String getSimpleTypeName(String fullType) {
        if (fullType == null) {
            return "void";
        }
        // Remove generic type parameters
        int genericStart = fullType.indexOf('<');
        if (genericStart > 0) {
            String baseType = fullType.substring(0, genericStart);
            String genericPart = fullType.substring(genericStart);
            return getSimpleName(baseType) + genericPart;
        }
        return getSimpleName(fullType);
    }
    
    /**
     * Build detailed tooltip
     */
    private String buildTooltip(ClassNode classNode) {
        StringBuilder sb = new StringBuilder();

        sb.append("Full Path: ").append(classNode.getId());
        sb.append("\nType: ").append(classNode.getType());
        sb.append("\n\n");

        sb.append("Attributes (").append(classNode.getFields().size()).append("):\n");
        if (classNode.getFields().isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (Field field : classNode.getFields()) {
                sb.append("  ").append(field.toString()).append("\n");
            }
        }

        sb.append("\nMethods (").append(classNode.getMethods().size()).append("):\n");
        if (classNode.getMethods().isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (Method method : classNode.getMethods()) {
                sb.append("  ").append(method.toString()).append("\n");
            }
        }

        return sb.toString();
    }
    
    /**
     * Get color based on class type
     * Using light colors to differentiate class types
     */
    private Color getColorForType(String type) {
        switch (type) {
            case "class":
                return new Color(display, 173, 216, 230); // Light blue
            case "interface":
                return new Color(display, 144, 238, 144); // Light green
            case "enum":
                return new Color(display, 255, 255, 224); // Light yellow
            default:
                return new Color(display, 211, 211, 211); // Light gray
        }
    }

    /**
     * Get color for type with note indicator (yellow tint)
     */
    private Color getColorForTypeWithNotes(String type) {
        switch (type) {
            case "class":
                return new Color(display, 255, 255, 200); // Light yellow-blue
            case "interface":
                return new Color(display, 220, 255, 200); // Light yellow-green
            case "enum":
                return new Color(display, 255, 255, 180); // Brighter yellow
            default:
                return new Color(display, 255, 255, 200); // Light yellow-gray
        }
    }
    
    /**
     * Detect bidirectional connections (A->B and B->A)
     */
    private Map<String, List<Relation>> detectBidirectionalConnections(List<Relation> relations) {
        Map<String, List<Relation>> bidirectionalMap = new HashMap<>();

        for (int i = 0; i < relations.size(); i++) {
            Relation r1 = relations.get(i);
            for (int j = i + 1; j < relations.size(); j++) {
                Relation r2 = relations.get(j);

                // Check if bidirectional (A->B and B->A)
                if (r1.getSourceId().equals(r2.getTargetId()) &&
                    r1.getTargetId().equals(r2.getSourceId())) {

                    String key = createConnectionKey(r1.getSourceId(), r1.getTargetId());
                    List<Relation> pair = new ArrayList<>();
                    pair.add(r1);
                    pair.add(r2);
                    bidirectionalMap.put(key, pair);
                }
            }
        }

        return bidirectionalMap;
    }

    /**
     * Create a unique key for a connection pair
     */
    private String createConnectionKey(String id1, String id2) {
        // Sort to ensure consistent key regardless of direction
        if (id1.compareTo(id2) < 0) {
            return id1 + "<->" + id2;
        } else {
            return id2 + "<->" + id1;
        }
    }

    /**
     * Check if this relation is part of a bidirectional connection
     */
    private boolean isBidirectional(Relation relation, Map<String, List<Relation>> bidirectionalMap) {
        String key = createConnectionKey(relation.getSourceId(), relation.getTargetId());
        return bidirectionalMap.containsKey(key);
    }

    /**
     * Get the index of this relation in its bidirectional pair (0 or 1)
     */
    private int getBidirectionalIndex(Relation relation, Map<String, List<Relation>> bidirectionalMap) {
        String key = createConnectionKey(relation.getSourceId(), relation.getTargetId());
        List<Relation> pair = bidirectionalMap.get(key);
        if (pair != null) {
            return pair.indexOf(relation);
        }
        return -1;
    }

    /**
     * Create an edge for a relation
     */
    private void createRelationEdge(Relation relation) {
        createRelationEdge(relation, null, new HashMap<>());
    }

    private void createRelationEdge(Relation relation, List<Note> notes) {
        createRelationEdge(relation, notes, new HashMap<>());
    }

    private void createRelationEdge(Relation relation, List<Note> notes, Map<String, List<Relation>> bidirectionalMap) {
        GraphNode source = nodeMap.get(relation.getSourceId());
        GraphNode target = nodeMap.get(relation.getTargetId());

        // Skip if nodes don't exist (external classes)
        if (source == null || target == null) {
            return;
        }

        // Create connection with appropriate style
        int connectionStyle = getConnectionStyle(relation.getType());
        GraphConnection connection = new GraphConnection(graph, connectionStyle, source, target);

        // All lines are BLACK for standard UML appearance
        Color lineColor = new Color(display, 0, 0, 0); // Black
        connection.setLineColor(lineColor);

        // Set line style based on relation type (solid vs dashed)
        int lineStyle = getLineStyle(relation.getType());
        connection.setLineStyle(lineStyle);

        // Set line width - BOLDER (3 pixels instead of 2)
        connection.setLineWidth(3);

        // Set connection decorations (arrow style)
        setConnectionDecorations(connection, relation.getType());

        // Apply orthogonal (Manhattan) routing for cleaner right-angle connections
        // Note: Manhattan routing handles bidirectional overlap naturally, so we don't need custom curves
        applyOrthogonalRouting(connection);

        // No text label - let the arrow decorations speak for themselves (standard UML)
        // Relationship type is shown in tooltip

        // Set tooltip
        String tooltip = buildRelationTooltip(relation);
        connection.setTooltip(new Label(tooltip));
    }
    
    /**
     * Apply orthogonal (Manhattan) routing to connection for right-angle arrows
     */
    private void applyOrthogonalRouting(GraphConnection connection) {
        try {
            org.eclipse.draw2d.PolylineConnection polyline =
                (org.eclipse.draw2d.PolylineConnection) connection.getConnectionFigure();

            // Use Manhattan (orthogonal) router for right-angle connections
            org.eclipse.draw2d.ManhattanConnectionRouter router =
                new org.eclipse.draw2d.ManhattanConnectionRouter();

            polyline.setConnectionRouter(router);

        } catch (Exception e) {
            // If routing fails, connection will remain direct (acceptable fallback)
            System.err.println("Could not apply orthogonal routing: " + e.getMessage());
        }
    }

    /**
     * Apply curve to bidirectional connections to avoid overlap
     */
    private void applyBidirectionalCurve(GraphConnection connection, Relation relation,
                                         Map<String, List<Relation>> bidirectionalMap) {
        try {
            org.eclipse.draw2d.PolylineConnection polyline =
                (org.eclipse.draw2d.PolylineConnection) connection.getConnectionFigure();

            // Determine which connection this is (first or second)
            int index = getBidirectionalIndex(relation, bidirectionalMap);

            if (index == 0) {
                // First connection - curve upward/left
                org.eclipse.draw2d.BendpointConnectionRouter router =
                    new org.eclipse.draw2d.BendpointConnectionRouter();
                polyline.setConnectionRouter(router);

                // Create a bendpoint to curve the line
                org.eclipse.draw2d.RelativeBendpoint bendpoint1 =
                    new org.eclipse.draw2d.RelativeBendpoint(polyline);
                bendpoint1.setRelativeDimensions(
                    new org.eclipse.draw2d.geometry.Dimension(-30, -30),
                    new org.eclipse.draw2d.geometry.Dimension(30, -30)
                );
                bendpoint1.setWeight(0.5f);

                List<org.eclipse.draw2d.Bendpoint> bendpoints = new ArrayList<>();
                bendpoints.add(bendpoint1);
                polyline.setRoutingConstraint(bendpoints);

            } else if (index == 1) {
                // Second connection - curve downward/right
                org.eclipse.draw2d.BendpointConnectionRouter router =
                    new org.eclipse.draw2d.BendpointConnectionRouter();
                polyline.setConnectionRouter(router);

                // Create a bendpoint to curve the line in opposite direction
                org.eclipse.draw2d.RelativeBendpoint bendpoint1 =
                    new org.eclipse.draw2d.RelativeBendpoint(polyline);
                bendpoint1.setRelativeDimensions(
                    new org.eclipse.draw2d.geometry.Dimension(30, 30),
                    new org.eclipse.draw2d.geometry.Dimension(-30, 30)
                );
                bendpoint1.setWeight(0.5f);

                List<org.eclipse.draw2d.Bendpoint> bendpoints = new ArrayList<>();
                bendpoints.add(bendpoint1);
                polyline.setRoutingConstraint(bendpoints);
            }

        } catch (Exception e) {
            // If curving fails, connections will remain straight (acceptable fallback)
            System.err.println("Could not apply curve to bidirectional connection: " + e.getMessage());
        }
    }

    /**
     * Set proper arrow decorations for different relationship types
     */
    private void setConnectionDecorations(GraphConnection connection, String relationType) {
        // Import required for decorations
        org.eclipse.draw2d.PolylineConnection polyline =
            (org.eclipse.draw2d.PolylineConnection) connection.getConnectionFigure();

        // Set source and target decorations based on UML standards
        switch (relationType) {
            case "inheritance":
                // Hollow triangle arrow at target (parent class)
                org.eclipse.draw2d.PolygonDecoration triangle = createHollowTriangle();
                polyline.setTargetDecoration(triangle);
                polyline.setLineWidth(3);
                break;

            case "implements":
                // Hollow triangle arrow at target (interface) with dashed line
                org.eclipse.draw2d.PolygonDecoration triangle2 = createHollowTriangle();
                polyline.setTargetDecoration(triangle2);
                polyline.setLineWidth(3);
                break;

            case "association":
                // Simple arrow at target
                org.eclipse.draw2d.PolylineDecoration arrow = new org.eclipse.draw2d.PolylineDecoration();
                arrow.setScale(12, 6); // Make arrow bigger and more visible
                polyline.setTargetDecoration(arrow);
                polyline.setLineWidth(3);
                break;

            case "aggregation":
                // Hollow diamond at source (container side)
                org.eclipse.draw2d.PolygonDecoration diamond = createHollowDiamond();
                polyline.setSourceDecoration(diamond);
                polyline.setTargetDecoration(null); // No arrow at target - just diamond at source
                polyline.setLineWidth(3);
                break;

            case "composition":
                // Filled diamond at source (container side)
                org.eclipse.draw2d.PolygonDecoration filledDiamond = createFilledDiamond();
                polyline.setSourceDecoration(filledDiamond);
                polyline.setTargetDecoration(null); // No arrow at target - just diamond at source
                polyline.setLineWidth(3);
                break;

            default:
                // Default arrow
                org.eclipse.draw2d.PolylineDecoration defaultArrow = new org.eclipse.draw2d.PolylineDecoration();
                defaultArrow.setScale(12, 6);
                polyline.setTargetDecoration(defaultArrow);
                polyline.setLineWidth(3);
                break;
        }
    }

    /**
     * Create a hollow triangle decoration (for inheritance/implements)
     */
    private org.eclipse.draw2d.PolygonDecoration createHollowTriangle() {
        org.eclipse.draw2d.PolygonDecoration triangle = new org.eclipse.draw2d.PolygonDecoration();

        // Define triangle shape: pointing right (towards target)
        org.eclipse.draw2d.geometry.PointList points = new org.eclipse.draw2d.geometry.PointList();
        points.addPoint(0, 0);      // Tip of arrow
        points.addPoint(-12, -6);   // Top left
        points.addPoint(-12, 6);    // Bottom left
        points.addPoint(0, 0);      // Back to tip (close the shape)

        triangle.setTemplate(points);
        triangle.setScale(1, 1);

        // Hollow (not filled) - white background with black outline
        triangle.setFill(true);
        triangle.setBackgroundColor(new Color(display, 255, 255, 255)); // White fill
        triangle.setForegroundColor(new Color(display, 0, 0, 0)); // Black outline
        triangle.setLineWidth(2);

        return triangle;
    }

    /**
     * Create a hollow diamond decoration (for aggregation)
     */
    private org.eclipse.draw2d.PolygonDecoration createHollowDiamond() {
        org.eclipse.draw2d.PolygonDecoration diamond = new org.eclipse.draw2d.PolygonDecoration();

        // Define diamond shape
        org.eclipse.draw2d.geometry.PointList points = new org.eclipse.draw2d.geometry.PointList();
        points.addPoint(0, 0);      // Right point (towards line)
        points.addPoint(-6, -6);    // Top point
        points.addPoint(-12, 0);    // Left point
        points.addPoint(-6, 6);     // Bottom point
        points.addPoint(0, 0);      // Back to right (close the shape)

        diamond.setTemplate(points);
        diamond.setScale(1, 1);

        // Hollow (not filled) - white background with black outline
        diamond.setFill(true);
        diamond.setBackgroundColor(new Color(display, 255, 255, 255)); // White fill
        diamond.setForegroundColor(new Color(display, 0, 0, 0)); // Black outline
        diamond.setLineWidth(2);

        return diamond;
    }

    /**
     * Create a filled diamond decoration (for composition)
     */
    private org.eclipse.draw2d.PolygonDecoration createFilledDiamond() {
        org.eclipse.draw2d.PolygonDecoration diamond = new org.eclipse.draw2d.PolygonDecoration();

        // Define diamond shape (same as hollow)
        org.eclipse.draw2d.geometry.PointList points = new org.eclipse.draw2d.geometry.PointList();
        points.addPoint(0, 0);      // Right point (towards line)
        points.addPoint(-6, -6);    // Top point
        points.addPoint(-12, 0);    // Left point
        points.addPoint(-6, 6);     // Bottom point
        points.addPoint(0, 0);      // Back to right (close the shape)

        diamond.setTemplate(points);
        diamond.setScale(1, 1);

        // Filled with black
        diamond.setFill(true);
        diamond.setBackgroundColor(new Color(display, 0, 0, 0)); // Black fill
        diamond.setForegroundColor(new Color(display, 0, 0, 0)); // Black outline
        diamond.setLineWidth(2);

        return diamond;
    }

    /**
     * Get connection style for relation type (arrow types)
     */
    private int getConnectionStyle(String relationType) {
        switch (relationType) {
            case "inheritance":
            case "implements":
            case "association":
                // Directed connection with arrow
                return ZestStyles.CONNECTIONS_DIRECTED;
            case "aggregation":
            case "composition":
                // Solid connection without default arrow (we add custom diamond)
                return ZestStyles.CONNECTIONS_SOLID;
            default:
                return ZestStyles.CONNECTIONS_DIRECTED;
        }
    }

    /**
     * Get line style (solid vs dashed) for relation type
     * Following standard UML conventions
     */
    private int getLineStyle(String relationType) {
        switch (relationType) {
            case "implements":
                // Dashed line for interface implementation
                return SWT.LINE_DASH;
            case "inheritance":
            case "association":
            case "aggregation":
            case "composition":
                // Solid line for other relationships
                return SWT.LINE_SOLID;
            default:
                return SWT.LINE_SOLID;
        }
    }

    /**
     * Build tooltip for relation
     */
    private String buildRelationTooltip(Relation relation) {
        return String.format("%s\n%s → %s%s",
            relation.getType(),
            getSimpleName(relation.getSourceId()),
            getSimpleName(relation.getTargetId()),
            relation.isTargetExternal() ? " (external)" : ""
        );
    }
    
    /**
     * Get simple name from fully qualified name
     */
    private String getSimpleName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }
    
    /**
     * Create package nodes for external dependencies (Eclipse package icon style)
     */
    private void createExternalDependencyNodes(Map<String, java.util.Set<String>> externalDependencies) {
        for (String packageName : externalDependencies.keySet()) {
            GraphNode packageNode = new GraphNode(graph, SWT.NONE);

            // Package icon text (folder icon + package name)
            String nodeText = "\uD83D\uDCC1 " + packageName; // 📁 folder icon
            packageNode.setText(nodeText);

            // Package icon styling - light yellow/tan like Eclipse
            Color packageColor = new Color(display, 255, 248, 220); // Light beige/package color
            packageNode.setBackgroundColor(packageColor);

            // Thinner black border
            packageNode.setBorderColor(new Color(display, 0, 0, 0));
            packageNode.setBorderWidth(1);

            // Smaller size for packages
            packageNode.setSize(150, 50);

            // Add tooltip showing which classes use this package
            java.util.Set<String> users = externalDependencies.get(packageName);
            String tooltip = "External Package: " + packageName + "\nUsed by: " + String.join(", ", users);
            packageNode.setTooltip(new Label(tooltip));

            // Store in nodeMap with package prefix
            nodeMap.put("package:" + packageName, packageNode);
        }
    }

    /**
     * Create dependency connections from classes to external packages
     */
    private void createDependencyConnections(Map<String, java.util.Set<String>> externalDependencies) {
        for (Map.Entry<String, java.util.Set<String>> entry : externalDependencies.entrySet()) {
            String packageName = entry.getKey();
            GraphNode packageNode = nodeMap.get("package:" + packageName);

            if (packageNode == null) {
                continue; // Skip if package node wasn't created
            }

            // Create connections from each class to this package
            for (String className : entry.getValue()) {
                // Find the class node
                GraphNode classNode = findClassNodeByName(className);

                if (classNode != null) {
                    // Create dashed dependency arrow from class to package
                    GraphConnection connection = new GraphConnection(
                        graph,
                        ZestStyles.CONNECTIONS_DIRECTED,
                        classNode,
                        packageNode
                    );

                    // Style: dashed line, lighter color
                    connection.setLineColor(new Color(display, 100, 100, 100)); // Gray
                    connection.setLineStyle(SWT.LINE_DOT); // Dotted line for dependencies
                    connection.setLineWidth(2);

                    // Simple arrow at package
                    org.eclipse.draw2d.PolylineConnection polyline =
                        (org.eclipse.draw2d.PolylineConnection) connection.getConnectionFigure();
                    org.eclipse.draw2d.PolylineDecoration arrow = new org.eclipse.draw2d.PolylineDecoration();
                    arrow.setScale(8, 4);
                    polyline.setTargetDecoration(arrow);

                    // Apply orthogonal routing to dependency connections too
                    applyOrthogonalRouting(connection);

                    // Tooltip
                    connection.setTooltip(new Label(className + " depends on " + packageName));
                }
            }
        }
    }

    /**
     * Find class node by class name (search through nodeMap)
     */
    private GraphNode findClassNodeByName(String className) {
        for (Map.Entry<String, GraphNode> entry : nodeMap.entrySet()) {
            String key = entry.getKey();
            // Skip package nodes
            if (key.startsWith("package:")) {
                continue;
            }
            // Match class name (get simple name from fully qualified)
            if (key.endsWith("." + className) || key.equals(className)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Create visual note boxes for all notes
     */
    private void createNoteBoxes(List<Note> notes) {
        // Group notes by element ID
        Map<String, List<Note>> notesByElement = new HashMap<>();
        for (Note note : notes) {
            String elementId = note.getLinkedElementId();
            notesByElement.computeIfAbsent(elementId, k -> new ArrayList<>()).add(note);
        }

        // Create note boxes for each element
        for (Map.Entry<String, List<Note>> entry : notesByElement.entrySet()) {
            String elementId = entry.getKey();
            List<Note> elementNotes = entry.getValue();

            // Check if this is a connection note (format: "ClassA->ClassB")
            if (elementId.contains("->")) {
                createNoteBoxesForConnection(elementId, elementNotes);
                continue;
            }

            GraphNode classNode = nodeMap.get(elementId);

            if (classNode == null) {
                // Try with simple name (for backward compatibility)
                String simpleName = getSimpleName(elementId);
                classNode = nodeMap.get(simpleName);
            }

            if (classNode != null) {
                createNoteBoxesForElement(classNode, elementId, elementNotes);
            } else {
                // Class doesn't exist - create orphaned notes (grayed out)
                createOrphanedNoteBoxes(elementId, elementNotes);
            }
        }
    }

    /**
     * Create note boxes for a connection/association line
     */
    private void createNoteBoxesForConnection(String connectionId, List<Note> notes) {
        // Find the connection
        String[] parts = connectionId.split("->");
        if (parts.length != 2) return;

        String sourceName = parts[0].trim();
        String destName = parts[1].trim();
        GraphConnection connection = findConnection(sourceName, destName);

        // If connection doesn't exist, create orphaned connection notes
        if (connection == null) {
            createOrphanedConnectionNotes(connectionId, notes);
            return;
        }

        List<GraphNode> noteNodes = new ArrayList<>();
        int noteIndex = 0;

        for (Note note : notes) {
            GraphNode noteNode = createNoteBox(note, noteIndex);
            noteNodes.add(noteNode);

            // Store mapping
            graphNodeToNoteMap.put(noteNode, note);

            // Track this as a connection note so we can reposition it after layout
            connectionNoteMap.put(noteNode, connection);
            connectionNoteIndexMap.put(noteNode, noteIndex);

            // Add listener to repaint lines when this note is moved
            addConnectionNoteListener(noteNode);

            // Position near the connection
            positionNoteNearConnection(noteNode, connection, noteIndex);

            // No connecting lines needed - the position alone makes it clear
            // the note is attached to the association line, not to the classes
            // (Zest doesn't support connecting nodes to connections directly)

            noteIndex++;
        }

        noteNodesMap.put(connectionId, noteNodes);
    }

    /**
     * Create note boxes for a specific element
     */
    private void createNoteBoxesForElement(GraphNode classNode, String elementId, List<Note> notes) {
        List<GraphNode> noteNodes = new ArrayList<>();
        int noteIndex = 0;

        for (Note note : notes) {
            GraphNode noteNode = createNoteBox(note, noteIndex);
            noteNodes.add(noteNode);

            // Store mapping from GraphNode to Note
            graphNodeToNoteMap.put(noteNode, note);

            // Position the note next to the class (prevents overlapping!)
            positionNoteNearClass(noteNode, classNode, noteIndex);

            // Connect note to class with a dashed line
            GraphConnection connection = new GraphConnection(graph, SWT.NONE, noteNode, classNode);
            connection.setLineStyle(SWT.LINE_DOT);
            connection.setLineWidth(1);
            connection.setLineColor(new Color(display, 150, 150, 150)); // Gray

            // Apply orthogonal routing to note connections too
            applyOrthogonalRouting(connection);

            noteIndex++;
        }

        noteNodesMap.put(elementId, noteNodes);
    }

    /**
     * Create orphaned note boxes (for notes whose classes no longer exist)
     */
    private void createOrphanedNoteBoxes(String elementId, List<Note> notes) {
        List<GraphNode> noteNodes = new ArrayList<>();
        int noteIndex = 0;

        for (Note note : notes) {
            GraphNode noteNode = createOrphanedNoteBox(note, elementId, noteIndex);
            noteNodes.add(noteNode);

            // Store mapping from GraphNode to Note
            graphNodeToNoteMap.put(noteNode, note);

            noteIndex++;
        }

        noteNodesMap.put(elementId, noteNodes);
    }

    /**
     * Create orphaned connection notes (for notes whose associations no longer exist)
     */
    private void createOrphanedConnectionNotes(String connectionId, List<Note> notes) {
        List<GraphNode> noteNodes = new ArrayList<>();
        int noteIndex = 0;

        for (Note note : notes) {
            GraphNode noteNode = createOrphanedConnectionNoteBox(note, connectionId, noteIndex);
            noteNodes.add(noteNode);

            // Store mapping from GraphNode to Note
            graphNodeToNoteMap.put(noteNode, note);

            noteIndex++;
        }

        noteNodesMap.put(connectionId, noteNodes);
    }

    /**
     * Create a single orphaned connection note box (grayed out, association missing)
     */
    private GraphNode createOrphanedConnectionNoteBox(Note note, String missingConnectionId, int index) {
        GraphNode noteNode = new GraphNode(graph, SWT.NONE);

        // Build note text with orphaned indicator
        String noteText = buildOrphanedConnectionNoteText(note, missingConnectionId);
        noteNode.setText(noteText);

        // Set gray color to indicate orphaned status
        Color bgColor = new Color(display, 200, 200, 200); // Gray for orphaned
        noteNode.setBackgroundColor(bgColor);

        // Set gray border
        noteNode.setBorderColor(new Color(display, 150, 150, 150)); // Gray border
        noteNode.setBorderWidth(3);

        // Set tooltip with warning about missing association
        String tooltip = buildOrphanedConnectionNoteTooltip(note, missingConnectionId);
        noteNode.setTooltip(new Label(tooltip));

        return noteNode;
    }

    /**
     * Create a single orphaned note box (grayed out, no connection to class)
     */
    private GraphNode createOrphanedNoteBox(Note note, String missingClassId, int index) {
        GraphNode noteNode = new GraphNode(graph, SWT.NONE);

        // Build note text with orphaned indicator
        String noteText = buildOrphanedNoteText(note, missingClassId);
        noteNode.setText(noteText);

        // Set gray color to indicate orphaned status
        Color bgColor = new Color(display, 200, 200, 200); // Gray for orphaned
        noteNode.setBackgroundColor(bgColor);

        // Set gray border
        noteNode.setBorderColor(new Color(display, 150, 150, 150)); // Gray border
        noteNode.setBorderWidth(3);

        // Set tooltip with warning about missing class
        String tooltip = buildOrphanedNoteTooltip(note, missingClassId);
        noteNode.setTooltip(new Label(tooltip));

        return noteNode;
    }

    /**
     * Build display text for orphaned note
     */
    private String buildOrphanedNoteText(Note note, String missingClassId) {
        StringBuilder sb = new StringBuilder();

        // Orphaned indicator
        sb.append("⚠️ Orphaned Note\n");

        // Content (truncated to 25 chars for compact size)
        String content = note.getContent();
        if (content.length() > 25) {
            content = content.substring(0, 22) + "...";
        }
        sb.append(content);

        sb.append("\n──────\n");

        // Show missing class
        String simpleName = getSimpleName(missingClassId);
        if (simpleName.length() > 12) {
            simpleName = simpleName.substring(0, 9) + "...";
        }
        sb.append("Class: ").append(simpleName).append("\n");
        sb.append("(not found)");

        return sb.toString();
    }

    /**
     * Build tooltip for orphaned note
     */
    private String buildOrphanedNoteTooltip(Note note, String missingClassId) {
        StringBuilder sb = new StringBuilder();

        sb.append("⚠️ ORPHANED NOTE ⚠️\n\n");
        sb.append("This note is linked to a class that no longer exists:\n");
        sb.append("Missing Class: ").append(missingClassId).append("\n\n");

        sb.append("Note by ").append(note.getAuthor()).append("\n");
        sb.append("Created: ").append(note.getFormattedDate()).append("\n\n");

        sb.append("Content:\n").append(note.getContent()).append("\n");

        if (note.getReplies() != null && !note.getReplies().isEmpty()) {
            sb.append("\n").append(note.getReplies().size()).append(" Replies:\n");
            for (int i = 0; i < Math.min(3, note.getReplies().size()); i++) {
                NoteReply reply = note.getReplies().get(i);
                sb.append("  • ").append(reply.getAuthor()).append(": ");
                String replyContent = reply.getContent();
                if (replyContent.length() > 40) {
                    replyContent = replyContent.substring(0, 37) + "...";
                }
                sb.append(replyContent).append("\n");
            }
            if (note.getReplies().size() > 3) {
                sb.append("  ... and ").append(note.getReplies().size() - 3).append(" more");
            }
        }

        sb.append("\n\nTip: Delete this note or restore the class to remove this warning.");

        return sb.toString();
    }

    /**
     * Build display text for orphaned connection note
     */
    private String buildOrphanedConnectionNoteText(Note note, String missingConnectionId) {
        StringBuilder sb = new StringBuilder();

        // Orphaned indicator
        sb.append("⚠️ Orphaned Note\n");

        // Content (truncated to 25 chars for compact size)
        String content = note.getContent();
        if (content.length() > 25) {
            content = content.substring(0, 22) + "...";
        }
        sb.append(content);

        sb.append("\n──────\n");

        // Show missing association
        String[] parts = missingConnectionId.split("->");
        if (parts.length == 2) {
            String source = parts[0].trim();
            String dest = parts[1].trim();
            if (source.length() > 8) source = source.substring(0, 8) + "...";
            if (dest.length() > 8) dest = dest.substring(0, 8) + "...";
            sb.append(source).append(" → ").append(dest).append("\n");
        } else {
            sb.append("Association: ").append(missingConnectionId).append("\n");
        }
        sb.append("(not found)");

        return sb.toString();
    }

    /**
     * Build tooltip for orphaned connection note
     */
    private String buildOrphanedConnectionNoteTooltip(Note note, String missingConnectionId) {
        StringBuilder sb = new StringBuilder();

        sb.append("⚠️ ORPHANED NOTE ⚠️\n\n");
        sb.append("This note is linked to an association that no longer exists:\n");
        sb.append("Missing Association: ").append(missingConnectionId).append("\n\n");

        sb.append("Note by ").append(note.getAuthor()).append("\n");
        sb.append("Created: ").append(note.getFormattedDate()).append("\n\n");

        sb.append("Content:\n").append(note.getContent()).append("\n");

        if (note.getReplies() != null && !note.getReplies().isEmpty()) {
            sb.append("\n").append(note.getReplies().size()).append(" Replies:\n");
            for (int i = 0; i < Math.min(3, note.getReplies().size()); i++) {
                NoteReply reply = note.getReplies().get(i);
                sb.append("  • ").append(reply.getAuthor()).append(": ");
                String replyContent = reply.getContent();
                if (replyContent.length() > 40) {
                    replyContent = replyContent.substring(0, 37) + "...";
                }
                sb.append(replyContent).append("\n");
            }
            if (note.getReplies().size() > 3) {
                sb.append("  ... and ").append(note.getReplies().size() - 3).append(" more");
            }
        }

        sb.append("\n\nTip: Delete this note or restore the association to remove this warning.");

        return sb.toString();
    }

    /**
     * Create a single note box styled as a sticky note
     */
    private GraphNode createNoteBox(Note note, int index) {
        GraphNode noteNode = new GraphNode(graph, SWT.NONE);

        // Build note text
        String noteText = buildStickyNoteText(note);
        noteNode.setText(noteText);

        // Set sticky note color based on active status
        Color bgColor;
        if (!note.isActive()) {
            bgColor = new Color(display, 200, 200, 200); // Gray for inactive
        } else {
            // Classic sticky note yellow
            bgColor = new Color(display, 255, 253, 150); // Bright yellow
        }
        noteNode.setBackgroundColor(bgColor);

        // Set border to simulate sticky note shadow/edge
        noteNode.setBorderColor(new Color(display, 230, 220, 100)); // Darker yellow border
        noteNode.setBorderWidth(3);

        // Set tooltip with full details
        String tooltip = buildNoteTooltip(note);
        noteNode.setTooltip(new Label(tooltip));

        return noteNode;
    }

    /**
     * Build display text for sticky note style (professional, clean version)
     */
    private String buildStickyNoteText(Note note) {
        StringBuilder sb = new StringBuilder();

        // Professional header with timestamp
        sb.append("NOTE\n");
        sb.append(note.getFormattedDate()).append("\n");
        sb.append("════════\n");

        // Content (truncated to 30 chars for readability)
        String content = note.getContent();
        if (content.length() > 30) {
            content = content.substring(0, 27) + "...";
        }
        sb.append(content);

        sb.append("\n────────\n");

        // Author (compact)
        String author = note.getAuthor();
        if (author.contains(" ")) {
            author = author.split(" ")[0];
        }
        if (author.length() > 10) {
            author = author.substring(0, 10);
        }
        sb.append("By: ").append(author);

        // Replies indicator (professional, no icon)
        if (note.getReplies() != null && note.getReplies().size() > 0) {
            sb.append("\nReplies: ").append(note.getReplies().size());
        }

        return sb.toString();
    }


    /**
     * Build tooltip for note
     */
    private String buildNoteTooltip(Note note) {
        StringBuilder sb = new StringBuilder();

        sb.append("Note by ").append(note.getAuthor()).append("\n");
        sb.append("Created: ").append(note.getFormattedDate()).append("\n");
        sb.append("Status: ").append(note.isActive() ? "Active" : "Inactive").append("\n\n");

        sb.append("Content:\n").append(note.getContent()).append("\n");

        if (note.getReplies() != null && !note.getReplies().isEmpty()) {
            sb.append("\n").append(note.getReplies().size()).append(" Replies:\n");
            for (int i = 0; i < Math.min(3, note.getReplies().size()); i++) {
                NoteReply reply = note.getReplies().get(i);
                sb.append("  • ").append(reply.getAuthor()).append(": ");
                String replyContent = reply.getContent();
                if (replyContent.length() > 40) {
                    replyContent = replyContent.substring(0, 37) + "...";
                }
                sb.append(replyContent).append("\n");
            }
            if (note.getReplies().size() > 3) {
                sb.append("  ... and ").append(note.getReplies().size() - 3).append(" more");
            }
        }

        return sb.toString();
    }

    /**
     * Get map of created nodes (for testing)
     */
    public Map<String, GraphNode> getNodeMap() {
        return nodeMap;
    }

    /**
     * Get the Note object associated with a GraphNode
     */
    public Note getNoteForGraphNode(GraphNode node) {
        return graphNodeToNoteMap.get(node);
    }

    /**
     * Check if a GraphNode is a note
     */
    public boolean isNoteNode(GraphNode node) {
        return graphNodeToNoteMap.containsKey(node);
    }

    /**
     * Get the class ID for a GraphNode
     */
    public String getClassIdForGraphNode(GraphNode node) {
        return graphNodeToClassIdMap.get(node);
    }

    /**
     * Reposition all connection notes after layout has been applied
     * This ensures notes stay near their associated association lines
     * even after the layout algorithm moves the class nodes
     */
    public void repositionConnectionNotes() {
        // Iterate through all connection notes and reposition them
        for (Map.Entry<GraphNode, GraphConnection> entry : connectionNoteMap.entrySet()) {
            GraphNode noteNode = entry.getKey();
            GraphConnection connection = entry.getValue();
            Integer noteIndex = connectionNoteIndexMap.get(noteNode);

            // Only reposition if the connection still exists and index is found
            if (connection != null && noteIndex != null) {
                // Use the same positioning logic as initial creation
                positionNoteNearConnection(noteNode, connection, noteIndex);
            }
        }

        // Trigger a redraw of the custom connection lines layer
        if (connectionLinesLayer != null) {
            connectionLinesLayer.repaint();
        }

        // Also redraw the graph to ensure everything is refreshed
        graph.redraw();
    }

    /**
     * Add a listener to a connection note that repaints the connection lines
     * when the note is moved (e.g., by user dragging it)
     */
    private void addConnectionNoteListener(GraphNode noteNode) {
        // Get the note's connection to also listen to endpoint movements
        GraphConnection connection = connectionNoteMap.get(noteNode);

        // Add listener to the note itself
        org.eclipse.draw2d.IFigure noteFigure = noteNode.getNodeFigure();
        if (noteFigure != null) {
            noteFigure.addFigureListener(new org.eclipse.draw2d.FigureListener() {
                @Override
                public void figureMoved(org.eclipse.draw2d.IFigure source) {
                    // When the note moves, repaint the connection lines layer
                    if (connectionLinesLayer != null) {
                        connectionLinesLayer.repaint();
                    }
                }
            });
        }

        // Also add listeners to the association's source and destination nodes
        // so the line updates when the classes move
        if (connection != null) {
            GraphNode source = (GraphNode) connection.getSource();
            GraphNode dest = (GraphNode) connection.getDestination();

            if (source != null && source.getNodeFigure() != null) {
                source.getNodeFigure().addFigureListener(new org.eclipse.draw2d.FigureListener() {
                    @Override
                    public void figureMoved(org.eclipse.draw2d.IFigure src) {
                        if (connectionLinesLayer != null) {
                            connectionLinesLayer.repaint();
                        }
                    }
                });
            }

            if (dest != null && dest.getNodeFigure() != null) {
                dest.getNodeFigure().addFigureListener(new org.eclipse.draw2d.FigureListener() {
                    @Override
                    public void figureMoved(org.eclipse.draw2d.IFigure src) {
                        if (connectionLinesLayer != null) {
                            connectionLinesLayer.repaint();
                        }
                    }
                });
            }
        }
    }
}
package views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.GridLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalTreeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import core.model.ClassNode;
import core.model.IRBuilder;
import core.model.IntermediateRepresentation;
import core.model.Note;
import core.model.NoteReply;
import core.parser.AssociationDetector;
import core.parser.ClassInfo;
import core.parser.InheritanceDetector;
import core.parser.JavaClassParser;
import core.persistence.NoteStorage;
import core.visualization.ClassDiagramGenerator;

/**
 * Main UML Diagram View - With Notes System
 */

// Whole UML Diagram generator here. Every one please make sure you read it once(for those who didn't get a chance to work on this file).

public class UMLDiagramView extends ViewPart {
    
    public static final String ID = "views.UMLDiagramView";
    
    private Graph graph;
    private ClassDiagramGenerator diagramGenerator;
    private IntermediateRepresentation ir;
    private int currentLayoutMode = 3; // 0=Spring, 1=Tree, 2=Horizontal Tree, 3=Grid
    private boolean showNotes = true; // Toggle for showing/hiding notes
    private org.eclipse.swt.graphics.Color whiteBackground; // White background color (override dark mode)
    private double zoomLevel = 1.0; // Current zoom level (1.0 = 100%)

    private List<Note> notes;
    private NoteStorage noteStorage;
    private String currentUser;
    private Map<String, java.util.Set<String>> externalDependencies; // Store external dependencies for refresh
    private IJavaProject currentJavaProject; // Current Java project for note storage
    
    private Action generateAction;
    private Action refreshAction;
    private Action toggleLayoutAction;
    private Action zoomInAction;
    private Action zoomOutAction;
    private Action addNoteAction;
    private Action toggleNotesAction; // Toggle show/hide notes
    private Action deleteNoteAction;
    private Action exportPNGAction;
    private Action exportPNGWithoutNotesAction;
    private Action replyToNoteAction;
    
    @Override
    public void createPartControl(Composite parent) {
        // Initialize notes system
        notes = new ArrayList<>();
        currentUser = System.getProperty("user.name");
        // Note: noteStorage will be initialized when project is loaded

        // Create white color for background (override dark mode)
        whiteBackground = new org.eclipse.swt.graphics.Color(parent.getDisplay(), 255, 255, 255);

        // Force white background on parent composite (override dark mode)
        parent.setBackground(whiteBackground);

        // Create Zest graph with scrollbars for navigation
        graph = new Graph(parent, SWT.V_SCROLL | SWT.H_SCROLL);

        // Set white background on graph (override dark mode)
        graph.setBackground(whiteBackground);

        // Force white background on graph's internal composite
        if (graph.getParent() != null) {
            graph.getParent().setBackground(whiteBackground);
        }

        // Force white background on Draw2D layers (critical for dark mode)
        setGraphBackgroundWhite();

        // Create diagram generator
        diagramGenerator = new ClassDiagramGenerator(graph, parent.getDisplay());

        // Apply initial layout
        applyLayout();
        
        // Add selection listener
        graph.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                updateActionStates();
            }
        });

        // Add double-click listener for note interaction
        graph.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleNoteClick();
            }
        });

        // Create actions
        makeActions();
        contributeToActionBars();
    }
    
    /**
     * Generate diagram from workspace projects
     */
    private void generateDiagram() {
        try {
            IJavaProject javaProject = getFirstJavaProject();
            if (javaProject == null) {
                MessageDialog.openInformation(getSite().getShell(),
                    "No Project",
                    "No Java projects found in workspace.");
                return;
            }

            // Initialize or update note storage with current project
            if (currentJavaProject == null || !currentJavaProject.equals(javaProject)) {
                currentJavaProject = javaProject;
                noteStorage = new NoteStorage(javaProject.getProject());
                loadNotes();
            }
            
            List<ClassInfo> parsedClasses = parseProject(javaProject);
            
            if (parsedClasses.isEmpty()) {
                MessageDialog.openInformation(getSite().getShell(),
                    "No Classes",
                    "No Java classes found in project: " + javaProject.getElementName());
                return;
            }
            
            IRBuilder builder = new IRBuilder();
            ir = builder.build(parsedClasses);
            
            List<IType> types = getAllTypes(javaProject);
            InheritanceDetector inheritanceDetector = new InheritanceDetector(ir);
            inheritanceDetector.detectInheritance(types);
            
            List<ClassNode> projectClasses = new ArrayList<>(ir.getAllClasses().values());
            AssociationDetector associationDetector = new AssociationDetector(ir, projectClasses);
            associationDetector.detectAssociations(types);

            // Detect external dependencies (imports)
            core.parser.ExternalDependencyDetector dependencyDetector = new core.parser.ExternalDependencyDetector();
            externalDependencies = dependencyDetector.detectExternalDependencies(parsedClasses);

            // Generate diagram with notes and external dependencies
            diagramGenerator.generateDiagram(ir, showNotes ? notes : null, externalDependencies);
            applyLayout();

            // Ensure white background is maintained (override dark mode)
            graph.setBackground(whiteBackground);
            setGraphBackgroundWhite(); // Also set Draw2D layers to white

            // Show success message with notes and dependencies info
            String message = String.format("Generated diagram with %d classes and %d relations.",
                ir.getAllClasses().size(),
                ir.getAllRelations().size());

            if (externalDependencies != null && !externalDependencies.isEmpty()) {
                message += String.format("\n%d external package(s) detected.", externalDependencies.size());
            }

            if (notes.isEmpty()) {
                message += "\n\nNo notes found. Select a class and click 'Add Note' to create one!";
            } else {
                message += String.format("\n\n%d note(s) loaded.", notes.size());
            }

            MessageDialog.openInformation(getSite().getShell(),
                "Diagram Generated",
                message);
            
        } catch (Exception e) {
            MessageDialog.openError(getSite().getShell(),
                "Error",
                "Failed to generate diagram: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add note to selected element
     */
    private void addNote() {
        List<?> selection = graph.getSelection();
        if (selection.isEmpty()) {
            MessageDialog.openInformation(getSite().getShell(),
                "No Selection",
                "Please select a class or connection to add a note.");
            return;
        }
        
        Object selected = selection.get(0);
        String elementId = getElementId(selected);
        
        if (elementId == null) {
            return;
        }
        
        NoteDialog dialog = new NoteDialog(getSite().getShell());
        if (dialog.open() == Window.OK) {
            Note note = new Note(dialog.getContent(), dialog.getAuthor(), elementId);
            notes.add(note);
            saveNotes();

            // Add the note to diagram without full regeneration
            diagramGenerator.addSingleNote(note, elementId);

            MessageDialog.openInformation(getSite().getShell(),
                "Note Added",
                "Sticky note created and visible on diagram!");
        }
    }
    
    /**
     * Reply to selected note
     */
    private void replyToNote() {
        List<?> selection = graph.getSelection();
        if (selection.isEmpty()) {
            MessageDialog.openInformation(getSite().getShell(),
                "No Selection",
                "Please select a sticky note.");
            return;
        }

        Object selected = selection.get(0);
        if (!(selected instanceof GraphNode)) {
            return;
        }

        GraphNode selectedNode = (GraphNode) selected;

        // Check if this is a note node
        if (!diagramGenerator.isNoteNode(selectedNode)) {
            MessageDialog.openInformation(getSite().getShell(),
                "Not a Note",
                "Please select a sticky note to reply to.");
            return;
        }

        // Get the specific note
        Note note = diagramGenerator.getNoteForGraphNode(selectedNode);
        if (note == null) {
            MessageDialog.openError(getSite().getShell(),
                "Error",
                "Could not find the selected note.");
            return;
        }

        // Add reply to the specific note
        addReplyToNote(note);
    }
    
    /**
     * View notes for selected element
     */
    /**
 * View notes for selected element
 */
private void viewNotes() {
    List<?> selection = graph.getSelection();
    if (selection.isEmpty()) {
        MessageDialog.openInformation(getSite().getShell(),
            "No Selection",
            "Please select an element to view its notes.");
        return;
    }
    
    Object selected = selection.get(0);
    String elementId = getElementId(selected);
    
    if (elementId == null) {
        return;
    }
    
    List<Note> elementNotes = getNotesForElement(elementId);
    
    if (elementNotes.isEmpty()) {
        MessageDialog.openInformation(getSite().getShell(),
            "No Notes",
            "No notes found for this element.");
        return;
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append("Notes for: ").append(elementId).append("\n\n");
    for (int i = 0; i < elementNotes.size(); i++) {
        Note note = elementNotes.get(i);
        sb.append("=".repeat(50)).append("\n");
        sb.append("Note ").append(i + 1).append(":\n");
        sb.append(note.toString()).append("\n");
        
        if (note.getAuthor().equals(currentUser)) {
            sb.append("   (You can delete this)\n");
        }
        
        // Show replies
        if (note.getReplies() != null && !note.getReplies().isEmpty()) {
            sb.append("\n  Replies:\n");
            for (NoteReply reply : note.getReplies()) {
                sb.append("    - ").append(reply.toString()).append("\n");
            }
        }
        sb.append("\n");
    }
    
    MessageDialog.openInformation(getSite().getShell(),
        "Notes",
        sb.toString());
}
    
    /**
     * Delete selected note (owner only)
     */
    private void deleteNote() {
        List<?> selection = graph.getSelection();
        if (selection.isEmpty()) {
            MessageDialog.openInformation(getSite().getShell(),
                "No Selection",
                "Please select a sticky note.");
            return;
        }

        Object selected = selection.get(0);
        if (!(selected instanceof GraphNode)) {
            return;
        }

        GraphNode selectedNode = (GraphNode) selected;

        // Check if this is a note node
        if (!diagramGenerator.isNoteNode(selectedNode)) {
            MessageDialog.openInformation(getSite().getShell(),
                "Not a Note",
                "Please select a sticky note to delete.");
            return;
        }

        // Get the specific note
        Note note = diagramGenerator.getNoteForGraphNode(selectedNode);
        if (note == null) {
            MessageDialog.openError(getSite().getShell(),
                "Error",
                "Could not find the selected note.");
            return;
        }

        // Check ownership
        if (!note.getAuthor().equals(currentUser)) {
            MessageDialog.openError(getSite().getShell(),
                "Not Authorized",
                "You can only delete your own notes.");
            return;
        }

        // Delete the specific note
        deleteNoteConfirm(note);
    }
    
    /**
     * Export diagram as PNG
     */
    private void exportDiagramAsPNG(boolean includeNotes) {
        if (graph == null || graph.isDisposed()) {
            MessageDialog.openError(getSite().getShell(),
                "Export Error",
                "No diagram to export.");
            return;
        }

        // File dialog for save location
        org.eclipse.swt.widgets.FileDialog dialog =
            new org.eclipse.swt.widgets.FileDialog(getSite().getShell(), SWT.SAVE);
        dialog.setFilterNames(new String[] { "PNG Images (*.png)" });
        dialog.setFilterExtensions(new String[] { "*.png" });
        dialog.setFileName("uml_diagram.png");

        String filePath = dialog.open();
        if (filePath == null) {
            return; // User cancelled
        }

        // Ensure .png extension
        if (!filePath.toLowerCase().endsWith(".png")) {
            filePath += ".png";
        }

        // Save current note visibility state
        boolean originalShowNotes = showNotes;
        boolean needsRestore = false;

        try {
            // Temporarily adjust note visibility to match export preference
            if (includeNotes && !showNotes) {
                // Export WITH notes, but notes are currently hidden - show them temporarily
                diagramGenerator.toggleNoteVisibility(true);
                graph.redraw();
                graph.getDisplay().update();
                needsRestore = true;
            } else if (!includeNotes && showNotes) {
                // Export WITHOUT notes, but notes are currently shown - hide them temporarily
                diagramGenerator.toggleNoteVisibility(false);
                graph.redraw();
                graph.getDisplay().update();
                needsRestore = true;
            }

            core.export.DiagramExporter exporter = new core.export.DiagramExporter();
            exporter.exportToPNG(graph, filePath);

            MessageDialog.openInformation(getSite().getShell(),
                "Export Successful",
                "Diagram exported to:\n" + filePath);

        } catch (Exception e) {
            MessageDialog.openError(getSite().getShell(),
                "Export Error",
                "Failed to export diagram: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Restore original note visibility state if we changed it
            if (needsRestore) {
                diagramGenerator.toggleNoteVisibility(originalShowNotes);
                graph.redraw();
            }
        }
    }
    
    /**
     * Get element ID from selection
     */
    private String getElementId(Object element) {
        if (element instanceof GraphNode) {
            GraphNode node = (GraphNode) element;
            // Use the actual class ID from the diagram generator
            String classId = diagramGenerator.getClassIdForGraphNode(node);
            if (classId != null) {
                return classId;
            }
            // Fallback to text for backward compatibility
            return node.getText();
        } else if (element instanceof GraphConnection) {
            GraphConnection conn = (GraphConnection) element;
            // Extract only the class names (first line) to create clean connection ID
            String sourceName = extractClassName(conn.getSource().getText());
            String destName = extractClassName(conn.getDestination().getText());
            return sourceName + "->" + destName;
        }
        return null;
    }

    /**
     * Extract class name from node text (first line only)
     */
    private String extractClassName(String nodeText) {
        if (nodeText == null || nodeText.isEmpty()) {
            return "";
        }
        // Get first line only (class name)
        String[] lines = nodeText.split("\n");
        return lines[0].trim();
    }
    
    /**
     * Get notes for an element
     */
    private List<Note> getNotesForElement(String elementId) {
        List<Note> result = new ArrayList<>();
        for (Note note : notes) {
            if (note.getLinkedElementId().equals(elementId)) {
                result.add(note);
            }
        }
        return result;
    }
    
    /**
     * Load notes from storage
     */
    private void loadNotes() {
        if (noteStorage == null) {
            notes = new ArrayList<>();
            return;
        }
        try {
            notes = noteStorage.loadNotes();
        } catch (IOException e) {
            notes = new ArrayList<>();
        }
    }

    /**
     * Save notes to storage
     */
    private void saveNotes() {
        if (noteStorage == null) {
            MessageDialog.openWarning(getSite().getShell(),
                "Save Warning",
                "Cannot save notes: No project loaded. Please generate a diagram first.");
            return;
        }
        try {
            noteStorage.saveNotes(notes);
        } catch (IOException e) {
            MessageDialog.openError(getSite().getShell(),
                "Save Error",
                "Failed to save notes: " + e.getMessage());
        }
    }
    
    private IJavaProject getFirstJavaProject() throws CoreException {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                return JavaCore.create(project);
            }
        }
        return null;
    }
    
    private List<ClassInfo> parseProject(IJavaProject javaProject) throws Exception {
        List<ClassInfo> allClasses = new ArrayList<>();
        JavaClassParser parser = new JavaClassParser();

        for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
            if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                for (org.eclipse.jdt.core.IJavaElement element : root.getChildren()) {
                    if (element instanceof IPackageFragment) {
                        IPackageFragment packageFragment = (IPackageFragment) element;

                        // Skip test packages
                        if (isTestPackage(packageFragment)) {
                            continue;
                        }

                        for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
                            // Skip test files
                            if (isTestFile(unit)) {
                                continue;
                            }

                            List<ClassInfo> classes = parser.parse(unit);
                            // Filter out test classes
                            for (ClassInfo classInfo : classes) {
                                if (!isTestClass(classInfo)) {
                                    allClasses.add(classInfo);
                                }
                            }
                        }
                    }
                }
            }
        }
        return allClasses;
    }
    
    private List<IType> getAllTypes(IJavaProject javaProject) throws Exception {
        List<IType> types = new ArrayList<>();
        for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
            if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                for (org.eclipse.jdt.core.IJavaElement element : root.getChildren()) {
                    if (element instanceof IPackageFragment) {
                        IPackageFragment packageFragment = (IPackageFragment) element;

                        // Skip test packages
                        if (isTestPackage(packageFragment)) {
                            continue;
                        }

                        for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
                            // Skip test files
                            if (isTestFile(unit)) {
                                continue;
                            }

                            for (IType type : unit.getAllTypes()) {
                                // Skip test classes
                                if (!isTestType(type)) {
                                    types.add(type);
                                }
                            }
                        }
                    }
                }
            }
        }
        return types;
    }
    
    private void applyLayout() {
        // Calculate moderate canvas size based on number of nodes
        int nodeCount = graph.getNodes().size();
        int baseWidth = 2800;   // Slightly increased from 2400
        int baseHeight = 2100;  // Slightly increased from 1800

        // More reasonable scaling for larger projects (prevents overlapping without excessive space)
        // Use square root scaling to grow more slowly as node count increases
        int scaleFactor = (int) Math.max(1, Math.sqrt(nodeCount / 5.0));
        int scaledWidth = baseWidth + (scaleFactor * 400);   // Moderate scaling
        int scaledHeight = baseHeight + (scaleFactor * 300); // Moderate scaling

        switch (currentLayoutMode) {
            case 1: // Tree Layout (Vertical)
                TreeLayoutAlgorithm treeLayout = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
                graph.setPreferredSize(scaledWidth, scaledHeight);
                graph.setLayoutAlgorithm(treeLayout, true);
                break;

            case 2: // Horizontal Tree Layout
                HorizontalTreeLayoutAlgorithm hTreeLayout = new HorizontalTreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
                graph.setPreferredSize(scaledWidth, scaledHeight);
                graph.setLayoutAlgorithm(hTreeLayout, true);
                break;

            case 3: // Grid Layout (Best for no overlaps!)
                GridLayoutAlgorithm gridLayout = new GridLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
                graph.setPreferredSize(scaledWidth, scaledHeight);
                graph.setLayoutAlgorithm(gridLayout, true);
                break;

            case 0: // Spring Layout (Default)
            default:
                // Spring layout with balanced spacing
                SpringLayoutAlgorithm springLayout = new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);

                // Balanced parameters for good separation without excessive space
                springLayout.setSpringLength(200);         // Balanced spacing
                springLayout.setIterations(3500);          // Good convergence
                springLayout.setRandom(false);             // Deterministic layout
                springLayout.setSpringMove(9);             // Good separation
                springLayout.setSpringStrain(0.65);        // Balanced spread

                // Set moderately sized canvas
                graph.setPreferredSize(scaledWidth, scaledHeight);

                graph.setLayoutAlgorithm(springLayout, true);
                break;
        }

        // After layout is applied, reposition connection notes to stay near their association lines
        diagramGenerator.repositionConnectionNotes();
    }

    private void refreshDiagram() {
        if (ir != null) {
            diagramGenerator.generateDiagram(ir, showNotes ? notes : null, externalDependencies);
            applyLayout();

            // Enable zoom actions after diagram is generated
            updateZoomActionStates();

            // Ensure white background is maintained after refresh
            graph.setBackground(whiteBackground);
            setGraphBackgroundWhite(); // Also set Draw2D layers to white
        }
    }

    /**
     * Toggle notes visibility without regenerating the entire diagram
     */
    private void toggleNotesVisibility() {
        showNotes = !showNotes;
        toggleNotesAction.setText(showNotes ? "Hide Notes" : "Show Notes");
        toggleNotesAction.setToolTipText(showNotes ? "Hide sticky notes from diagram" : "Show sticky notes on diagram");

        // Just toggle visibility of note nodes - don't regenerate entire diagram!
        diagramGenerator.toggleNoteVisibility(showNotes);
    }
    
    private void toggleLayout() {
        currentLayoutMode = (currentLayoutMode + 1) % 4; // Cycle through 0, 1, 2, 3
        applyLayout();

        // Update button text based on current mode
        String[] layoutNames = {"Spring Layout", "Tree Layout", "Horizontal Tree", "Grid Layout"};
        String nextLayoutName = layoutNames[(currentLayoutMode + 1) % 4];
        toggleLayoutAction.setText("Switch to " + nextLayoutName);
        toggleLayoutAction.setToolTipText("Current: " + layoutNames[currentLayoutMode] + ". Click to switch.");
    }


    /**
     * Zoom in to the diagram
     */
    private void zoomIn() {
        if (zoomLevel < 3.0) { // Max zoom 300%
            zoomLevel += 0.25;
            applyZoom();
            updateZoomActionStates();
        }
    }

    /**
     * Zoom out from the diagram
     */
    private void zoomOut() {
        if (zoomLevel > 0.1) { // Min zoom 10% (reduced from 25% for better zoom out)
            zoomLevel -= 0.25;
            applyZoom();
            updateZoomActionStates();
        }
    }

    /**
     * Reset zoom to 100%
     */
    private void zoomReset() {
        zoomLevel = 1.0;
        applyZoom();
    }

    /**
     * Apply the current zoom level to the graph
     */
    private void applyZoom() {
        if (graph != null && !graph.isDisposed()) {
            try {
                // Get the root figure and apply scaling using ScalableFigure if available
                org.eclipse.draw2d.IFigure rootFigure = graph.getRootLayer();

                if (rootFigure instanceof org.eclipse.draw2d.ScalableFigure) {
                    ((org.eclipse.draw2d.ScalableFigure) rootFigure).setScale(zoomLevel);
                    graph.redraw();
                } else {
                    // Alternative: Adjust graph size as a zoom effect
                    int baseWidth = 2400;
                    int baseHeight = 1800;
                    int newWidth = (int) (baseWidth * zoomLevel);
                    int newHeight = (int) (baseHeight * zoomLevel);
                    graph.setPreferredSize(newWidth, newHeight);

                    // Only redraw, don't reapply layout (prevents layout reset)
                    graph.redraw();
                }

            } catch (Exception e) {
                System.err.println("Zoom error: " + e.getMessage());
            }

            // Update zoom action tooltips
            updateZoomActionTooltips();
        }
    }

    /**
     * Update zoom action tooltips with current zoom level
     */
    private void updateZoomActionTooltips() {
        int zoomPercent = (int) (zoomLevel * 100);
        if (zoomInAction != null) {
            zoomInAction.setToolTipText("Zoom In (Current: " + zoomPercent + "%)");
        }
        if (zoomOutAction != null) {
            zoomOutAction.setToolTipText("Zoom Out (Current: " + zoomPercent + "%)");
        }
    }

    /**
     * Update zoom action enabled/disabled states based on current zoom level
     */
    private void updateZoomActionStates() {
        if (zoomInAction != null) {
            zoomInAction.setEnabled(zoomLevel < 3.0);
        }
        if (zoomOutAction != null) {
            zoomOutAction.setEnabled(zoomLevel > 0.1);
        }
    }
    
    /**
     * Handle note click - show options menu
     */
    private void handleNoteClick() {
        List<?> selection = graph.getSelection();
        if (selection.isEmpty()) {
            return;
        }

        Object selected = selection.get(0);
        if (!(selected instanceof GraphNode)) {
            return;
        }

        GraphNode selectedNode = (GraphNode) selected;

        // Check if this is a note node
        if (diagramGenerator.isNoteNode(selectedNode)) {
            Note note = diagramGenerator.getNoteForGraphNode(selectedNode);
            if (note != null) {
                showNoteOptionsDialog(note);
            }
        }
    }

    /**
     * Show options dialog for a note
     */
    private void showNoteOptionsDialog(Note note) {
        // Create options based on ownership
        boolean isOwner = note.getAuthor().equals(currentUser);

        StringBuilder message = new StringBuilder();
        message.append("Note by: ").append(note.getAuthor()).append("\n");
        message.append("Created: ").append(note.getFormattedDate()).append("\n");
        message.append("Status: ").append(note.isActive() ? "Active" : "Inactive").append("\n\n");
        message.append("Content:\n").append(note.getContent()).append("\n\n");

        if (note.getReplies() != null && !note.getReplies().isEmpty()) {
            message.append("Replies (").append(note.getReplies().size()).append("):\n");
            for (NoteReply reply : note.getReplies()) {
                message.append("  • ").append(reply.getAuthor()).append(": ");
                message.append(reply.getContent()).append("\n");
                message.append("    (").append(reply.getFormattedDate()).append(")\n");
            }
        } else {
            message.append("No replies yet.\n");
        }

        message.append("\n─────────────────────\n");
        message.append("What would you like to do?");

        // Create buttons array
        String[] buttons;
        if (isOwner) {
            buttons = new String[]{"View Replies", "Add Reply", "Delete Note", "Close"};
        } else {
            buttons = new String[]{"View Replies", "Add Reply", "Close"};
        }

        org.eclipse.jface.dialogs.MessageDialog dialog =
            new org.eclipse.jface.dialogs.MessageDialog(
                getSite().getShell(),
                "Sticky Note",
                null,
                message.toString(),
                org.eclipse.jface.dialogs.MessageDialog.QUESTION,
                buttons,
                0
            );

        int result = dialog.open();

        // Handle button clicks
        if (isOwner) {
            if (result == 0) {
                // View Replies
                showRepliesDialog(note);
            } else if (result == 1) {
                // Add Reply
                addReplyToNote(note);
            } else if (result == 2) {
                // Delete Note
                deleteNoteConfirm(note);
            }
        } else {
            if (result == 0) {
                // View Replies
                showRepliesDialog(note);
            } else if (result == 1) {
                // Add Reply
                addReplyToNote(note);
            }
        }
    }

    /**
     * Show detailed replies dialog
     */
    private void showRepliesDialog(Note note) {
        StringBuilder sb = new StringBuilder();
        sb.append("Replies for note: ").append(note.getContent().substring(0, Math.min(30, note.getContent().length())));
        if (note.getContent().length() > 30) {
            sb.append("...");
        }
        sb.append("\n\n");

        if (note.getReplies() == null || note.getReplies().isEmpty()) {
            sb.append("No replies yet.");
        } else {
            for (int i = 0; i < note.getReplies().size(); i++) {
                NoteReply reply = note.getReplies().get(i);
                sb.append((i + 1)).append(". ");
                sb.append(reply.getAuthor()).append(" (").append(reply.getFormattedDate()).append("):\n");
                sb.append("   ").append(reply.getContent()).append("\n\n");
            }
        }

        MessageDialog.openInformation(getSite().getShell(),
            "Note Replies",
            sb.toString());
    }

    /**
     * Add reply to a note
     */
    private void addReplyToNote(Note note) {
        org.eclipse.jface.dialogs.InputDialog dialog =
            new org.eclipse.jface.dialogs.InputDialog(
                getSite().getShell(),
                "Add Reply",
                "Enter your reply:",
                "",
                null
            );

        if (dialog.open() == Window.OK) {
            String replyContent = dialog.getValue();
            if (replyContent != null && !replyContent.trim().isEmpty()) {
                NoteReply reply = new NoteReply(replyContent, currentUser);
                note.addReply(reply);
                saveNotes();

                MessageDialog.openInformation(getSite().getShell(),
                    "Reply Added",
                    "Your reply has been added to the note!");

                // Update note display without full regeneration
                diagramGenerator.updateNoteDisplay(note);
            }
        }
    }

    /**
     * Delete note with confirmation
     */
    private void deleteNoteConfirm(Note note) {
        boolean confirm = MessageDialog.openConfirm(getSite().getShell(),
            "Delete Note",
            "Are you sure you want to delete this note?\n\n" +
            "Content: " + note.getContent().substring(0, Math.min(50, note.getContent().length())) +
            (note.getContent().length() > 50 ? "..." : ""));

        if (confirm) {
            // Completely remove the note from the list
            notes.remove(note);
            saveNotes();

            MessageDialog.openInformation(getSite().getShell(),
                "Note Deleted",
                "The note has been permanently deleted.");

            // Remove note from diagram without full regeneration
            diagramGenerator.removeSingleNote(note);
        }
    }

    private void updateActionStates() {
        List<?> selection = graph.getSelection();
        boolean hasSelection = !selection.isEmpty();

        // Add note is enabled when selecting a class (not a note) OR an association line
        boolean isClassSelected = false;
        boolean isConnectionSelected = false;
        boolean isNoteSelected = false;

        if (hasSelection) {
            Object selected = selection.get(0);

            if (selected instanceof GraphNode) {
                GraphNode node = (GraphNode) selected;
                isNoteSelected = diagramGenerator.isNoteNode(node);
                isClassSelected = !isNoteSelected && diagramGenerator.getClassIdForGraphNode(node) != null;
            } else if (selected instanceof GraphConnection) {
                // Association line selected
                isConnectionSelected = true;
            }
        }

        // Enable "Add Note" for both classes and association lines
        addNoteAction.setEnabled(isClassSelected || isConnectionSelected);
        deleteNoteAction.setEnabled(isNoteSelected);
        replyToNoteAction.setEnabled(isNoteSelected);
    }
    
    private void makeActions() {
        generateAction = new Action() {
            public void run() { generateDiagram(); }
        };
        generateAction.setText("Generate Diagram");
        generateAction.setToolTipText("Generate UML diagram from Java project");
        
        refreshAction = new Action() {
            public void run() { refreshDiagram(); }
        };
        refreshAction.setText("Refresh");
        
        toggleLayoutAction = new Action() {
            public void run() { toggleLayout(); }
        };
        toggleLayoutAction.setText("Switch to Tree Layout");
        toggleLayoutAction.setToolTipText("Current: Spring Layout. Click to cycle through layouts (Spring → Tree → Horizontal → Grid)");

        zoomInAction = new Action() {
            public void run() { zoomIn(); }
        };
        zoomInAction.setText("Zoom In");
        zoomInAction.setToolTipText("Zoom In (Current: 100%)");

        zoomOutAction = new Action() {
            public void run() { zoomOut(); }
        };
        zoomOutAction.setText("Zoom Out");
        zoomOutAction.setToolTipText("Zoom Out (Current: 100%)");

        addNoteAction = new Action() {
            public void run() { addNote(); }
        };
        addNoteAction.setText("Add Note");
        addNoteAction.setToolTipText("Add note to selected element");
        addNoteAction.setEnabled(false);
        
        replyToNoteAction = new Action() {
            public void run() {
                replyToNote();
            }
        };
        replyToNoteAction.setText("Reply to Note");
        replyToNoteAction.setToolTipText("Reply to selected sticky note");
        replyToNoteAction.setEnabled(false);

        toggleNotesAction = new Action() {
            public void run() { toggleNotesVisibility(); }
        };
        toggleNotesAction.setText("Hide Notes");
        toggleNotesAction.setToolTipText("Hide sticky notes from diagram");

        deleteNoteAction = new Action() {
            public void run() { deleteNote(); }
        };
        deleteNoteAction.setText("Delete Note");
        deleteNoteAction.setToolTipText("Delete selected sticky note (owner only)");
        deleteNoteAction.setEnabled(false);
        exportPNGAction = new Action() {
            public void run() {
                exportDiagramAsPNG(true);
            }
        };
        exportPNGAction.setText("Export PNG");
        exportPNGAction.setToolTipText("Export diagram as PNG image");

        exportPNGWithoutNotesAction = new Action() {
            public void run() {
                exportDiagramAsPNG(false);
            }
        };
        exportPNGWithoutNotesAction.setText("Export PNG (No Notes)");
        exportPNGWithoutNotesAction.setToolTipText("Export diagram as PNG without notes");
    }
    
    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }
    
    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(generateAction);
        manager.add(new Separator());
        manager.add(toggleNotesAction);
        manager.add(new Separator());
        manager.add(addNoteAction);
        manager.add(replyToNoteAction);
        manager.add(deleteNoteAction);
        manager.add(new Separator());
        manager.add(exportPNGAction);
        manager.add(exportPNGWithoutNotesAction);
        manager.add(new Separator());
        manager.add(refreshAction);
        manager.add(toggleLayoutAction);
        manager.add(new Separator());
        manager.add(zoomInAction);
        manager.add(zoomOutAction);
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(generateAction);
        manager.add(new Separator());
        manager.add(toggleNotesAction);
        manager.add(new Separator());
        manager.add(addNoteAction);
        manager.add(replyToNoteAction);
        manager.add(deleteNoteAction);
        manager.add(new Separator());
        manager.add(exportPNGAction);
        manager.add(exportPNGWithoutNotesAction);
        manager.add(new Separator());
        manager.add(toggleLayoutAction);
        manager.add(new Separator());
        manager.add(zoomInAction);
        manager.add(zoomOutAction);
    }

    /**
     * Force white background on all Draw2D layers
     * This ensures the diagram is white even in Eclipse dark mode
     */
    private void setGraphBackgroundWhite() {
        if (graph == null) return;

        try {
            // Get the root Draw2D figure
            org.eclipse.draw2d.IFigure rootFigure = graph.getRootLayer();
            if (rootFigure != null) {
                // Only set background on the root layer itself - NOT on children
                // Setting on children would make diagram elements invisible!
                rootFigure.setBackgroundColor(org.eclipse.draw2d.ColorConstants.white);
                rootFigure.setOpaque(true); // Make it opaque so the background shows
            }

            // Also try to set on the viewport if available
            if (graph.getContents() != null) {
                // Only set on container, not children
                graph.getContents().setBackgroundColor(org.eclipse.draw2d.ColorConstants.white);
                graph.getContents().setOpaque(true);
            }
        } catch (Exception e) {
            // If any issues accessing internal figures, just continue
            System.err.println("Could not set Draw2D background to white: " + e.getMessage());
        }
    }

    @Override
    public void setFocus() {
        if (graph != null && !graph.isDisposed()) {
            graph.setFocus();
        }
    }
    
    @Override
    public void dispose() {
        saveNotes();

        // Dispose white background color resource
        if (whiteBackground != null && !whiteBackground.isDisposed()) {
            whiteBackground.dispose();
        }

        super.dispose();
    }

    /**
     * Check if a package is a test package
     * Test packages contain "test" in their name or path
     */
    private boolean isTestPackage(IPackageFragment packageFragment) {
        String packageName = packageFragment.getElementName();
        // Check if package name contains "test" (case insensitive)
        return packageName.toLowerCase().contains("test");
    }

    /**
     * Check if a compilation unit is a test file
     * Test files typically end with "Test.java" or are in test directories
     */
    private boolean isTestFile(ICompilationUnit unit) {
        String fileName = unit.getElementName();
        // Check if file name ends with "Test.java" or "Tests.java"
        return fileName.endsWith("Test.java") || fileName.endsWith("Tests.java");
    }

    /**
     * Check if a ClassInfo represents a test class
     * Test classes have names ending with "Test" or package containing "test"
     */
    private boolean isTestClass(ClassInfo classInfo) {
        // Check class name
        if (classInfo.getName().endsWith("Test") || classInfo.getName().endsWith("Tests")) {
            return true;
        }
        // Check package name
        if (classInfo.getPackageName().toLowerCase().contains("test")) {
            return true;
        }
        return false;
    }

    /**
     * Check if an IType represents a test class
     * Test types have names ending with "Test" or are in test packages
     */
    private boolean isTestType(IType type) {
        String typeName = type.getElementName();
        // Check type name
        if (typeName.endsWith("Test") || typeName.endsWith("Tests")) {
            return true;
        }
        // Check if in test package
        IPackageFragment packageFragment = (IPackageFragment) type.getAncestor(org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT);
        if (packageFragment != null) {
            String packageName = packageFragment.getElementName();
            if (packageName.toLowerCase().contains("test")) {
                return true;
            }
        }
        return false;
    }
}
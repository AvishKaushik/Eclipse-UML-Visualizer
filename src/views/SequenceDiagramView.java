package views;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import core.model.SequenceDiagram;
import core.model.SequenceMessage;
import core.parser.MethodCallParser;
import core.visualization.SequenceDiagramRenderer;

/**
 * View for displaying sequence diagrams using PlantUML
 */

// Again, followed the documentation. Please follow the standards when making changes here. 
public class SequenceDiagramView extends ViewPart {

    public static final String ID = "views.SequenceDiagramView";

    private ScrolledComposite scrolledComposite;
    private Canvas canvas;
    private Image currentImage;
    private SequenceDiagram currentDiagram;
    private double zoomLevel = 1.0; // Current zoom level (1.0 = 100%)

    private Action generateProjectAction;
    private Action refreshAction;
    private Action exportPNGAction;
    private Action zoomInAction;
    private Action zoomOutAction;

    @Override
    public void createPartControl(Composite parent) {
        // Create scrolled composite
        scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        scrolledComposite.setLayout(new FillLayout());

        // Create canvas for drawing the image
        canvas = new Canvas(scrolledComposite, SWT.NONE);
        scrolledComposite.setContent(canvas);

        // Setup paint listener with zoom support
        canvas.addPaintListener(e -> {
            if (currentImage != null && !currentImage.isDisposed()) {
                // Calculate scaled dimensions
                int scaledWidth = (int) (currentImage.getBounds().width * zoomLevel);
                int scaledHeight = (int) (currentImage.getBounds().height * zoomLevel);

                // Draw scaled image
                e.gc.drawImage(currentImage,
                    0, 0, currentImage.getBounds().width, currentImage.getBounds().height,
                    0, 0, scaledWidth, scaledHeight);
            } else {
                e.gc.drawText("No diagram generated yet.\nClick 'Generate Project Diagram' to begin.", 10, 10);
            }
        });

        // Create actions
        makeActions();
        contributeToActionBars();
    }

    /**
     * Generate sequence diagram for the entire project
     */
    private void generateForProject() {
        try {
            IJavaProject javaProject = getFirstJavaProject();
            if (javaProject == null) {
                MessageDialog.openInformation(getSite().getShell(),
                    "No Project",
                    "No Java projects found in workspace.");
                return;
            }

            // Get all types in the project
            List<IType> allTypes = getAllTypes(javaProject);

            if (allTypes.isEmpty()) {
                MessageDialog.openInformation(getSite().getShell(),
                    "No Classes",
                    "No classes found in project.");
                return;
            }

            // Create a combined sequence diagram
            currentDiagram = new SequenceDiagram(javaProject.getElementName(), "Project Overview");
            MethodCallParser parser = new MethodCallParser();

            // Set whitelist of user classes (ONLY these will be included)
            parser.setUserClasses(allTypes);

            int totalMessages = 0;
            int processedClasses = 0;

            for (IType type : allTypes) {
                try {
                    SequenceDiagram classDiagram = parser.parseClass(type);

                    // Merge participants and messages into the main diagram
                    for (String participant : classDiagram.getParticipants()) {
                        currentDiagram.addParticipant(participant);
                    }

                    for (SequenceMessage message : classDiagram.getMessages()) {
                        currentDiagram.addMessage(message);
                        totalMessages++;
                    }

                    processedClasses++;

                } catch (Exception e) {
                    // Skip classes that cause errors
                }
            }

            if (currentDiagram.isEmpty()) {
                MessageDialog.openInformation(getSite().getShell(),
                    "Empty Diagram",
                    "No method calls found in the project.\n\nThe project may have only empty methods or interfaces.");
                return;
            }

            // HARD LIMIT: Refuse to render extremely large diagrams
            if (currentDiagram.getMessageCount() > 500 || currentDiagram.getParticipantCount() > 100) {
                MessageDialog.openError(getSite().getShell(),
                    "Diagram Too Large",
                    String.format("Cannot render this diagram - it exceeds memory limits:\n\n" +
                        "Current Size:\n" +
                        "  • %d participants (limit: 100)\n" +
                        "  • %d messages (limit: 500)\n\n" +
                        "Why this limit exists:\n" +
                        "  • Diagrams this large cause OutOfMemoryError\n" +
                        "  • The diagram would be unreadable anyway\n" +
                        "  • Sequence diagrams are meant for specific workflows\n\n" +
                        "Solution:\n" +
                        "  ✓ For project overview, use Class Diagram instead\n" +
                        "  ✓ Focus on specific interaction flows, not entire project",
                        currentDiagram.getParticipantCount(),
                        currentDiagram.getMessageCount()));
                return;
            }

            // Warn about moderately large diagrams
            if (currentDiagram.getMessageCount() > 50 || currentDiagram.getParticipantCount() > 20) {
                boolean proceed = MessageDialog.openQuestion(getSite().getShell(),
                    "Large Diagram Warning",
                    String.format("This diagram is large and may take time to render:\n\n" +
                        "  • %d participants\n" +
                        "  • %d messages\n\n" +
                        "Do you want to continue?",
                        currentDiagram.getParticipantCount(),
                        currentDiagram.getMessageCount()));

                if (!proceed) {
                    return;
                }
            }

            // Render the diagram
            renderDiagram();

            // Update view title
            setContentDescription(String.format("Sequence Diagram: %s (%d classes, %d messages, %d participants)",
                javaProject.getElementName(),
                processedClasses,
                totalMessages,
                currentDiagram.getParticipantCount()));

            MessageDialog.openInformation(getSite().getShell(),
                "Diagram Generated",
                String.format("Generated project-wide sequence diagram\n" +
                    "Project: %s\n" +
                    "Classes Processed: %d\n" +
                    "Participants: %d\n" +
                    "Messages: %d",
                    javaProject.getElementName(),
                    processedClasses,
                    currentDiagram.getParticipantCount(),
                    totalMessages));

        } catch (Exception e) {
            MessageDialog.openError(getSite().getShell(),
                "Generation Error",
                "Failed to generate project diagram: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Render the current diagram using PlantUML
     */
    private void renderDiagram() {
        try {
            // Dispose old image
            if (currentImage != null && !currentImage.isDisposed()) {
                currentImage.dispose();
            }

            // Render new image
            currentImage = SequenceDiagramRenderer.renderToImage(
                currentDiagram,
                getSite().getShell().getDisplay()
            );

            if (currentImage != null) {
                // Force layout update to ensure view is sized
                scrolledComposite.layout(true);

                // Apply initial zoom
                applyZoom();
            }
        } catch (OutOfMemoryError e) {
            MessageDialog.openError(getSite().getShell(),
                "Out of Memory",
                String.format("The sequence diagram is too large to render:\n" +
                    "- %d participants\n" +
                    "- %d messages\n\n" +
                    "The diagram exceeds available memory.\n\n" +
                    "Solutions:\n" +
                    "1. Increase Eclipse memory limit (-Xmx in eclipse.ini)\n" +
                    "2. Focus on smaller projects with fewer classes\n" +
                    "3. Use Class Diagram for project overview instead",
                    currentDiagram.getParticipantCount(),
                    currentDiagram.getMessageCount()));
            e.printStackTrace();
        }
    }

    /**
     * Apply current zoom level to the canvas
     */
    private void applyZoom() {
        if (currentImage != null && !currentImage.isDisposed()) {
            int scaledWidth = (int) (currentImage.getBounds().width * zoomLevel);
            int scaledHeight = (int) (currentImage.getBounds().height * zoomLevel);

            Point size = new Point(scaledWidth, scaledHeight);
            canvas.setSize(size);
            scrolledComposite.setMinSize(size);

            // Redraw
            canvas.redraw();

            // Update zoom action states
            updateZoomActionStates();
        }
    }

    /**
     * Zoom in
     */
    private void zoomIn() {
        if (zoomLevel < 3.0) { // Max 300%
            zoomLevel += 0.25;
            applyZoom();
        }
    }

    /**
     * Zoom out
     */
    private void zoomOut() {
        if (zoomLevel > 0.25) { // Min 25%
            zoomLevel -= 0.25;
            applyZoom();
        }
    }

    /**
     * Update zoom action enabled/disabled states
     */
    private void updateZoomActionStates() {
        if (zoomInAction != null) {
            zoomInAction.setEnabled(zoomLevel < 3.0);
            zoomInAction.setToolTipText("Zoom In (Current: " + (int)(zoomLevel * 100) + "%)");
        }
        if (zoomOutAction != null) {
            zoomOutAction.setEnabled(zoomLevel > 0.25);
            zoomOutAction.setToolTipText("Zoom Out (Current: " + (int)(zoomLevel * 100) + "%)");
        }
    }

    /**
     * Get all USER-CREATED types from project (excludes library/framework classes)
     * Only returns classes from the project's source code, not dependencies
     */
    private List<IType> getAllTypes(IJavaProject javaProject) throws Exception {
        List<IType> allTypes = new ArrayList<>();

        for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
            // Only process SOURCE folders (not binary/library JARs)
            if (root.getKind() != IPackageFragmentRoot.K_SOURCE) {
                continue;
            }

            for (org.eclipse.jdt.core.IJavaElement element : root.getChildren()) {
                if (element instanceof IPackageFragment) {
                    IPackageFragment packageFragment = (IPackageFragment) element;

                    // Skip test packages
                    if (isTestPackage(packageFragment)) {
                        continue;
                    }

                    // Skip library/framework packages (even if somehow in source)
                    if (isLibraryPackage(packageFragment)) {
                        continue;
                    }

                    for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
                        // Skip test files
                        if (isTestFile(unit)) {
                            continue;
                        }

                        for (IType type : unit.getAllTypes()) {
                            // Only include user-created classes (skip test and library classes)
                            if (!isTestType(type) && isUserCreatedClass(type)) {
                                allTypes.add(type);
                            }
                        }
                    }
                }
            }
        }

        return allTypes;
    }

    /**
     * Check if a package is from a library/framework (not user code)
     */
    private boolean isLibraryPackage(IPackageFragment packageFragment) {
        String packageName = packageFragment.getElementName();

        // Filter out common library/framework packages
        String[] libraryPrefixes = {
            "java.", "javax.",           // Java standard library
            "org.eclipse.",              // Eclipse framework
            "org.junit.", "junit.",      // JUnit
            "org.apache.",               // Apache libraries
            "org.springframework.",      // Spring
            "com.google.", "com.sun.",   // Google/Sun
            "org.hibernate.",            // Hibernate
            "org.w3c.", "org.xml.",      // XML standards
            "net.sf.",                   // SourceForge
            "org.slf4j.", "org.log4j.",  // Logging
            "com.fasterxml.jackson.",    // Jackson
            "org.json.",                 // JSON
            "net.sourceforge."           // SourceForge alt
        };

        for (String prefix : libraryPrefixes) {
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a type is user-created (not from libraries)
     * User classes are typically in project-specific packages
     */
    private boolean isUserCreatedClass(IType type) {
        try {
            // Binary types are from compiled libraries, not source
            if (type.isBinary()) {
                return false;
            }

            // Check if type has a resource (source file)
            // Library classes don't have resources in the workspace
            if (type.getResource() == null) {
                return false;
            }

            // Check package name - filter out library packages
            IPackageFragment packageFragment = (IPackageFragment) type.getAncestor(org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT);
            if (packageFragment != null && isLibraryPackage(packageFragment)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            // If we can't determine, assume it's not user-created (safe default)
            return false;
        }
    }

    /**
     * Export current diagram
     */
    private void exportDiagram() {
        if (currentDiagram == null) {
            MessageDialog.openError(getSite().getShell(),
                "Export Error",
                "No diagram to export. Please generate a diagram first.");
            return;
        }

        org.eclipse.swt.widgets.FileDialog dialog =
            new org.eclipse.swt.widgets.FileDialog(getSite().getShell(), SWT.SAVE);
        dialog.setFilterNames(new String[] { "PNG Images (*.png)" });
        dialog.setFilterExtensions(new String[] { "*.png" });

        String defaultFileName = currentDiagram.getClassName() + "_sequence_diagram.png";
        dialog.setFileName(defaultFileName);

        String filePath = dialog.open();
        if (filePath == null) {
            return;
        }

        if (!filePath.toLowerCase().endsWith(".png")) {
            filePath += ".png";
        }

        try {
            boolean success = SequenceDiagramRenderer.exportToPNG(currentDiagram, filePath);

            if (success) {
                MessageDialog.openInformation(getSite().getShell(),
                    "Export Successful",
                    "Sequence diagram exported to:\n" + filePath);
            } else {
                MessageDialog.openError(getSite().getShell(),
                    "Export Error",
                    "Failed to export diagram");
            }

        } catch (Exception e) {
            MessageDialog.openError(getSite().getShell(),
                "Export Error",
                "Failed to export diagram: " + e.getMessage());
            e.printStackTrace();
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

    /**
     * Refresh current diagram
     */
    private void refreshDiagram() {
        // Refresh project diagram
        generateForProject();
    }

    /**
     * Create actions
     */
    private void makeActions() {
        generateProjectAction = new Action() {
            public void run() {
                generateForProject();
            }
        };
        generateProjectAction.setText("Generate Project Diagram");
        generateProjectAction.setToolTipText("Generate sequence diagram for the entire project");

        refreshAction = new Action() {
            public void run() {
                refreshDiagram();
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Refresh current diagram");

        exportPNGAction = new Action() {
            public void run() {
                exportDiagram();
            }
        };
        exportPNGAction.setText("Export PNG");
        exportPNGAction.setToolTipText("Export diagram as PNG");

        // Zoom actions
        zoomInAction = new Action() {
            public void run() {
                zoomIn();
            }
        };
        zoomInAction.setText("Zoom In");
        zoomInAction.setToolTipText("Zoom In (Current: 100%)");

        zoomOutAction = new Action() {
            public void run() {
                zoomOut();
            }
        };
        zoomOutAction.setText("Zoom Out");
        zoomOutAction.setToolTipText("Zoom Out (Current: 100%)");
    }

    /**
     * Contribute to action bars
     */
    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(generateProjectAction);
        manager.add(new Separator());
        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(zoomInAction);
        manager.add(zoomOutAction);
        manager.add(new Separator());
        manager.add(exportPNGAction);
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(generateProjectAction);
        manager.add(new Separator());
        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(zoomInAction);
        manager.add(zoomOutAction);
        manager.add(new Separator());
        manager.add(exportPNGAction);
    }

    @Override
    public void setFocus() {
        if (canvas != null && !canvas.isDisposed()) {
            canvas.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (currentImage != null && !currentImage.isDisposed()) {
            currentImage.dispose();
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

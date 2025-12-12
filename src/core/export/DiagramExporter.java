package core.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.widgets.Graph;

/**
 * Exports Zest diagrams to PNG images
 * Export Functionality with and without notes
 */

/* NOTE - This supports exporting with notes and without notes so we
* don’t need two separate exporters. It may look simple now,
* but changing this later will probably break more than you expect :))
*/
public class DiagramExporter {
    
    /**
     * Export graph to PNG file
     */
    public void exportToPNG(Graph graph, String filePath) throws IOException {
        if (graph == null || graph.isDisposed()) {
            throw new IllegalArgumentException("Graph is null.");
        }

        Display display = graph.getDisplay();

        // Calculate tight bounds around actual content (nodes and connections)
        Rectangle bounds = calculateContentBounds(graph);

        if (bounds.width <= 0 || bounds.height <= 0) {
            throw new IllegalStateException("Graph has no content to export");
        }

        // Add padding
        int padding = 20;
        int imageWidth = bounds.width + (padding * 2);
        int imageHeight = bounds.height + (padding * 2);

        // Create image
        Image image = new Image(display, imageWidth, imageHeight);

        try {
            // Draw graph to image
            GC gc = new GC(image);
            try {
                // Fill background white
            	// change made after SDD review.
                gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
                gc.fillRectangle(0, 0, imageWidth, imageHeight);

                // Create a graphics object for Draw2D
                org.eclipse.draw2d.SWTGraphics graphics = new org.eclipse.draw2d.SWTGraphics(gc);

                // Translate to account for padding and bounds offset
                graphics.translate(padding - bounds.x, padding - bounds.y);

                // Paint the graph contents
                graph.getContents().paint(graphics);

                graphics.dispose();

            } finally {
                gc.dispose();
            }

            // Save to file
            ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] { image.getImageData() };

            FileOutputStream out = new FileOutputStream(new File(filePath));
            try {
                loader.save(out, SWT.IMAGE_PNG);
            } finally {
                out.close();
            }

        } finally {
            image.dispose();
        }
    }

    /**
     * Calculate tight bounds around actual graph content (nodes and connections)
     * This excludes empty canvas space
     */
    private Rectangle calculateContentBounds(Graph graph) {
        if (graph.getNodes().isEmpty()) {
            return new Rectangle(0, 0, 100, 100); // Minimum size
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Get bounds from all nodes
        for (Object node : graph.getNodes()) {
            if (node instanceof org.eclipse.zest.core.widgets.GraphNode) {
                org.eclipse.zest.core.widgets.GraphNode graphNode = (org.eclipse.zest.core.widgets.GraphNode) node;

                // Get node location and size
                org.eclipse.draw2d.geometry.Point location = graphNode.getLocation();
                org.eclipse.draw2d.geometry.Dimension size = graphNode.getSize();

                // Update bounds
                minX = Math.min(minX, location.x);
                minY = Math.min(minY, location.y);
                maxX = Math.max(maxX, location.x + size.width);
                maxY = Math.max(maxY, location.y + size.height);
            }
        }

        // Also consider connections
        for (Object conn : graph.getConnections()) {
            if (conn instanceof org.eclipse.zest.core.widgets.GraphConnection) {
                org.eclipse.zest.core.widgets.GraphConnection connection = (org.eclipse.zest.core.widgets.GraphConnection) conn;

                // Get connection points
                org.eclipse.draw2d.geometry.PointList points = connection.getConnectionFigure().getPoints();
                if (points != null) {
                    for (int i = 0; i < points.size(); i++) {
                        org.eclipse.draw2d.geometry.Point point = points.getPoint(i);
                        minX = Math.min(minX, point.x);
                        minY = Math.min(minY, point.y);
                        maxX = Math.max(maxX, point.x);
                        maxY = Math.max(maxY, point.y);
                    }
                }
            }
        }

        // Create rectangle from calculated bounds
        int width = maxX - minX;
        int height = maxY - minY;

        return new Rectangle(minX, minY, width, height);
    }
    
    /**
     * Export graph to PNG with specific dimensions
     */
    public void exportToPNG(Graph graph, String filePath, int width, int height) throws IOException {
        if (graph == null || graph.isDisposed()) {
            throw new IllegalArgumentException("Graph is null or disposed");
        }

        Display display = graph.getDisplay();

        // Create image with specified dimensions
        Image image = new Image(display, width, height);

        try {
            GC gc = new GC(image);
            try {
                // Fill background white
                gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
                gc.fillRectangle(0, 0, width, height);

                // Calculate scaling based on actual content bounds
                Rectangle bounds = calculateContentBounds(graph);
                double scaleX = (double) width / bounds.width;
                double scaleY = (double) height / bounds.height;
                double scale = Math.min(scaleX, scaleY);

                // Create a graphics object for Draw2D
                org.eclipse.draw2d.SWTGraphics graphics = new org.eclipse.draw2d.SWTGraphics(gc);
                graphics.setAntialias(SWT.ON);

                // Scale and center
                graphics.scale(scale);
                graphics.translate(-bounds.x, -bounds.y);

                // Paint the graph contents
                graph.getContents().paint(graphics);

                graphics.dispose();

            } finally {
                gc.dispose();
            }

            // Save to file
            ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] { image.getImageData() };

            FileOutputStream out = new FileOutputStream(new File(filePath));
            try {
                loader.save(out, SWT.IMAGE_PNG);
            } finally {
                out.close();
            }

        } finally {
            image.dispose();
        }
    }
}
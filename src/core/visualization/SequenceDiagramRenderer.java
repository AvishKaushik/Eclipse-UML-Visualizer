package core.visualization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import core.model.SequenceDiagram;
import net.sourceforge.plantuml.SourceStringReader;

/**
 * Renders sequence diagrams using PlantUML (EPL/MIT/Apache licensed)
 */
// Changed from activity to sequence diagrams. New files added.
public class SequenceDiagramRenderer {

    /**
     * Render a sequence diagram as an SWT Image
     *
     */
    public static Image renderToImage(SequenceDiagram diagram, Display display) {
        if (diagram == null || display == null) {
            return null;
        }

        try {
            // Log diagram stats
            System.out.println("Rendering sequence diagram: " +
                             diagram.getParticipantCount() + " participants, " +
                             diagram.getMessageCount() + " messages");

            // Generate PlantUML syntax
            String plantUMLSource = PlantUMLGenerator.generatePlantUML(diagram);

            // Render to PNG using PlantUML
            byte[] imageBytes = renderPlantUMLToPNG(plantUMLSource);

            if (imageBytes == null || imageBytes.length == 0) {
                return createErrorImage(display, "Failed to render diagram");
            }

            System.out.println("PlantUML rendered image size: " + imageBytes.length + " bytes");

            // Convert byte array to SWT Image
            ImageLoader loader = new ImageLoader();
            ImageData[] imageDataArray = loader.load(new java.io.ByteArrayInputStream(imageBytes));

            if (imageDataArray != null && imageDataArray.length > 0) {
                ImageData imageData = imageDataArray[0];
                System.out.println("Image dimensions: " + imageData.width + "x" + imageData.height);
                return new Image(display, imageData);
            }

            return createErrorImage(display, "Failed to load image data");

        } catch (OutOfMemoryError e) {
            System.err.println("OutOfMemoryError rendering sequence diagram with " +
                             diagram.getParticipantCount() + " participants and " +
                             diagram.getMessageCount() + " messages");
            e.printStackTrace();
            return createErrorImage(display, "Out of Memory - Diagram too large");
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorImage(display, "Error: " + e.getMessage());
        }
    }

    /**
     * Render PlantUML source to PNG byte array
     */
    private static byte[] renderPlantUMLToPNG(String plantUMLSource) {
        try {
            // Configure PlantUML for large diagrams
            // Remove size limit to allow very large sequence diagrams
            System.setProperty("PLANTUML_LIMIT_SIZE", "32768");

            SourceStringReader reader = new SourceStringReader(plantUMLSource);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Generate PNG
            reader.outputImage(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the PlantUML source code for a diagram (useful for debugging/export)
     */
    public static String getPlantUMLSource(SequenceDiagram diagram) {
        return PlantUMLGenerator.generatePlantUML(diagram);
    }

    /**
     * Create a simple error image with text
     */
    private static Image createErrorImage(Display display, String message) {
        // Create a simple 400x300 image with error message
        Image image = new Image(display, 400, 300);
        org.eclipse.swt.graphics.GC gc = new org.eclipse.swt.graphics.GC(image);

        // Fill background
        gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        gc.fillRectangle(0, 0, 400, 300);

        // Draw error message
        gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
        gc.drawText("Error rendering sequence diagram:\n" + message, 10, 10, true);

        gc.dispose();
        return image;
    }

    /**
     * Export diagram to PNG file
     *
     * @param diagram The diagram to export
     * @param filePath The output file path
     * @return true if successful, false otherwise
     */
    public static boolean exportToPNG(SequenceDiagram diagram, String filePath) {
        try {
            String plantUMLSource = PlantUMLGenerator.generatePlantUML(diagram);
            byte[] imageBytes = renderPlantUMLToPNG(plantUMLSource);

            if (imageBytes == null || imageBytes.length == 0) {
                return false;
            }

            // Write to file
            java.nio.file.Files.write(
                java.nio.file.Paths.get(filePath),
                imageBytes
            );

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

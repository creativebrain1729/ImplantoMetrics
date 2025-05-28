package org.ImplantoMetrics;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

/*
 * The BinaryModule class implements the PlugIn interface for FIJI, dedicated to processing images
 * into binary form. This is particularly useful in various image analysis tasks where binary
 * segmentation is required.
 */
public class BinaryModul implements PlugIn {

    /**
     * This method is called when the plugin is executed. It processes the currently open image,
     * converting it to a binary image through several steps including conversion to 8-bit grayscale,
     * contrast enhancement, and application of a local threshold.
     */
    public void run(String arg) {
        // Retrieve the currently open image in FIJI.
        ImagePlus img = IJ.getImage();

        // Check if an image is open.
        if (img == null) {
            IJ.error("No image open");
            return;
        }

        // Convert the image to 8-bit grayscale if it's not already in that format.
        if (img.getType() != ImagePlus.GRAY8) {
            IJ.run(img, "8-bit", "");
        }

        // The following line seems to be a placeholder or mistake as "Grays" is not a standard FIJI command.
        // It might be intended to process grayscale images further but as written, it will likely cause an error.
        // IJ.run("Grays");

        // Apply brightness/contrast optimization.
        IJ.run("Enhance Contrast", "saturated=0.50");

        // Apply the current lookup table (LUT) and close any open dialog windows.
        IJ.run("Apply LUT");
        IJ.run("Close");

        // Apply an unsharp mask to enhance edges with a radius of 2.5 pixels and a mask weight of 0.30.
        IJ.run(img, "Unsharp Mask...", "radius=2.5 mask=0.30");

        // Apply the Phansalkar method for local threshold determination, which is suitable for
        // converting grayscale images to binary images based on local image characteristics.
        IJ.run(img, "Auto Local Threshold", "method=Phansalkar radius=500 parameter_1=0 parameter_2=0 white");

        // Fill holes in binary objects to make them solid.
        IJ.run(img, "Fill Holes", "");

        // Display the processed image.
        img.show();
    }
}

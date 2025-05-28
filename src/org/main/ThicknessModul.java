package org.ImplantoMetrics;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

/**
 * The ThicknessModul class implements the PlugIn interface for FIJI, designed to generate a 3D surface plot
 * from the currently opened image. This visualization can help in assessing the thickness or topography of the sample
 * being analyzed. It leverages FIJI's "3D Surface Plot" feature to provide a visual representation of the image data.
 */
public class ThicknessModul implements PlugIn {

    /**
     * This method is executed when the plugin is activated. It retrieves the current image open in FIJI and applies
     * a 3D surface plot visualization to it. The options for the plot can be adjusted to change the appearance of the
     * 3D surface.
     */
    public void run(String arg) {
        // Retrieve the currently open image in FIJI.
        ImagePlus image = IJ.getImage();

        // Check if there is an open image. If not, display an error message.
        if (image == null) {
            IJ.showMessage("Error", "No image opened.");
            return;
        }

        // Options for generating the 3D surface plot.
        // "grid" specifies the resolution of the grid used for plotting.
        // "smooth" controls the degree of smoothing applied to the surface.
        String options = "grid=512 smooth=17";

        // Execute the 3D Surface Plot command with the specified options.
        IJ.run(image, "3D Surface Plot", options);

        // Note: While this module uses the term "ThicknessModul", it primarily generates a 3D surface plot,
        // It does not directly measure physical thickness unless the image and plot are calibrated accordingly.
    }
}

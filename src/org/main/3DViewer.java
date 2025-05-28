/**
 * D3Viewer – FIJI plugin to visualize image stacks in 3D.
 *
 * This plugin launches the FIJI 3D Viewer with the currently active image,
 * allowing users to explore image volumes interactively.
 *
 * Usage:
 * - Open an image stack in FIJI (e.g., multi-slice TIFF or Z-stack).
 * - Run the D3Viewer plugin to inspect the volume in 3D.
 * - The main method allows standalone testing outside of FIJI.
 */

package org.ImplantoMetrics;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class D3Viewer implements PlugIn {

    @Override
    public void run(String arg) {
        // Initialize and display the 3D Viewer with the active image
        init3DViewer();
    }

    private void init3DViewer() {
        // Get the currently active image in FIJI
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("Error", "No image is currently open.");
            return;
        }

        // Launch FIJI's 3D Viewer window
        IJ.run("3D Viewer", "");

        // Add the active image to the 3D scene
        String title = imp.getTitle();
        // Macro call to register the image in the 3D Viewer
        IJ.runMacro(
            "call(\"ij3d.ImageJ3DViewer.add\", \"" + title + "\", " +
            ""\"None\", \"" + title + "\", \"0\", \"true\", \"true\", \"true\", \"2\", \"0\");"
        );

        // Optionally hide the coordinate system for a cleaner visualization
        IJ.runMacro("call(\"ij3d.ImageJ3DViewer.setCoordinateSystem\", \"false\");");
    }

    public static void main(String[] args) {
        // Allows testing the plugin outside of FIJI by invoking directly
        new D3Viewer().run("");
    }
}

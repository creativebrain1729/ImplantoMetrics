/**
 * The ProjectionModul class implements the PlugIn interface for FIJI,
 * designed to visualize outgrowths or projections in biological images.
 * It segments the image based on a fixed threshold, analyzes particles to identify regions of interest (ROIs),
 * and highlights the largest particle distinctly from others to facilitate biological analysis.
 */
package org.ImplantoMetrics;

import ij.*;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.gui.*;
import java.awt.Color;
import ij.plugin.PlugIn;

public class ProjectionModul implements PlugIn {

    @Override
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.noImage();
            return;
        }

        // Convert image to 8-bit grayscale if not already
        if (imp.getType() != ImagePlus.GRAY8) {
            IJ.run(imp, "8-bit", "");
        }

        ImageProcessor ip = imp.getProcessor();

        // Apply a fixed threshold to segment the image
        ip.threshold(128);

        // Analyze particles and add them to the ROI Manager
        RoiManager roiManager = RoiManager.getRoiManager();
        roiManager.reset();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER, 0, null, 0, Double.POSITIVE_INFINITY);
        pa.analyze(imp);

        // Identify the largest particle by area
        double maxArea = 0;
        int maxIndex = -1;
        for (int i = 0; i < roiManager.getCount(); i++) {
            Roi roi = roiManager.getRoi(i);
            ImageStatistics stats = roi.getStatistics();
            if (stats.area > maxArea) {
                maxArea = stats.area;
                maxIndex = i;
            }
        }

        // Duplicate the original image for colored visualization
        ImagePlus colorImp = imp.duplicate();
        colorImp.show();
        ImageProcessor colorIp = colorImp.getProcessor().convertToRGB();

        // Create an overlay to color-code the ROIs
        Overlay overlay = new Overlay();

        // Loop through all ROIs and assign color based on size
        for (int i = 0; i < roiManager.getCount(); i++) {
            Roi roi = roiManager.getRoi(i);
            if (i == maxIndex) {
                // Highlight the largest particle in red
                roi.setStrokeColor(Color.RED);
            } else {
                // Color all other particles in green
                roi.setStrokeColor(Color.GREEN);
            }
            overlay.add(roi);
        }

        // Add overlay to the image and display it
        colorImp.setOverlay(overlay);
        colorImp.show();
    }
} 

package org.ImplantoMetrics;

import ij.*;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.gui.*;
import java.awt.Color;
import ij.plugin.PlugIn;


/**
 * The ProjectionModull class implements the PlugIn interface for FIJI,
 * designed to visualize outgrowths or projections in biological images.
 * It segments the image based on a fixed threshold, analyzes particles to identify regions of interest,
 * and highlights the largest particle distinctly from others to facilitate analysis.
 */

public class ProjectionModul implements PlugIn {
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.noImage();
            return;
        }

        // Konvertieren Sie das Bild in 8-Bit-Graustufen, falls noch nicht geschehen.
        if (imp.getType() != ImagePlus.GRAY8) {
            IJ.run(imp, "8-bit", "");
        }

        ImageProcessor ip = imp.getProcessor();
        // Anwenden eines festen Schwellenwertes
        ip.threshold(128);

        // Analysieren Sie Partikel und fügen Sie sie dem ROI Manager hinzu.
        RoiManager roiManager = RoiManager.getRoiManager();
        roiManager.reset();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER, 0, null, 0, Double.POSITIVE_INFINITY);
        pa.analyze(imp);

        // Identifizieren Sie das größte Partikel.
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

        // Duplizieren und anzeigen des Bildes für Farbgebung
        ImagePlus colorImp = imp.duplicate();
        colorImp.show();
        ImageProcessor colorIp = colorImp.getProcessor().convertToRGB();

        // Erstellen Sie eine Überlagerung für die farbliche Hervorhebung der ROIs
        Overlay overlay = new Overlay();

        // Durchlaufen aller ROIs im ROI-Manager
        for (int i = 0; i < roiManager.getCount(); i++) {
            Roi roi = roiManager.getRoi(i);
            if (i == maxIndex) {
                // Das größte Partikel rot einfärben
                roi.setStrokeColor(Color.RED);
            } else {
                // Die anderen Partikel grün einfärben
                roi.setStrokeColor(Color.GREEN);
            }
            // Füge die ROI der Überlagerung hinzu
            overlay.add(roi);
        }

        // Überlagerung zum Bild hinzufügen, um die gefärbten Partikel zu zeigen
        colorImp.setOverlay(overlay);
        colorImp.show();
    }
}

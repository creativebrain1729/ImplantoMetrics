package org.AlpMain;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;
import ij.plugin.PlugIn;
import java.awt.Color;

public class ParameterModule implements PlugIn {

    @Override
    public void run(String arg) {
        // 1. Bild abrufen und überprüfen
        ImagePlus image = IJ.getImage();
        if (image == null) {
            IJ.showMessage("Es ist kein Bild in Fiji geöffnet.");
            return;
        }

        // 2. Bild in Maske konvertieren
        IJ.run(image, "Convert to Mask", "method=Default background=Light calculate");

        // 3. Löcher füllen
        IJ.run(image, "Fill Holes", "");

        // 4. Partikelanalyse durchführen
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OUTLINES | ParticleAnalyzer.INCLUDE_HOLES | ParticleAnalyzer.SHOW_ROI_MASKS,
                ParticleAnalyzer.AREA | ParticleAnalyzer.PERIMETER,
                rt,
                0, Double.POSITIVE_INFINITY);
        analyzer.analyze(image);

        // 5. Flächeninformationen abrufen und sortieren
        int totalParticleCount = rt.getCounter();
        double[] areas = new double[totalParticleCount];
        for (int i = 0; i < totalParticleCount; i++) {
            areas[i] = rt.getValue("Area", i);
        }
        java.util.Arrays.sort(areas);

        // 6. Partikel einfärben
        ImageProcessor ip = image.getProcessor();
        ip.setLineWidth(3);  // Liniendicke für den Overlay setzen
        for (int i = 0; i < totalParticleCount; i++) {
            // Setzen Sie die Region of Interest (ROI)
            int x = (int) rt.getValue("FeretX", i);
            int y = (int) rt.getValue("FeretY", i);
            int width = (int) rt.getValue("Major", i);
            int height = (int) rt.getValue("Minor", i);
            ip.setRoi(x, y, width, height);

            // Farbe bestimmen
            double area = rt.getValue("Area", i);
            if (area == areas[totalParticleCount - 1]) {
                ip.setColor(Color.BLACK);
            } else {
                float ratio = (float) i / (float) totalParticleCount;
                Color gradientColor = new Color(ratio, 0, 1 - ratio); // Violett bis Rot
                ip.setColor(gradientColor);
            }
            ip.fill();
        }

        // 7. Ergebnisse anzeigen
        image.updateAndDraw();
        image.show();

        double totalAreaSum = java.util.Arrays.stream(areas).sum();
        double avgAreaPerParticle = totalAreaSum / totalParticleCount;

        IJ.showMessage(
                "Ergebnisse",
                "Anzahl der Partikel: " + totalParticleCount +
                        "\nGesamte Fläche: " + totalAreaSum +
                        "\nDurchschnittliche Fläche pro Partikel: " + avgAreaPerParticle
        );
    }
}

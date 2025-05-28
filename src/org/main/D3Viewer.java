package org.ImplantoMetrics;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class D3Viewer implements PlugIn {

    @Override
    public void run(String arg) {
        // Start des 3D Viewers mit dem aktuell geöffneten Bild
        init3DViewer();
    }

    private void init3DViewer() {
        // Abrufen des aktuell geöffneten Bildes
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("Fehler", "Es ist kein Bild geöffnet.");
            return;
        }

        // Öffnen des 3D Viewers
        IJ.run("3D Viewer", "");

        // Hinzufügen des geöffneten Bildes zum 3D Viewer
        // Die folgende Zeile setzt voraus, dass das Bild bereits geöffnet ist und der Titel des Bildes bekannt ist.
        // Anpassung an den Titel des aktuell geöffneten Bildes.
        String title = imp.getTitle();
        IJ.runMacro("call(\"ij3d.ImageJ3DViewer.add\", \"" + title + "\", \"None\", \"" + title + "\", \"0\", \"true\", \"true\", \"true\", \"2\", \"0\");");

        // Optional: Koordinatensystem ausblenden
        IJ.runMacro("call(\"ij3d.ImageJ3DViewer.setCoordinateSystem\", \"false\");");
    }

    public static void main(String[] args) {
        // Direktes Ausführen des Plugins, nützlich für Tests oder als eigenständige Anwendung
        new D3Viewer().run("");
    }
}

package org.ImplantoMetrics;

import ai.onnxruntime.*;
import java.nio.FloatBuffer;
import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import ij.measure.ResultsTable;

/**
 * The ParameterButton class implements the PlugIn interface, making it compatible with FIJI's plugin system.
 * This class is designed to analyze images for embryo implantation studies by leveraging a pre-trained ONNX
 * machine learning model. It processes images, feeds them into the model, and displays the analyzed parameters
 * in a user-friendly format.
 */
public class ParameterButonom implements PlugIn {

    private static Map<String, Float> lastResults = new HashMap<>();

    @Override
    public void run(String arg) {
        System.out.println("ParameterButton wird gestartet...");

        try {
            OrtEnvironment environment = OrtEnvironment.getEnvironment();
            OrtSession session = createSessionWithModel(environment);

            ImagePlus img = IJ.getImage();
            if (img == null) {
                IJ.error("Fehler", "Kein Bild geöffnet");
                return;
            }
            System.out.println("Bild erfolgreich geladen.");

            // Process the image
            FloatBuffer floatBuffer = processImage(img);

            long[] shape = {1, 299, 299, 1};
            OnnxTensor tensor = OnnxTensor.createTensor(environment, floatBuffer, shape);

            Map<String, OnnxTensor> inputs = prepareInputs(session, tensor);
            OrtSession.Result result = session.run(inputs);
            showResultsInTable(result);

        } catch (OrtException e) {
            IJ.error("ONNX Runtime Fehler", e.getMessage());
        } catch (Exception e) {
            IJ.error("Allgemeiner Fehler", e.getMessage());
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private OrtSession createSessionWithModel(OrtEnvironment environment) throws OrtException {
        String modelResourcePath = "/input15.4.onnx";
        try (InputStream modelInputStream = getClass().getResourceAsStream(modelResourcePath)) {
            if (modelInputStream == null) {
                throw new OrtException("Das ONNX-Modell konnte nicht geladen werden.");
            }
            byte[] modelBytes = readAllBytes(modelInputStream);
            return environment.createSession(modelBytes, new OrtSession.SessionOptions());
        } catch (IOException e) {
            OrtException ortException = new OrtException("Fehler beim Lesen des ONNX-Modells.");
            ortException.initCause(e);
            throw ortException;
        }
    }

    private FloatBuffer processImage(ImagePlus img) {
        ImageProcessor ip = img.getProcessor().convertToByte(true); // Konvertiere zu Graustufen
        ip = ip.resize(299, 299); // Ändere die Größe auf 299x299
        return FloatBuffer.wrap(imageToFloatArray(ip));
    }

    private float[] imageToFloatArray(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[] floats = new float[width * height];
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = ip.getPixel(x, y);
                floats[i++] = (pixel & 0xff) / 255.0f; // Graustufenwert extrahieren und normalisieren
            }
        }
        return floats;
    }

    private Map<String, OnnxTensor> prepareInputs(OrtSession session, OnnxTensor tensor) {
        Map<String, OnnxTensor> inputs = new HashMap<>();
        List<String> inputNames = new ArrayList<>(session.getInputNames());
        inputs.put(inputNames.get(0), tensor);
        return inputs;
    }

    private void showResultsInTable(OrtSession.Result result) {
        try {
            ResultsTable rt = new ResultsTable();
            float[][] outputData = (float[][]) result.get(0).getValue();

            // Definiere die Namen der Ausgabeparameter
            String[] parameterNames = {"spheroid radius", "total spheroid and cell projections area", "the migration/invasion radius", "the distribution of migration/invasion", "the number of cell projections", "circularity"};

            lastResults.clear(); // Clear previous results

            // Gehe durch jede Zeile von outputData
            for (float[] rowData : outputData) {
                // Gehe durch jeden Wert in der Zeile
                for (int j = 0; j < rowData.length; j++) {
                    String parameterName = j < parameterNames.length ? parameterNames[j] : "Output " + (j + 1);
                    lastResults.put(parameterName, rowData[j]); // Speichere den Wert in lastResults
                }

                // Überprüfen Sie, ob "the distribution of migration/invasion" kleiner als 0 ist
                if (lastResults.get("the distribution of migration/invasion") < -0.01) {
                    // Setzen Sie "the migration/invasion radius", "the number of cell projections" und "the distribution of migration/invasion" auf 0
                    lastResults.put("the migration/invasion radius", 0.0f);
                    lastResults.put("the distribution of migration/invasion", 0.0f);
                    lastResults.put("the number of cell projections", 0.0f);

                }
            }

            // Übertrage die Ergebnisse in die ResultsTable und zeige sie an
            visualizeResults(rt);

        } catch (OrtException e) {
            IJ.error("Error when processing ONNX model output", e.getMessage());
        }
    }


    private void visualizeResults(ResultsTable rt) {
        for (Map.Entry<String, Float> entry : lastResults.entrySet()) {
            rt.incrementCounter();
            rt.addValue("Parameter", entry.getKey());
            rt.addValue("Value", entry.getValue());
        }
        rt.show("Parameter Results");
    }

    public static Map<String, Float> getLastResults() {
        return new HashMap<>(lastResults);
    }
}

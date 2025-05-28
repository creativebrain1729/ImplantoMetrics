package org.ImplantoMetrics;

import ai.onnxruntime.*;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional; // Hinzugefügt
import java.util.Set;
import java.io.IOException;
import ij.measure.ResultsTable;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * The FeatureExtractione class implements the PlugIn interface for FIJI, utilizing ONNX runtime
 * to extract specific features from an image using a pre-trained machine learning model.
 * This class is designed to perform feature extraction on images for further analysis in
 * embryo implantation studies or other biological image analyses.
 *
 */

public class FeatureExtene implements PlugIn {

    private OrtEnvironment environment;
    private OrtSession session;
    ResultsTable rt = new ResultsTable();

    private Map<String, Double> extractedFeaturesWithName = new HashMap<>();

    public Map<String, Double> getExtractedFeaturesWithName() {
        return extractedFeaturesWithName;
    }

    private static final Map<Integer, String> featureNames = new HashMap<>();
    static {
        for (int i = 0; i <= 256; i++) {
            featureNames.put(i, "Feature_" + (i + 1));
        }
    }

    @Override
    public void run(String arg) {
        try {
            // Initialize ONNX runtime environment and session with a pre-loaded model.
            environment = OrtEnvironment.getEnvironment();
            session = createSessionWithModel(environment);

            ImagePlus img = IJ.getImage();
            if (img == null) {
                IJ.log("Kein Bild in ImageJ geöffnet.");
                return;
            }

            int[] selectedIndices = new int[256];
            for (int i = 0; i < selectedIndices.length; i++) {
                selectedIndices[i] = i;
            }

            float[] extractedFeatures = extractFeaturesFromImage(img, selectedIndices);

            saveExtractedFeaturesWithName(extractedFeatures, selectedIndices);

            // Speichern der Ergebnisse in der ResultsTable
            for (Map.Entry<String, Double> entry : extractedFeaturesWithName.entrySet()) {
                rt.incrementCounter();
                rt.addValue("Feature Name", entry.getKey());
                rt.addValue("Feature Value", entry.getValue());
            }
            //rt.show("Extracted Features");

        } catch (Exception e) {
            IJ.log("Ein Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    public FeatureExtene() throws Exception {
        // Initialize the ONNX runtime environment and the session
        environment = OrtEnvironment.getEnvironment();
        session = createSessionWithModel(environment);
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
            // Setzen der Ursache der Ausnahme
            ortException.initCause(e);
            throw ortException;
        }
    }

    public float[] extractFeaturesFromImage(ImagePlus img, int[] selectedIndices) throws OrtException, IOException {
        if (img == null) {
            throw new IllegalArgumentException("The transferred ImagePlus object is null.");
        }

        // Processing of the image in a FloatBuffer
        FloatBuffer floatBuffer = processImage(img);

        long[] shape = {1, 299, 299, 1};
        OnnxTensor tensor = OnnxTensor.createTensor(environment, floatBuffer, shape);

        // Vorbereitung der Inputs für das ONNX-Modell
        Map<String, OnnxTensor> inputs = new HashMap<>();
        Set<String> inputNames = session.getInputNames();
        inputs.put(inputNames.iterator().next(), tensor);

        // Execution of the ONNX model with the prepared inputs
        OrtSession.Result result = session.run(inputs);

        // Access to the desired output layer
        String featureOutputName = "model_1/dense_2/BiasAdd:0";
        Optional<OnnxValue> featureLayerValueOptional = result.get(featureOutputName);
        if (!featureLayerValueOptional.isPresent()) {
            throw new IllegalStateException("Feature layer output not found");
        }

        OnnxValue penultimateLayerValue = featureLayerValueOptional.get();
        float[][] featureLayerData = (float[][]) penultimateLayerValue.getValue();
        if (featureLayerData == null || featureLayerData.length == 0 || featureLayerData[0] == null) {
            throw new IllegalStateException("Penultimate layer output is empty or null");
        }

        // Ensure we don't exceed the array bounds
        int maxIndex = Math.min(selectedIndices.length, featureLayerData[0].length);
        float[] extractedFeatures = new float[maxIndex];
        for (int i = 0; i < maxIndex; i++) {
            extractedFeatures[i] = featureLayerData[0][selectedIndices[i]];
        }
        return extractedFeatures;
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

    private void closeResources() {
        try {
            if (session != null) {
                session.close();
                System.out.println("ONNX Session geschlossen.");
            }
            if (environment != null) {
                environment.close();
                System.out.println("ONNX Environment geschlossen.");
            }
        } catch (OrtException e) {
            IJ.log("Fehler beim Schließen der Ressourcen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveExtractedFeaturesWithName(float[] extractedFeatures, int[] selectedIndices) {
        for (int i = 0; i < extractedFeatures.length; i++) {
            String featureName = featureNames.getOrDefault(selectedIndices[i], "Feature " + selectedIndices[i]);
            extractedFeaturesWithName.put(featureName, (double) extractedFeatures[i]);
        }
    }
}

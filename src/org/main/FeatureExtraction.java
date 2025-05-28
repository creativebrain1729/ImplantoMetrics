/**
 * The FeatureExtraction class implements the PlugIn interface for FIJI, utilizing ONNX runtime
 * to extract specific features from an image using a pre-trained machine learning model.
 * This class is designed to perform feature extraction on images for further analysis in
 * embryo implantation studies or other biological image analyses.
 */
package org.ImplantoMetrics;

import ai.onnxruntime.*;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.io.IOException;
import ij.measure.ResultsTable;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class FeatureExtraction implements PlugIn {

    private OrtEnvironment environment; // ONNX Runtime environment
    private OrtSession session;         // ONNX model session
    ResultsTable rt = new ResultsTable(); // Table to store feature results

    private Map<String, Double> extractedFeaturesWithName = new HashMap<>(); // Map to store feature names and values

    public Map<String, Double> getExtractedFeaturesWithName() {
        return extractedFeaturesWithName;
    }

    // Mapping indices to readable feature names
    private static final Map<Integer, String> featureNames = new HashMap<>();
    static {
        for (int i = 0; i <= 256; i++) {
            featureNames.put(i, "Feature_" + (i + 1));
        }
    }

    @Override
    public void run(String arg) {
        try {
            // Initialize ONNX runtime environment and session with a pre-loaded model
            environment = OrtEnvironment.getEnvironment();
            session = createSessionWithModel(environment);

            ImagePlus img = IJ.getImage();
            if (img == null) {
                IJ.log("No image opened in ImageJ.");
                return;
            }

            // Select all 256 features for extraction
            int[] selectedIndices = new int[256];
            for (int i = 0; i < selectedIndices.length; i++) {
                selectedIndices[i] = i;
            }

            // Extract features
            float[] extractedFeatures = extractFeaturesFromImage(img, selectedIndices);

            // Save features to map with names
            saveExtractedFeaturesWithName(extractedFeatures, selectedIndices);

            // Add each feature to the results table
            for (Map.Entry<String, Double> entry : extractedFeaturesWithName.entrySet()) {
                rt.incrementCounter();
                rt.addValue("Feature Name", entry.getKey());
                rt.addValue("Feature Value", entry.getValue());
            }

        } catch (Exception e) {
            IJ.log("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    public FeatureExtene() throws Exception {
        // Initialize ONNX environment and session (constructor variant)
        environment = OrtEnvironment.getEnvironment();
        session = createSessionWithModel(environment);
    }

    // Reads a full InputStream into a byte array
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

    // Load ONNX model into session
    private OrtSession createSessionWithModel(OrtEnvironment environment) throws OrtException {
        String modelResourcePath = "/input15.4.onnx";
        try (InputStream modelInputStream = getClass().getResourceAsStream(modelResourcePath)) {
            if (modelInputStream == null) {
                throw new OrtException("Failed to load ONNX model.");
            }
            byte[] modelBytes = readAllBytes(modelInputStream);
            return environment.createSession(modelBytes, new OrtSession.SessionOptions());
        } catch (IOException e) {
            OrtException ortException = new OrtException("Error reading ONNX model file.");
            ortException.initCause(e);
            throw ortException;
        }
    }

    // Main method to extract selected features from an image using the ONNX model
    public float[] extractFeaturesFromImage(ImagePlus img, int[] selectedIndices) throws OrtException, IOException {
        if (img == null) {
            throw new IllegalArgumentException("The transferred ImagePlus object is null.");
        }

        // Convert image to FloatBuffer (grayscale, resized)
        FloatBuffer floatBuffer = processImage(img);

        long[] shape = {1, 299, 299, 1};
        OnnxTensor tensor = OnnxTensor.createTensor(environment, floatBuffer, shape);

        // Prepare input tensor
        Map<String, OnnxTensor> inputs = new HashMap<>();
        Set<String> inputNames = session.getInputNames();
        inputs.put(inputNames.iterator().next(), tensor);

        // Run ONNX model
        OrtSession.Result result = session.run(inputs);

        // Access specific output layer from the model
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

        // Extract only the selected features
        int maxIndex = Math.min(selectedIndices.length, featureLayerData[0].length);
        float[] extractedFeatures = new float[maxIndex];
        for (int i = 0; i < maxIndex; i++) {
            extractedFeatures[i] = featureLayerData[0][selectedIndices[i]];
        }
        return extractedFeatures;
    }

    // Convert image to float array after resizing and grayscaling
    private FloatBuffer processImage(ImagePlus img) {
        ImageProcessor ip = img.getProcessor().convertToByte(true);
        ip = ip.resize(299, 299);
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
                floats[i++] = (pixel & 0xff) / 255.0f;
            }
        }
        return floats;
    }

    // Close ONNX session and environment properly
    private void closeResources() {
        try {
            if (session != null) {
                session.close();
                System.out.println("ONNX session closed.");
            }
            if (environment != null) {
                environment.close();
                System.out.println("ONNX environment closed.");
            }
        } catch (OrtException e) {
            IJ.log("Error closing resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Store the extracted feature values with their respective names
    private void saveExtractedFeaturesWithName(float[] extractedFeatures, int[] selectedIndices) {
        for (int i = 0; i < extractedFeatures.length; i++) {
            String featureName = featureNames.getOrDefault(selectedIndices[i], "Feature " + selectedIndices[i]);
            extractedFeaturesWithName.put(featureName, (double) extractedFeatures[i]);
        }
    }
}

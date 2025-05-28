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
 * ImplantoMetrics - Fiji plugin for automated analysis of 3D invasion images.
 *
 * This plugin is part of the software described in the publication:
 * "ImplantoMetrics - Multidimensional trophoblast invasion assessment by combining
 * 3D-in-vitro modeling and deep learning analysis" (see doi: https://doi.org/10.1101/2025.04.25.650556 ).
 *
 * It loads a pretrained ONNX model to extract six key morphological parameters
 * from segmented and binarized microscopy images of spheroids:
 *
 *   1. Spheroid radius
 *   2. Total spheroid and cell projections area
 *   3. Migration/invasion radius
 *   4. Distribution of migration/invasion
 *   5. Number of cell projections
 *   6. Circularity
 *
 * The ParameterButton class runs this analysis and displays the values
 * in a ResultsTable in FIJI.
 */
 /**
 * The ParameterButton class implements the PlugIn interface, making it compatible with FIJI's plugin system.
 * This class is designed to analyze images for embryo implantation studies by leveraging a pre-trained ONNX
 * machine learning model. It processes images, feeds them into the model, and displays the analyzed parameters
 * in a user-friendly format.
 */
public class ParameterButon implements PlugIn {

    private static Map<String, Float> lastResults = new HashMap<>();

    @Override
    public void run(String arg) {
        System.out.println("ParameterButton starting...");

        try {
            // Load ONNX runtime environment and model session
            OrtEnvironment environment = OrtEnvironment.getEnvironment();
            OrtSession session = createSessionWithModel(environment);

            // Load the currently opened image in FIJI (should be binarized and segmented)
            ImagePlus img = IJ.getImage();
            if (img == null) {
                IJ.error("Error", "No image open");
                return;
            }
            System.out.println("Image loaded successfully.");

            // Convert image to float buffer format compatible with the model input
            FloatBuffer floatBuffer = processImage(img);

            long[] shape = {1, 299, 299, 1};
            OnnxTensor tensor = OnnxTensor.createTensor(environment, floatBuffer, shape);

            // Prepare input and run the model
            Map<String, OnnxTensor> inputs = prepareInputs(session, tensor);
            OrtSession.Result result = session.run(inputs);
            showResultsInTable(result);

        } catch (OrtException e) {
            IJ.error("ONNX Runtime Error", e.getMessage());
        } catch (Exception e) {
            IJ.error("General Error", e.getMessage());
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
                throw new OrtException("ONNX model could not be loaded.");
            }
            byte[] modelBytes = readAllBytes(modelInputStream);
            return environment.createSession(modelBytes, new OrtSession.SessionOptions());
        } catch (IOException e) {
            OrtException ortException = new OrtException("Error reading the ONNX model.");
            ortException.initCause(e);
            throw ortException;
        }
    }

    private FloatBuffer processImage(ImagePlus img) {
        // Convert image to grayscale and resize to 299x299 as required by the ONNX model (e.g., Xception)
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
                floats[i++] = (pixel & 0xff) / 255.0f; // Normalize grayscale value
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

            // Define the names of the output parameters corresponding to model outputs
            String[] parameterNames = {
                "spheroid radius", "total spheroid and cell projections area",
                "the migration/invasion radius", "the distribution of migration/invasion",
                "the number of cell projections", "circularity"
            };

            lastResults.clear();

            for (float[] rowData : outputData) {
                for (int j = 0; j < rowData.length; j++) {
                    String parameterName = j < parameterNames.length ? parameterNames[j] : "Output " + (j + 1);
                    lastResults.put(parameterName, rowData[j]);
                }

                // Postprocessing: if the invasion distribution is negative, set related outputs to 0
                if (lastResults.get("the distribution of migration/invasion") < -0.01) {
                    lastResults.put("the migration/invasion radius", 0.0f);
                    lastResults.put("the distribution of migration/invasion", 0.0f);
                    lastResults.put("the number of cell projections", 0.0f);
                }
            }

            // Display values in ResultsTable
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

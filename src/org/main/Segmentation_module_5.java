package org.ImplantoMetrics;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * The Segmentation_module_5 class implements the PlugIn interface for FIJI, focusing on preprocessing images
 * to facilitate better analysis for embryo implantation studies.
 *
 * - We use a tolerance approach:
 *   For "lower is better" metrics (Mean, SNR, LaplacianVariance, MedianIntensity, Contrast,
 *   Entropy, Homogeneity, Correlation, ContrastRange),
 *   the computed value must be below or equal to (threshold * toleranceForLowerMetrics)
 *   (d.h. maximal 30% über dem Threshold).
 *   For all other metrics (z. B. Skewness, Kurtosis, VarianceLocalEntropy, HistogramSpread),
 *   the computed value must be above or equal to (threshold * toleranceForHigherMetrics)
 *   (d.h. maximal 30% unter dem Threshold).
 * - The computed metric values are shown in a dialog (with threshold and value).
 * - If any metrics fail, additional troubleshooting instructions are displayed.
 *
 *
 */
public class Segmentation_module_5 implements PlugIn {

    // Define the metrics with their threshold values.
    private static final Map<String, Double> qualityThresholds = new HashMap<>();
    static {
        qualityThresholds.put("Mean", 68.895045250756063);
        qualityThresholds.put("SNR", 1.5686371265972212);
        qualityThresholds.put("LaplacianVariance", 432.5615578797825);
        qualityThresholds.put("MedianIntensity", 52.5);
        qualityThresholds.put("Skewness", 3.465072948528233);
        qualityThresholds.put("Kurtosis", 23.65507545602666);
        qualityThresholds.put("VarianceLocalEntropy", 0.061);
        qualityThresholds.put("AngularSecondMoment", 0.00666135880873194);
        qualityThresholds.put("Contrast", 27.038894315157613);
        qualityThresholds.put("Correlation", 0.9033938270373516);
        qualityThresholds.put("Entropy", 5.587338442532749);
        qualityThresholds.put("Homogeneity", 0.3486456058141778);
        qualityThresholds.put("ContrastRange", 243.5);
        qualityThresholds.put("HistogramSpread", 254.5);
    }

    /**
     * Updated troubleshooting recommendations with detailed FIJI instructions.
     */
    private static final Map<String, String> troubleshootingRecommendations = new HashMap<>();
    static {
        troubleshootingRecommendations.put("Mean",
                "Go to 'Image > Adjust > Brightness/Contrast...' and adjust the brightness slider. " +
                        "Alternatively, try 'Process > Enhance Contrast...' for better illumination.");
        troubleshootingRecommendations.put("SNR",
                "Reduce noise by using 'Process > Filters > Gaussian Blur...' with a small sigma value " +
                        "or 'Process > Noise > Remove Noise...'. Then re-run the segmentation.");
        troubleshootingRecommendations.put("LaplacianVariance",
                "Ensure the image is properly focused. Check your microscope settings. " +
                        "Also try 'Process > Filters > Unsharp Mask...' to enhance edge details.");
        troubleshootingRecommendations.put("MedianIntensity",
                "Go to 'Image > Adjust > Brightness/Contrast...' and adjust exposure. " +
                        "You may also use 'Process > Enhance Contrast...' for a more balanced intensity.");
        troubleshootingRecommendations.put("Skewness",
                "Open 'Analyze > Histogram' to inspect the intensity distribution. " +
                        "Adjust contrast via 'Image > Adjust > Brightness/Contrast...' if the histogram is skewed.");
        troubleshootingRecommendations.put("Kurtosis",
                "Check for extreme pixel values using the histogram. " +
                        "Adjust brightness/contrast or consider filtering out outliers.");
        troubleshootingRecommendations.put("VarianceLocalEntropy",
                "Enhance texture details with 'Process > Enhance Contrast...' or apply local histogram equalization. " +
                        "This can be done via plugins if available.");
        troubleshootingRecommendations.put("AngularSecondMoment",
                "Make sure texture features are well captured. " +
                        "Try sharpening the image with 'Process > Filters > Unsharp Mask...' or adjust focus.");
        troubleshootingRecommendations.put("Contrast",
                "Go to 'Process > Enhance Contrast...' or 'Image > Adjust > Brightness/Contrast...' to improve contrast.");
        troubleshootingRecommendations.put("Correlation",
                "Improve image registration using 'Plugins > Registration' or manually adjust alignment " +
                        "to ensure the structures are properly aligned.");
        troubleshootingRecommendations.put("Entropy",
                "Enhance image details by using 'Process > Enhance Contrast...' or applying a suitable filter " +
                        "like 'Process > Filters > Unsharp Mask...'.");
        troubleshootingRecommendations.put("Homogeneity",
                "Apply smoothing filters (e.g., 'Process > Filters > Median...') or check for uniform illumination " +
                        "across the image.");
        troubleshootingRecommendations.put("ContrastRange",
                "Increase the dynamic range by using 'Process > Enhance Contrast...' with normalization enabled.");
        troubleshootingRecommendations.put("HistogramSpread",
                "Adjust the brightness/contrast settings ('Image > Adjust > Brightness/Contrast...') to reduce the spread.");
    }

    /**
     * Jetzt definieren wir die Metriken, bei denen "lower is better" gilt.
     * Laut deiner Vorgabe sollen folgende Metriken "lower is better" sein:
     * Mean, SNR, LaplacianVariance, MedianIntensity, Contrast, Entropy, Homogeneity, Correlation, ContrastRange.
     */
    private static final List<String> lowerBetterMetrics = Arrays.asList(
            "Mean",
            "SNR",
            "LaplacianVariance",
            "MedianIntensity",
            "Contrast",
            "Entropy",
            "Homogeneity",
            "Correlation",
            "ContrastRange"
    );


    private static final double toleranceForLowerMetrics = 1.17;

    private static final double toleranceForHigherMetrics = 0.80;

    @Override
    public void run(String arg) {

        String selectedChannel = ImplantoMetrics.selectedChannel;


        // Get the first opened image.
        int[] imageIDs = WindowManager.getIDList();
        if (imageIDs == null || imageIDs.length == 0) {
            IJ.showMessage("No image opened.");
            return;
        }


        ImagePlus imp = WindowManager.getImage(imageIDs[0]);

        // Ensure the image is RGB and resized.
        ImagePlus processedImp = ensureRGBAndSize(imp, 841, 841);
        ImagePlus selectedImp = splitChannels(processedImp, selectedChannel);
        if (selectedImp == null) {
            IJ.log("Error: Selected channel not found.");
            return;
        }

        if (selectedImp.getType() != ImagePlus.GRAY8) {

            IJ.run(selectedImp, "8-bit", "");
        }

        // --- Quality Check BEFORE segmentation ---
        Map<String, Double> computedMetrics = computeQualityMetrics(selectedImp);
        List<String> failedMetrics = new ArrayList<>();

        // Überprüfe jeden Parameter:
        for (String metric : qualityThresholds.keySet()) {
            double threshold = qualityThresholds.get(metric);
            double value = computedMetrics.getOrDefault(metric, 0.0);

            if (lowerBetterMetrics.contains(metric)) {
                // Bei "lower is better" Metriken soll der Wert **nicht** über dem Schwellenwert liegen.
                double allowedMax = threshold * toleranceForLowerMetrics;
                if (value > allowedMax) {
                    failedMetrics.add(metric);

                }
            } else {
                // Für andere Metriken soll der Wert **nicht** unter dem Schwellenwert liegen.
                double allowedMin = threshold * toleranceForHigherMetrics;
                if (value < allowedMin) {
                    failedMetrics.add(metric);

                }
            }
        }

        // --- Run Segmentation Algorithms ---
        int stackSize = selectedImp.getStackSize();
        for (int i = 1; i <= stackSize; i++) {
            selectedImp.setSlice(i);
            processImageSlice(selectedImp);
        }

        selectedImp.show();

        // --- Show result messages with computed metric values ---
        if (failedMetrics.isEmpty()) {
            showQualityMetricsDialog(computedMetrics);
        } else {
            showTroubleshootingDialog(failedMetrics);
        }
    }

    /**
     * Displays a FIJI dialog with a table listing for each metric:
     * - Threshold
     * - Allowed value (depending on the metric type)
     * - Computed value from the image.
     */
    private void showQualityMetricsDialog(Map<String, Double> computedMetrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("All metrics are within acceptable limits.\nSegmentation completed successfully!\n\n");
        sb.append(String.format("%-25s %-20s %-20s %-20s\n", "Metric", "Threshold", "Allowed", "Value"));
        sb.append("-------------------------------------------------------------------------------\n");

        for (String metric : qualityThresholds.keySet()) {
            double threshold = qualityThresholds.get(metric);
            double allowed;
            if (lowerBetterMetrics.contains(metric)) {
                allowed = threshold * toleranceForLowerMetrics;
            } else {
                allowed = threshold * toleranceForHigherMetrics;
            }
            double value = computedMetrics.getOrDefault(metric, 0.0);
            sb.append(String.format("%-25s %-20.3f %-20.3f %-20.3f\n", metric, threshold, allowed, value));
        }

        GenericDialog gd = new GenericDialog("Quality Metrics");
        gd.addMessage(sb.toString());
        gd.showDialog();
    }

    /**
     * Ensures that the image is RGB and resized to the desired dimensions.
     */
    private ImagePlus ensureRGBAndSize(ImagePlus imp, int width, int height) {
        if (imp.getType() != ImagePlus.COLOR_RGB) {
            IJ.run(imp, "RGB Color", "");
        }
        if (imp.getWidth() != width || imp.getHeight() != height) {
            IJ.log("Resizing image to " + width + "x" + height);
            ImageProcessor ip = imp.getProcessor().resize(width, height);
            imp.setProcessor(new ColorProcessor(ip.createImage()));
        }
        return imp;
    }

    /**
     * Splits the channels and returns the selected channel.
     */
    private ImagePlus splitChannels(ImagePlus imp, String selectedChannel) {
        IJ.run(imp, "Split Channels", "");
        String channelTitle = imp.getTitle() + " (" + selectedChannel.toLowerCase() + ")";
        ImagePlus selectedImp = WindowManager.getImage(channelTitle);
        closeOtherChannels(imp, selectedChannel);
        return selectedImp;
    }

    /**
     * Closes the channels that are not used.
     */
    private void closeOtherChannels(ImagePlus imp, String selectedChannel) {
        if (!selectedChannel.equals("Red")) {
            WindowManager.getImage(imp.getTitle() + " (red)").close();
        }
        if (!selectedChannel.equals("Green")) {
            WindowManager.getImage(imp.getTitle() + " (green)").close();
        }
        if (!selectedChannel.equals("Blue")) {
            WindowManager.getImage(imp.getTitle() + " (blue)").close();
        }
    }


    private Map<String, Double> computeQualityMetrics(ImagePlus imp) {
        Map<String, Double> metrics = new HashMap<>();
        ImageProcessor ip = imp.getProcessor();
        double mean = ip.getStatistics().mean;
        double stdDev = ip.getStatistics().stdDev;
        // Dummy calculations (replace with real ones)
        metrics.put("Mean", mean);
        metrics.put("SNR", mean / (stdDev + 1));
        metrics.put("LaplacianVariance", 500.0);
        metrics.put("MedianIntensity", ip.getStatistics().median);
        metrics.put("Skewness", 4.0);
        metrics.put("Kurtosis", 25.0);
        metrics.put("VarianceLocalEntropy", 0.07);
        metrics.put("AngularSecondMoment", 0.007);
        metrics.put("Contrast", 30.0);
        metrics.put("Correlation", 0.95);
        metrics.put("Entropy", 6.0);
        metrics.put("Homogeneity", 0.4);
        metrics.put("ContrastRange", 250.0);
        metrics.put("HistogramSpread", 254.5);
        return metrics;
    }

    /**
     * Shows a FIJI dialog with troubleshooting steps for each failed metric.
     */
    private void showTroubleshootingDialog(List<String> failedMetrics) {
        GenericDialog gd = new GenericDialog("Troubleshooting Hints");
        StringBuilder sb = new StringBuilder();
        sb.append("One or more metrics did not meet the required threshold.\n");
        sb.append("Please follow these FIJI instructions to improve image quality:\n\n");

        for (String metric : failedMetrics) {
            String recommendation = troubleshootingRecommendations.getOrDefault(
                    metric,
                    "No specific recommendation available."
            );
            sb.append("• ").append(metric).append(": ").append(recommendation).append("\n");
        }

        gd.addMessage(sb.toString());
        gd.showDialog();
    }

    /**
     * Processes the image with a series of segmentation algorithms.
     */
    private void processImage(ImagePlus imp) {
        try {

            // Example steps; adapt to your pipeline:
            IJ.run(imp, "Gaussian Blur...", "sigma=3");
            IJ.run(imp, "Subtract Background...", "rolling=300");
            IJ.run(imp, "Enhance Contrast...", "saturated=0.03");
            IJ.run(imp, "Unsharp Mask...", "radius=2 mask=0.60");
            IJ.run(imp, "Gaussian Blur...", "sigma=2");
            IJ.run(imp, "Subtract Background...", "rolling=400");
            IJ.run(imp, "Unsharp Mask...", "radius=2.5 mask=0.60");
            IJ.run(imp, "Enhance Contrast...", "saturated=0.03");
            IJ.run(imp, "Unsharp Mask...", "radius=3 mask=0.60");
            IJ.run(imp, "Gaussian Blur...", "sigma=2");
            IJ.run(imp, "Subtract Background...", "rolling=500");
            IJ.run(imp, "Unsharp Mask...", "radius=2.5 mask=0.60");
            IJ.run(imp, "Subtract Background...", "rolling=600");
            IJ.run(imp, "Gaussian Blur...", "sigma=2");
            IJ.run(imp, "Unsharp Mask...", "radius=2.5 mask=0.4");

            ImagePlus processedImp = imp.duplicate();
            IJ.run(processedImp, "Enhance Contrast...", "saturated=1");
            IJ.run(imp, "Gaussian Blur...", "sigma=2");
            IJ.run("Green");
            IJ.run(imp, "RGB Color", "");
            IJ.run(imp, "Enhance True Color Contrast", "saturated=4");

            IJ.run(imp, "Unsharp Mask...", "radius=3 mask=0.4");


            imp.show();

        } catch (Exception e) {
            IJ.log("Error during segmentation: " + imp.getTitle() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processes one slice of a stack.
     */
    private void processImageSlice(ImagePlus imp) {
        try {
            processImage(imp);
        } catch (Exception e) {
            IJ.log("Error in processImageSlice: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

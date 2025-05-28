package org.ImplantoMetrics;

import ij.IJ;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import javax.swing.JOptionPane;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import com.google.gson.*;

public class IF_module_45 implements PlugIn {

    private final List<Double> rawIFHistory = new ArrayList<>();
    private final Map<String, Double> paramVals = new HashMap<>();
    private final Map<String, Double> featureVals = new HashMap<>();
    private String classification = "A";

    @Override
    public void run(String arg) {
        int timeH = askTime();

        readParameters();
        readFeatures();
        SHAPExtraction shap = readSHAP(timeH);

        double rawIF = calculateImplantationFactor(
                shap.getExtractedSHAPValuesWithName().values().stream()
                        .mapToDouble(Double::doubleValue).toArray(),
                extractMeasuredParams(),
                extractInteractionValues(
                        shap.getExtractedSHAPValuesWithName(),
                        Math.max(featureVals.size(), 1)
                ),
                featureVals.values().stream().mapToDouble(Double::doubleValue).toArray(),
                timeH
        );

        rawIFHistory.add(rawIF);

        if (rawIFHistory.size() >= 4) {
            double s0 = rawIFHistory.get(rawIFHistory.size() - 4);
            double s3 = rawIFHistory.get(rawIFHistory.size() - 1);
            double slope = (s3 - s0) / 3.0;
            classification = slope < 0 ? "D" : "E";
        } else {
            classification = "A";
        }

        double calIF = TimeSigmoidCalibrator.apply(rawIF, timeH, classification);

        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Time_h", timeH);
        rt.addValue("Invasion Factor", calIF);
        rt.show("Results");
    }

    private int askTime() {
        String in = JOptionPane.showInputDialog("Please enter Time [h] (0–143):");
        try {
            int t = Integer.parseInt(in);
            if (t >= 0 && t <= 143) return t;
        } catch (Exception ignored) {}
        JOptionPane.showMessageDialog(null, "Value must be an integer between 0 and 143.", "Error", JOptionPane.ERROR_MESSAGE);
        throw new RuntimeException("invalid time input");
    }

    private String normalizeName(String c) {
        if (c == null) return "";
        String s = c.trim().toLowerCase();

        if (s.equals("cell radius") || s.equals("radius") || s.equals("spheroid size")) {
            return "spheroid radius";
        } else if (s.equals("area") || s.equals("spheroid area") || s.equals("cell area")) {
            return "total spheroid and cell projections area";
        } else if (s.equals("migration radius") || s.equals("invasion radius") || s.equals("migration/invasion radius")) {
            return "the migration/invasion radius";
        } else if (s.equals("distribution of migration") || s.equals("distribution of invasion")) {
            return "the distribution of migration/invasion";
        } else if (s.equals("number of projections") || s.equals("projection count") || s.equals("cell projections")) {
            return "the number of cell projections";
        } else {
            return c;
        }
    }



    private void readParameters() {
        paramVals.clear();
        new ParameterButonom().run("");
        ParameterButonom.getLastResults()
                .forEach((k, v) -> paramVals.put(normalizeName(k), (double) v));
    }

    private void readFeatures() {
        featureVals.clear();
        try {
            FeatureExtene f = new FeatureExtene();
            f.run("");
            Map<String, Double> ext = f.getExtractedFeaturesWithName();
            if (ext != null) featureVals.putAll(ext);
        } catch (Exception e) {
            IJ.log("Feature extraction failed: " + e);
        }
    }

    private SHAPExtraction readSHAP(int t) {
        SHAPExtraction s = new SHAPExtraction();
        s.setTime(t);
        s.run("");
        return s;
    }

    private double[] extractMeasuredParams() {
        String[] keys = {
                "spheroid radius",
                "total spheroid and cell projections area",
                "the migration/invasion radius",
                "the number of cell projections"
        };
        return paramVals.entrySet().stream()
                .filter(e -> Arrays.asList(keys).contains(e.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .toArray();
    }

    private Map<String, Integer> createIdx() {
        String[] names = {
                "spheroid radius",
                "total spheroid and cell projections area",
                "the migration/invasion radius",
                "the number of cell projections",
                "circularity"
        };
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < names.length; i++) map.put(names[i], i);
        return map;
    }

    private double[][] extractInteractionValues(Map<String, Double> shap, int featCnt) {
        double[][] mat = new double[featCnt][featCnt];
        Map<String, Integer> idx = createIdx();
        for (Map.Entry<String, Double> e : shap.entrySet()) {
            String key = e.getKey();
            if (!key.contains("-Feature_")) continue;
            String[] p = key.split("-Feature_");
            if (p.length != 2) continue;
            Integer i = idx.get(normalizeName(p[0].trim()));
            int j;
            try {
                j = Integer.parseInt(p[1]) - 1;
            } catch (Exception ex) {
                continue;
            }
            if (i != null && j < featCnt) {
                mat[i][j] = mat[j][i] = e.getValue();
            }
        }
        return mat;
    }

    private double calculateImplantationFactor(
            double[] shapValues,
            double[] measuredParams,
            double[][] interactionValues,
            double[] selectedFeatures,
            int currentInterval
    ) {
        double numerator = 0, denominator = 0;
        double maxSHAP = Math.max(Arrays.stream(shapValues).map(Math::abs).max().orElse(1.0), 1e-3);
        double w = 2.0;
        for (int i = 0; i < measuredParams.length; i++) {
            double rawSh = shapValues[i];
            double norm = Math.abs(rawSh) / maxSHAP;
            if (norm < 1e-3) norm = 1e-3;
            double amp = w * Math.pow(norm, 2);
            double sign = rawSh >= 0 ? 1 : -1;
            numerator += sign * amp * measuredParams[i];
            denominator += Math.pow(measuredParams[i], 2) + Math.pow(amp, 2);
        }
        return numerator / Math.sqrt(Math.max(denominator, 1e-9));
    }

    private static final class TimeSigmoidCalibrator {
        static class Model {
            double[] p;
            double med;
            double iqr;
        }

        private static final List<Integer> TIME_GRID = new ArrayList<>();
        private static final List<String> GROUPS = Arrays.asList("A", "D", "E");

        private static final Map<String, Map<Integer, Model>> MODEL_MAP = new HashMap<>();

        static {
            load("/sigmoid_global_v11.2.json");
        }

        static void load(String resource) {
            try (InputStream in = TimeSigmoidCalibrator.class.getResourceAsStream(resource)) {
                if (in == null) throw new RuntimeException("Model file not found: " + resource);
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, "UTF-8")).getAsJsonObject();

                double[] params = new double[]{
                        root.getAsJsonObject("params").get("b0").getAsDouble(),
                        root.getAsJsonObject("params").get("b1").getAsDouble(),
                        root.getAsJsonObject("params").get("k").getAsDouble(),
                        root.getAsJsonObject("params").get("x0").getAsDouble()
                };

                List<Double> medA = new ArrayList<>();
                for (JsonElement e : root.getAsJsonArray("med_A")) medA.add(e.getAsDouble());
                List<Double> iqrA = new ArrayList<>();
                for (JsonElement e : root.getAsJsonArray("iqr_A")) iqrA.add(e.getAsDouble());
                for (JsonElement e : root.getAsJsonArray("time_grid")) TIME_GRID.add(e.getAsInt());

                JsonArray deltaArray = root.getAsJsonArray("Δ");

                for (int g = 0; g < GROUPS.size(); g++) {
                    String group = GROUPS.get(g);
                    Map<Integer, Model> map = new HashMap<>();
                    JsonArray deltaGrp = deltaArray.get(g).getAsJsonArray();
                    int len = Math.min(Math.min(medA.size(), iqrA.size()), TIME_GRID.size());
                    int deltaLen = deltaGrp.size();
                    for (int t = 0; t < Math.min(len, deltaLen); t++) {

                        Model m = new Model();
                        m.p = params;
                        m.med = medA.get(t);
                        m.iqr = iqrA.get(t);
                        double d = deltaGrp.get(t).getAsDouble();
                        m.p = new double[]{params[0] + d, params[1], params[2], params[3]};
                        map.put(TIME_GRID.get(t), m);
                    }
                    MODEL_MAP.put(group, map);
                }
            } catch (Exception e) {
                IJ.log("[SigmoidCalibrator] Fehler beim Laden des Modells: " + e);
            }
        }

        static double apply(double rawIF, int timeH, String group) {
            Integer closest = TIME_GRID.stream()
                    .min(Comparator.comparingInt(t -> Math.abs(t - timeH)))
                    .orElse(null);
            if (closest == null) return rawIF;

            Map<Integer, Model> groupModels = MODEL_MAP.getOrDefault(group, Collections.emptyMap());
            Model m = groupModels.get(closest);
            if (m == null || m.iqr == 0.0) return rawIF;

            double z = (rawIF - m.med) / m.iqr;
            double arg = -m.p[2] * (z - m.p[3]);
            arg = Math.max(Math.min(arg, 700), -700);
            return m.p[0] + m.p[1] / (1.0 + Math.exp(arg));
        }

    }
}

/**
 * SHAPExtraction - Module for the extraction and visualization of SHAP values.
 *
 * This module is part of the ImplantoMetrics plugin and is used to analyze the significance of
 * parameters and features in the context of embryo implantation or similar biological processes.
 * It extracts SHAP (SHapley Additive exPlanations) values from CSV files representing different time intervals
 * and visualizes the results for further analysis.
 *
 * Main tasks of this module:
 * - Loading SHAP values from CSV files representing two models (Model A and Model B)
 *   and for interactions between features.
 * - Processing and filtering of SHAP values based on a time interval entered by the user.
 * - Visualization of the extracted SHAP values in a ResultsTable, which provides a quick
 *   overview of the meaning of different parameters and features.
 *
 * Application:
 * - Embryo implantation research: analyzing the dynamics and importance of individual
 *   parameters over different time intervals.
 * - Other biological contexts in which the interactions between several traits and their
 *   temporal significance are investigated.
 */

package org.ImplantoMetrics;

import ij.IJ;
import ij.plugin.PlugIn;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import ij.measure.ResultsTable;

import javax.swing.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class SHAPExtraction implements PlugIn {

    private Map<String, Double> extractedSHAPValuesWithName = new HashMap<>();
    private int time = -1; // user-specified time point in hours (0-143)

    public Map<String, Double> getExtractedSHAPValuesWithName() {
        return extractedSHAPValuesWithName;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public void run(String arg) {
        // Prompt user for time input if not yet defined
        if (time == -1) {
            int hour = getUserInput();
            if (hour == -1) return;
            this.time = hour;
        }

        // Determine which time interval to evaluate based on input
        int timeIntervalStart = (time / 8) * 8;
        int timeIntervalEnd = timeIntervalStart + 8;

        try {
            // Load SHAP values for two models and interaction effects
            List<String[]> shapValuesModelA = loadCSV("Model-A.csv", ';');
            List<String[]> shapValuesModelB = loadCSV("Model-B.csv", ';');
            List<String[]> interactionShapValues = loadCSV("interaction_intervals.csv", ',');

            // Process and display each dataset
            processAndDisplayData(shapValuesModelA, timeIntervalStart, timeIntervalEnd);
            processAndDisplayData(shapValuesModelB, timeIntervalStart, timeIntervalEnd);
            processAndDisplayData(interactionShapValues, timeIntervalStart, timeIntervalEnd);

            // Visualize results in FIJI table
            visualizeResults();

        } catch (Exception e) {
            IJ.error("Processing Error", e.getMessage());
            e.printStackTrace();
        }
    }

    // Prompt dialog for user to enter time in hours (0–143)
    private int getUserInput() {
        String input = JOptionPane.showInputDialog("Please enter the Time (0-143h):");
        if (input == null || input.isEmpty()) {
            return -1;
        }
        try {
            int hour = Integer.parseInt(input);
            if (hour < 0 || hour > 143) {
                JOptionPane.showMessageDialog(null, "Invalid hour entry. Please enter a value between 0 and 143.", "Error", JOptionPane.ERROR_MESSAGE);
                return -1;
            }
            return hour;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "The hour entered is not a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
    }

    // Wrapper to process values from a SHAP CSV list
    private void processAndDisplayData(List<String[]> shapValues, int timeIntervalStart, int timeIntervalEnd) {
        extractAndPrintValues(shapValues, timeIntervalStart, timeIntervalEnd);
    }

    // Core logic to extract SHAP values in a specified interval
    private void extractAndPrintValues(List<String[]> shapValues, int timeIntervalStart, int timeIntervalEnd) {
        String[] headers = shapValues.get(0); // First row: column names
        for (int i = 1; i < shapValues.size(); i++) {
            String[] row = shapValues.get(i);
            Integer timeValue = extractTimeValue(row[0]);
            if (timeValue != null && timeValue >= timeIntervalStart && timeValue < timeIntervalEnd) {
                for (int j = 1; j < row.length; j++) {
                    try {
                        String valueStr = row[j];
                        if (valueStr.contains(":")) {
                            valueStr = valueStr.split(":"[1].trim());
                        }
                        double value = Double.parseDouble(valueStr);
                        String columnName = headers[j];
                        extractedSHAPValuesWithName.put(columnName, value);
                    } catch (NumberFormatException e) {
                        System.err.println("Parsing error at: " + row[j] + " (row " + (i + 1) + ", col " + (j + 1) + ")");
                    }
                }
            }
        }

        // Optional console debug print
        for (Map.Entry<String, Double> entry : extractedSHAPValuesWithName.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    // Show results in ImageJ ResultsTable
    private void visualizeResults() {
        ResultsTable rt = new ResultsTable();

        for (Map.Entry<String, Double> entry : extractedSHAPValuesWithName.entrySet()) {
            rt.incrementCounter();
            rt.addValue("Parameter", entry.getKey());
            rt.addValue("Value", entry.getValue());
        }

        //rt.show("Extracted SHAP Values");
    }

    // Extract numerical time value from a CSV entry (e.g., "24-32")
    private static Integer extractTimeValue(String timeString) {
        try {
            return Integer.parseInt(timeString.split("-")[0].trim());
        } catch (NumberFormatException e) {
            System.err.println("Time parsing error: " + timeString);
            return null;
        }
    }

    // Load CSV resource from the project with a specified delimiter
    private static List<String[]> loadCSV(String resourceName, char separator) throws Exception {
        CSVParser csvParser = new CSVParserBuilder().withSeparator(separator).build();
        try (InputStream inputStream = SHAPExtraction.class.getClassLoader().getResourceAsStream(resourceName);
             CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream))
                     .withSkipLines(0)
                     .withCSVParser(csvParser)
                     .build()) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            return csvReader.readAll();
        }
    }

    // Normalize column/parameter names for consistent naming across modules
    private String normalizeParameterName(String columnName) {
        if (columnName.equalsIgnoreCase("Cell Radius")) return "spheroid radius";
        if (columnName.equalsIgnoreCase("Area")) return "total spheroid and cell projections area";
        if (columnName.equalsIgnoreCase("Migration Radius")) return "the migration/invasion radius";
        if (columnName.equalsIgnoreCase("Distribution of Migration")) return "the distribution of migration/invasion";
        if (columnName.equalsIgnoreCase("Number of Projections")) return "the number of cell projections";
        return columnName;
    }
}

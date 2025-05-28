

package org.ImplantoMetrics;

import ij.IJ;
import ij.plugin.PlugIn;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;



/**
 * ImplantoMetrics Plugin for FIJI

 * This FIJI plugin, ImplantoMetrics, is designed to enhance the analysis of embryo implantation
 * by providing a suite of tools for segmentation, binary module processing, and visualization
 * of implantation data. The main features include color channel selection for image analysis,
 * modules for segmentation and binary processing to facilitate image analysis, and a variety of
 * visualization tools such as projections, thickness analysis, and a 3D viewer to assist researchers
 * in studying the implantation process more effectively. The plugin supports green, red, and blue color
 * channels to accommodate different staining techniques used in microscopy images related to embryo
 * implantation studies.

 * Developed by Ayberk Alp Gyunesh

 * This method initializes the GUI for the ImplantoMetrics plugin, providing an interactive
 * and user-friendly interface. Users are presented with a variety of buttons that allow them
 * to access different modules of the plugin, each tailored for specific aspects of implantation
 * analysis.

 * The interface includes:
 * - A color channel selection drop-down to adapt the analysis to images captured in different
 *   color channels (Red, Green, Blue), enhancing flexibility in processing diverse image datasets.
 * - Segmentation Module: For preprocessing images to isolate the regions of interest, facilitating
 *   a more focused analysis.
 * - Binary Module: Converts images into binary format for further analysis, crucial for
 *   segmentation and feature extraction.
 * - Projections, Thickness, and 3D Viewer buttons: These visualization tools offer users the
 *   ability to examine the data in various formats, providing insights into the spatial
 *   distribution and physical characteristics of the sample.
 * - Parameter and Invasion Factor buttons: These features allow for the quantitative
 *   analysis of the images, assessing various parameters and computing the invasion factor
 *   which is pivotal for evaluating the success of implantation.
 *
 * Note: Ensure that the FIJI software is updated to the latest version for optimal compatibility
 * and performance of the ImplantoMetrics plugin.
 */




public class ImplantoMetrics implements PlugIn {

    public static String selectedChannel = "Green";

    @Override
    public void run(String arg) {
        SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    private void createAndShowGUI() {

        JFrame frame = new JFrame("ImplantoMetrics");
        frame.setSize(550, 650);
        frame.setLayout(new BorderLayout(10, 10));
        ((JComponent) frame.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));


        // Channel selection panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel channelLabel = new JLabel("Choose Color Channel:");
        JComboBox<String> channelComboBox = new JComboBox<>(new String[]{"Red", "Green", "Blue"});
        channelComboBox.setSelectedItem(selectedChannel);
        channelComboBox.addActionListener(e -> selectedChannel = (String) channelComboBox.getSelectedItem());
        topPanel.add(channelLabel);
        topPanel.add(channelComboBox);

        frame.add(topPanel, BorderLayout.NORTH);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);




        JButton segmentationButton = new JButton("Segmentation Module");
        segmentationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Class<?> moduleClass = Class.forName("org.ImplantoMetrics.Segmentation_module_5");
                    PlugIn moduleInstance = (PlugIn) moduleClass.getDeclaredConstructor().newInstance();
                    moduleInstance.run("");



                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                    IJ.log("Error loading or starting module: " + ex.getMessage());
                }
            }
        });
        buttonPanel.add(segmentationButton);








        JButton binaryButton = new JButton("Binary Module");
        binaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("Binary Module Button clicked");
                try {
                    Class<?> moduleClass = Class.forName("org.ImplantoMetrics.BinaryModul");
                    PlugIn moduleInstance = (PlugIn) moduleClass.getDeclaredConstructor().newInstance();
                    moduleInstance.run("");
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                    IJ.log("Error loading or starting module: " + ex.getMessage());
                }
            }
        });
        buttonPanel.add(binaryButton);

        // Visualisation Panel
        JPanel visualisationPanel = new JPanel();
        visualisationPanel.setBorder(BorderFactory.createTitledBorder("Visualisation"));
        visualisationPanel.setLayout(new GridLayout(3, 1));

        // Projections Button
        JButton projectionsButton = new JButton("Projections");
        projectionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    Class<?> moduleClass = Class.forName("org.ImplantoMetrics.ProjectionModul");
                    PlugIn moduleInstance = (PlugIn) moduleClass.getDeclaredConstructor().newInstance();
                    moduleInstance.run("");

                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                    IJ.log("Error loading or starting Projections Module: " + ex.getMessage());
                }
            }
        });
        visualisationPanel.add(projectionsButton);



        JButton thicknessButton = new JButton("Thickness");
        thicknessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("Thickness Module Button clicked");
                try {
                    Class<?> moduleClass = Class.forName("org.ImplantoMetrics.ThicknessModul");
                    PlugIn moduleInstance = (PlugIn) moduleClass.getDeclaredConstructor().newInstance();
                    moduleInstance.run("");
                    IJ.log("Thickness Module was started.");
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                    IJ.log("Error loading or starting Thickness Module: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        visualisationPanel.add(thicknessButton);




        JButton viewer3DButton = new JButton("3D Viewer");
        viewer3DButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("3D Viewer Module Button clicked");
                try {

                    Class<?> moduleClass = Class.forName("org.ImplantoMetrics.D3Viewer");
                    PlugIn moduleInstance = (PlugIn) moduleClass.getDeclaredConstructor().newInstance();
                    moduleInstance.run("");
                    IJ.log("3D Viewer Module was started.");
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                    IJ.log("Error loading or starting 3D Viewer Module: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        visualisationPanel.add(viewer3DButton);








        frame.add(buttonPanel, BorderLayout.WEST);
        frame.add(visualisationPanel, BorderLayout.EAST);



        JButton renderButton = new JButton("Parameter");
        renderButton.setBackground(new Color(128, 0, 128));
        renderButton.setForeground(Color.WHITE);
        renderButton.setFont(new Font("Arial", Font.BOLD, 16));
        renderButton.setToolTipText("Click to render with selected parameters");
        renderButton.setOpaque(true);
        renderButton.setBorderPainted(false);
        renderButton.setPreferredSize(new Dimension(200, 60));
        renderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("Parameter Module Button clicked");
                try {
                    Class<?> moduleClass = Class.forName("org.ImplantoMetrics.ParameterButonom");
                    PlugIn moduleInstance = (PlugIn) moduleClass.getDeclaredConstructor().newInstance();
                    moduleInstance.run("");

                    Map<String, Float> results = RenderModulen.getLastResults();

                    IJ.log("\\Close");
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                    IJ.log("Error loading or starting module: " + ex.getMessage());
                }
            }
        });


        JPanel renderButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        renderButtonPanel.add(renderButton);
        frame.add(renderButtonPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);


        // Erstellen des ImplantationFactor-Buttons
        JButton implantationFactorButton = new JButton("Invasion Factor");
        implantationFactorButton.setBackground(new Color(128, 0, 128));
        implantationFactorButton.setForeground(Color.WHITE);
        implantationFactorButton.setFont(new Font("Arial", Font.BOLD, 16));
        implantationFactorButton.setToolTipText("Click to execute Implantation Factor module");
        implantationFactorButton.setOpaque(true);
        implantationFactorButton.setBorderPainted(false);
        implantationFactorButton.setPreferredSize(new Dimension(200, 60));

        implantationFactorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {

                    Class<?> moduleClass = Class.forName("org.ImplantoMetrics.IF_module_45");
                    PlugIn moduleInstance = (PlugIn) moduleClass.getDeclaredConstructor().newInstance();


                    moduleInstance.run("");

                } catch (Exception e) {
                    System.err.println("Ein Fehler ist aufgetreten: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });








        JPanel implantationFactorButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        implantationFactorButtonPanel.add(implantationFactorButton);


        JPanel combinedButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        combinedButtonPanel.add(renderButtonPanel);
        combinedButtonPanel.add(implantationFactorButtonPanel);

        frame.add(combinedButtonPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);



    }



    public static void main(String[] args) {
        new ImplantoMetrics().run("");
    }
}

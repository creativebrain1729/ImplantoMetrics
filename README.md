
# ImplantoMetrics Plugin

## Introduction

**ImplantoMetrics** is a comprehensive plugin for Fiji (an ImageJ distribution), specifically designed to facilitate the quantitative analysis of implantation processes using a 3D in vitro model. The plugin leverages advanced image processing, machine learning algorithms, and statistical methods to offer deep insights into embryo implantation and related biological processes, such as cancer research.

ImplantoMetrics assesses critical parameters, including spheroid radius, total spheroid and cell projections area, migration/invasion radius, number of cell projections, distribution of migration/invasion, and circularity. These parameters are automatically extracted and analyzed from microscopy images, significantly enhancing both efficiency and accuracy over manual methods.

## Installation

### Prerequisites

- **Fiji (ImageJ)**: Ensure that Fiji is installed on your system. Fiji can be downloaded from the official [Fiji website](https://fiji.sc/).
- **Java**: Fiji requires Java. Make sure that Java is installed and correctly configured on your computer.

### Installing the ImplantoMetrics Plugin

1. **Download the ImplantoMetrics Plugin:**
   - Download the `ImplantoMetrics` plugin from the official website or GitHub repository.
   - Unzip the file and copy the `.jar` file into the `plugins` directory of your Fiji installation.

2. **Download and Install the BioVoxxel Plugin:**
   - ImplantoMetrics requires the BioVoxxel Toolbox plugin for advanced image processing and analysis.
   - To install the BioVoxxel plugin:
     - Open Fiji and go to `Help` > `Update...`.
     - Select `Manage Update Sites`.
     - Enable the `BioVoxxel` update site.
     - Click `Close` and then `Apply Changes` to complete the installation.
   - Alternatively, you can directly download the BioVoxxel plugin from the [BioVoxxel Website](https://biovoxxel.com/downloads/) and install it manually.

3. **Restart Fiji:**
   - After installing the plugins, restart Fiji to apply the changes.

## Usage Instructions

### Step 1: Preparing and Uploading Images

1. **Image Acquisition:**
   - Images should be acquired at regular intervals (e.g., every 8 or 12 hours) using a fluorescence microscope or similar imaging system. These images should capture the implantation process in your 3D in vitro model.

2. **Uploading Images to Fiji:**
   - Open Fiji and navigate to `File` > `Open...` to upload your acquired images.

### Step 2: Running the ImplantoMetrics Plugin

1. **Launching the Plugin:**
   - Go to `Plugins` > `ImplantoMetrics` to start the plugin.

2. **Selecting the Color Channel:**
   - Specify the color channel for analysis (default is green). This is crucial for correct segmentation.

3. **Image Segmentation:**
   - Select the 'Segmentation Module' to initiate fluorescence image segmentation. The plugin applies a series of image processing algorithms, including background correction, contrast enhancement, unsharp masking, and Gaussian blurring, to optimize image quality for segmentation. **It is essential to ensure that the images are of high quality, as this directly impacts the accuracy and success of the segmentation process.**

4. **Binary Conversion:**
   - Select the 'Binary Module'. This module binarizes the image, converting it into a two-color format, which is essential for identifying regions of interest (ROI) in the image .

### Step 3: Parameter Analysis

1. **Parameter Extraction:**
   - Click the 'Parameter' button to upload the pre-trained `ImplantationMetrics model`. This model will predict six critical parameters related to invasion success  .

2. **Calculating the Invasion Factor:**
   - Select the 'Invasion Factor' button, then enter the acquisition time of the image. The plugin calculates the invasion factor based on the predicted parameters and extracted features, representing the probability of successful invasion .

### Step 4: Visualization and Results

1. **Visualization Options:**
   - ImplantoMetrics offers integrated visualization methods, including Projections, Thickness, and a 3D Viewer. These tools allow you to visualize the distribution and structural characteristics of the spheroids, providing deeper insights into the implantation process .

2. **Saving and Exporting Results:**
   - After analysis, the results are displayed in a `ResultsTable`. You can save or export these results for further study or presentation.

##  Support


- For questions or issues, please contact developer support through the provided contact information.



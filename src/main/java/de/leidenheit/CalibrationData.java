package de.leidenheit;

import java.util.ArrayList;

import org.opencv.core.Mat;

public record CalibrationData(
    Mat meanMatrix, 
    Mat meanDistortionCoefficients, 
    // Mat uncertaincyMatrix, 
    // Mat uncertaincyDistortionCoefficients,
    ArrayList<Mat> rVectors,
    ArrayList<Mat> tVectors,
    double avgReprojectionErrors,
    boolean calibrationValid) {
}
package de.leidenheit;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Core;

public record CalibrationData(
    Mat cameraMatrix, 
    Mat distortionCoefficients, 
    ArrayList<Mat> rVectors,
    ArrayList<Mat> tVectors,
    double avgReprojectionErrors) {

    /**
     * Determines if the the camera matrix and 
     * the distortion coefficients are in a valid range.
     * 
     * @return Returns true if valid, otherwise false. 
     */
    public boolean isCalibrationValid() {
        return Core.checkRange(this.cameraMatrix) 
            && Core.checkRange(this.distortionCoefficients);
    } 
}
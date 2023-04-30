package de.leidenheit;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Core;

public record CalibrationData(
    Mat meanMatrix, 
    Mat meanDistortionCoefficients, 
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
        return Core.checkRange(this.meanMatrix) 
            && Core.checkRange(this.meanDistortionCoefficients);
    } 
}
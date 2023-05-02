package de.leidenheit;

import org.opencv.core.Scalar;

public record ContourParameter(
    double gaussFactor,         // 11
    double cannyThresholdLow,   // 25
    double cannyThresholdHigh,  // 75
    int dilateIterations,       // 1
    int erodeIterations,        // 1
    double areaThreshold,       // 100
    double epsilon,             // 0.01
    Scalar drawColorBGRA,       // 31, 240, 255
    int drawThickness           // 3
) {
    
    /**
     * Instantiates a {@link ContourParameter} with the following parameters:
     *  - gauss = 11
     *  - canny threshold = low 100, high 150
     *  - dilate iterations = 6
     *  - erode iterations = 2
     *  - area threshold = 100
     *  - epsilon = 0.01
     *  - contour colour = neon yellow
     *  - contour thickness = 3 
     * 
     * @return Returns an instance of {@link ContourParameter} with default parameters
     */
    public static ContourParameter defaultParameter() {
        return new ContourParameter(
            11, 
            100, 
            150, 
            6, 
            2, 
            100, 
            0.01, 
            new Scalar(31, 240, 255), 
            3);
    }
}

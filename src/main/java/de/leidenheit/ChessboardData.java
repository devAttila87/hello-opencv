package de.leidenheit;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;

public record ChessboardData(
    List<Mat> objectPoints,
    List<Mat> imagePoints
) {
    
    /**
     * Instantiates a {@link ChessboardData} with empty 
     * objectPoints and imagePoints. 
     * @return {@link ChessboardData}
     */
    public static ChessboardData init() {
        return new ChessboardData(
            new ArrayList<Mat>(),
            new ArrayList<Mat>());
    }
}

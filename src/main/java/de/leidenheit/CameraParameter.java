package de.leidenheit;

public record CameraParameter(
    int cameraResolutionWidth,
    int cameraResolutionHeight,
    int calibrationPatternWidth,
    int calibrationPatternHeight,
    double calibrationChessboardSquareSizeInMillimeter,
    double scaleFactor
) {

    /** Provides a instance of {@link CameraParameter} with the following preset values:
     * - camera resolution at 1080p 
     * - 54 corners in calibration chessboard pattern
     * - 0.5 scaling factor
     * - 40mm square size 
     *
     * @return {@link CameraParameter}
     */
    public static CameraParameter defaultParameter() {
        return new CameraParameter(
            1920, 
            1446, 
            9, 
            6, 
            40d, 
            0.5d);
    } 
}

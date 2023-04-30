package de.leidenheit;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.calib3d.Calib3d;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import java.util.logging.Logger;

import javax.swing.JPanel;

public final class DetectionUtil {

    private static final Logger LOGGER = Logger.getLogger("DetectionUtil");

    /** 
     * Distorts a image using given calibration information.
     * 
     * @param imageFilePath
     * @param cameraParameter
     * @param calibrationData
     * @return
     */
    public static Mat distortFunction(
        final String imageFilePath, 
        final CameraParameter cameraParameter,
        final CalibrationData calibrationData) {
        // reduce distortion in images
        // debug code: test with single image
        //final var files = new File("src/resources/chessboard/1080p");
        //final String imageFilePath = List.of(files.list()).stream()
        //    .map(fileName -> files.getAbsolutePath() + "/" + fileName)
        //    .findFirst()
        //    .orElse(null);
        LOGGER.info("Reducing distortion of image " + imageFilePath);
        final var dgbUndistortedImageMat = new Mat();
        final var dgbImageMat = Imgcodecs.imread(imageFilePath);
        // LOGGER.info("\n#########\n\tDistortion Coefficients: " + mDistortionCoefficients.dump());
        // LOGGER.info("\n#########\n\tCamera Matrix: " + mCameraMatrix.dump());
        // removes unwanted pixels from matrix and returns ROI
        final var optimalMatrix = Calib3d.getOptimalNewCameraMatrix(
            calibrationData.meanMatrix(),
            calibrationData.meanDistortionCoefficients(),
            dgbImageMat.size(), 
            1, 
            dgbImageMat.size());
        //LOGGER.info("\n#########\n\tOptimal Camera Matrix: " + optimalMatrix.dump()  
        //    + "\nROI=" + roi);

        Calib3d.undistort(
            dgbImageMat, 
            dgbUndistortedImageMat, 
            calibrationData.meanMatrix(), 
            calibrationData.meanDistortionCoefficients(),
            optimalMatrix);

        // resize
        Imgproc.resize(dgbImageMat, dgbImageMat, 
            new Size(
                dgbImageMat.width()*cameraParameter.scaleFactor(), 
                dgbImageMat.height()*cameraParameter.scaleFactor()));        
        Imgproc.resize(dgbUndistortedImageMat, dgbUndistortedImageMat, 
            new Size(
                dgbUndistortedImageMat.width()*cameraParameter.scaleFactor(), 
                dgbUndistortedImageMat.height()*cameraParameter.scaleFactor()));
        HighGui.imshow("before_dist", dgbImageMat);
        HighGui.waitKey(20);
        HighGui.destroyWindow("before_dist");
        HighGui.imshow("after_dist", dgbUndistortedImageMat);
        HighGui.waitKey(20);
        HighGui.destroyWindow("after_dist");
        
        return dgbUndistortedImageMat;
    } 

    /**
     * Draw given {@link Mat} image to a given {@link JPanel}.
     *
     * @param mat Image to draw.
     * @param panel Panel on which to draw image.
     */
    public static void drawImageToPanel(final Mat mat, final JPanel panel) {
        final BufferedImage bufferedImage = new BufferedImage(
                mat.width(),
                mat.height(),
                mat.channels() == 1 
                    ? BufferedImage.TYPE_BYTE_GRAY 
                    : BufferedImage.TYPE_3BYTE_BGR);
        mat.get(0, 0, ((DataBufferByte) bufferedImage
            .getRaster()
            .getDataBuffer())
            .getData());
        final Graphics graphics = panel.getGraphics();
        graphics.drawImage(bufferedImage, 0, 0, panel);
    }

    /**
     * Uses {@link HighGui.imshow} to present an image resized into 640x480 pixels.
     * for debugging purposes.
     * 
     * @param matImage
     * @param windowName
     */
    private static void debugShowImage(final Mat matImage, final String windowName) {
        final Size dSize = new Size(640, 480);
        final Mat matResized = new Mat();
        Imgproc.resize(matImage, matResized, dSize);
        HighGui.imshow(windowName, matResized);
        HighGui.waitKey(20);
        HighGui.destroyWindow(windowName);
    }

    private DetectionUtil() {
        // hide constructor
    }
}

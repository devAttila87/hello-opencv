package de.leidenheit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.bilateralFilter;

public final class DetectionUtil {

    private static final Logger LOGGER = Logger.getLogger("DetectionUtil");

    // calibration variables
    private static final CameraCalibrator camCalibrator = new CameraCalibrator(640, 480);


    private static final Size patternSize = new Size(6, 9);
    public static final int cornersSize = (int) (patternSize.width * patternSize.height);
    private static final boolean patterWasFound = false;
    private static final List<Mat> cornersBuffer = new ArrayList<>();
    private static final MatOfPoint2f mCorners = new MatOfPoint2f();
    private static boolean isCalibrated = false;

    private static final Mat cameraMatrix = new Mat();

    private static final Mat distortionCoefficients = new Mat();
    private static final double squareSize = 30; // mm


    private DetectionUtil() {
        // hide constructor
    }

    /**
     * Draw forwarded mat image to forwarded panel.
     *
     * @param mat Image to draw.
     * @param panel Panel on which to draw image.
     */
    public static void drawImage(final Mat mat, final JPanel panel) {
        final BufferedImage bufferedImage = new BufferedImage(
                mat.width(),
                mat.height(),
                mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR);
        mat.get(0, 0, ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData());
        final Graphics graphics = panel.getGraphics();
        graphics.drawImage(bufferedImage, 0, 0, panel);
    }

    /**
     * Used to process forwarded {@link Mat} image and return the result.
     *
     * @param mat Image to process.
     * @return Returns processed image.
     */
    public static Mat processImage(final Mat mat) {
        final Mat processed = new Mat(mat.height(), mat.width(), mat.type());


        return processed;
    }


    /**
     * TODO calibrates a camera
     *  The image is converted to grayscale.
     *  The corners of the chessboard pattern are found with the function "findChessboardCorners". When corners are found, they are optimized with the "cornerSubPix" function and added to the lists of object points and image points.
     *  The optimized corners are displayed on the image and the image is saved if the corresponding conditions are met.
     *  If no corners were found, a corresponding message is displayed.
     */

    // todo just a check
    private static void alternateTryCalibrate(Mat grayFrame, Mat rbgaFrame) {
       // camCalibrator.processFrame(grayFrame, rbgaFrame);
        camCalibrator.addCorners();
        /* TODO
        if (camCalibrator.getCornersBufferSize() > 0)
            camCalibrator.calibrate();
         */

        /* usage idea
        final var record = new CameraCalibrationRecord()
        record.imageSize(640, 480);       // size of the image in pixels to use for calibration
        record.patternSize(9, 6)          // size of the pattern in rows and columns to consider from the chess board
        record.squareSizeInMillimeter(30) // actual size of the chess board squares
        record.setChessBoardCornerFlags(x) // experimental

        // find chessboard corners, optimize and draw them
        final hasCorners = CameraCalibrationUtil.tryFindCorners(
            record, grayFrame, outCorners);
        if (hasCorners) {
            // do subpixel optimization
            ...
            CameraCalibarationUtil.doCornerSubpixelOptimization(
                criteria,
                grayFrame,
                outCorners,
                winSize,
                zeroZone
            );
        }
        drawChessboardCorners(
            rgbaFrame,
            record,
            outCorners,
            hasCorners
        );








        */


    }


//    public static boolean calibrateCamera(final Mat mat) {
//        // TODO better performance with this? yes
//        cornersBuffer.clear();
//
//        // termination criteria for Subpixel Optimization
//        final TermCriteria termCriteria =  new TermCriteria(
//                TermCriteria.EPS + TermCriteria.MAX_ITER,
//                60,
//                0.001);
//
//        // debug original show image
//        debugShowImage(mat);
//
//        // find corners
//        final Mat grayMat = new Mat();
//        Imgproc.cvtColor(mat, grayMat, COLOR_BGR2GRAY);
//        debugShowImage(grayMat);
//
//        // find chess board corners
//        LOGGER.info("Searching for corners...");
//        final Size boardSize = new Size(9, 6);
//        final MatOfPoint2f cornersMat = new MatOfPoint2f();
//        final boolean hasCorners = Calib3d.findChessboardCorners(
//                grayMat,
//                boardSize,
//                cornersMat,
//                // TODO consider using a flag
//                Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK
//                // -1
//                );
//        if (hasCorners) {
//            LOGGER.info("\t\tCorners found!");
//
//            // subpixeloptimizer on original image
//            Imgproc.cornerSubPix(
//                    grayMat,
//                    cornersMat,
//                    new Size(22, 22),
//                    new Size(-1, -1),
//                    termCriteria);
//            // draw and display the corners
//            Calib3d.drawChessboardCorners(
//                    mat,
//                    boardSize,
//                    cornersMat,
//                    true);
//
//            // fill buffer
//            cornersBuffer.add(cornersMat.clone());
//
//            debugShowImage(mat);
//
//            // calibration (see this example: http://www.java2s.com/example/java-src/pkg/de/vion/eyetracking/cameracalib/calibration/opencv/cameracalibrator-973c2.html)
//            // 1. set configuration
//            Mat.eye(3, 3, CvType.CV_64FC1).copyTo(cameraMatrix);
//            cameraMatrix.put(0,0, 1.0);
//            Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(distortionCoefficients);
//            // LOGGER.info("hopefully configured");
//
//            // TODO comment the meaning of the variables
//            final List<Mat> rvecs = new ArrayList<>();
//            final List<Mat> tvecs = new ArrayList<>();
//            final Mat reprojectionErrors = new Mat();
//            final List<Mat> objectPoints = new ArrayList<>();
//
//            objectPoints.add(Mat.zeros(cornersSize, 1, CvType.CV_32FC3));
//
//            // 2. calculate board corner positions
//            final int cn = 3;
//            float[] positions = new float[cornersSize * cn];
//            for (int i = 0; i < patternSize.height; i++) {
//                for (int j = 0; j < patternSize.width * cn; j += cn) {
//                    positions[(int) (i * patternSize.width * cn + j + 0)] = (2 * (j / cn) + i % 2)
//                            * (float) squareSize;
//                    positions[(int) (i * patternSize.width * cn + j + 1)] = i * (float) squareSize;
//                    positions[(int) (i * patternSize.width * cn + j + 2)] = 0;
//                }
//            }
//            objectPoints.get(0).create(cornersSize, 1, CvType.CV_32FC3);
//            objectPoints.get(0).put(0,0, positions);
//            LOGGER.info("calculation of the board corner positions:\n\t" + Arrays.toString(positions));
//
//            // 3. TODO why is this required?
//            LOGGER.info("objectPoints before applying cornerBuffer:\n\t" + objectPoints);
//            for (int i = 1; i < cornersBuffer.size(); i++) {
//                objectPoints.add(objectPoints.get(0));
//            }
//            LOGGER.info("objectPoints after applying cornerBuffer:\n\t" + objectPoints);
//
//            // 4. calibrate
//            final double calibrationResult = Calib3d.calibrateCamera(
//                    objectPoints,
//                    cornersBuffer,
//                    new Size(640, 480),
//                    cameraMatrix,
//                    distortionCoefficients,
//                    rvecs, tvecs
//                    // no flags
//            );
//            LOGGER.info("calibration result:");
//            LOGGER.info("ret=" + calibrationResult);
//            LOGGER.info("dist=" + distortionCoefficients);
//            LOGGER.info("rvecs=" + rvecs);
//            LOGGER.info("tvecs=" + tvecs);
//
//            // 5. check if calibrated
//            isCalibrated = Core.checkRange(cameraMatrix) && Core.checkRange(distortionCoefficients);
//            LOGGER.info("validation -> " + isCalibrated);
//
//        } else {
//            LOGGER.warning("\t\t No corners found!");
//        }
//        return isCalibrated;
//    }
    // todo alternative
    public static boolean calibrateCamera(final Mat rgbaMat) {
        final Mat grayMat = new Mat();
        Imgproc.cvtColor(rgbaMat, grayMat, COLOR_BGR2GRAY);
        // debugShowImage(grayMat);

        alternateTryCalibrate(grayMat, rgbaMat);
        return false;
    }

    private static void debugShowImage(Mat mat) {
        final Size dSize = new Size(640, 480);
        final Mat matResized = new Mat();
        Imgproc.resize(mat, matResized, dSize);
        HighGui.imshow("Test", matResized);
        HighGui.waitKey(20);
        HighGui.destroyWindow("Test");
    }


}

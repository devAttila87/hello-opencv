package de.leidenheit;

import org.opencv.aruco.*;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import static org.opencv.videoio.Videoio.CAP_DSHOW;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

import javax.swing.*;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger("HelloOpenCV");

    public static void main(String[] args) {
        // log everything
        LOGGER.setLevel(Level.INFO);

        // TODO MOVE
        // load openCV
        System.load("/home/leidenheit/hello-opencv/lib/libopencv_java460.so");
        LOGGER.info("OpenCV loaded successfully :-)");

        // create gui   
        
        // cameraFeed, processFeed, labelFilename, labelOperation
        /*
        final JPanel cameraFeed = new JPanel();
        cameraFeed.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final JPanel processedFeed = new JPanel();
        processedFeed.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final JLabel labelOperation = new JLabel("noop");
        final JLabel labelInput = new JLabel("noinput");
        final JPanel feedPanel = new JPanel();
        feedPanel.setSize(new Dimension(1280, 960));
        feedPanel.add(labelInput, BorderLayout.NORTH);
        feedPanel.add(labelOperation,BorderLayout.NORTH);
        feedPanel.add(cameraFeed, BorderLayout.SOUTH);
        feedPanel.add(processedFeed, BorderLayout.SOUTH);
        feedPanel.setVisible(true);

        // overall
        final JFrame window = new JFrame("Hello OpenCV");
        window.setSize(new Dimension(1280, 960));
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.setLayout(new GridLayout(1, 1));
        window.add(feedPanel);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // key listener
        window.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                LOGGER.info("\t\tkeyTyped -> " + e.getKeyCode() + "[" + e.getKeyChar() + "]");
            }

            @Override
            public void keyPressed(KeyEvent e) {
                LOGGER.info("\t\tkeyPressed -> " + e.getKeyCode() + "[" + e.getKeyChar() + "]");
            }

            @Override
            public void keyReleased(KeyEvent e) {
                LOGGER.info("\t\tkeyReleased -> " + e.getKeyCode() + "[" + e.getKeyChar() + "]");
            }
        });
        */

        // create video capture object (index 0 is default camera)
        /*
        final VideoCapture camera = new VideoCapture(1, CAP_DSHOW);
        camera.set(CAP_PROP_FPS, 30);
        camera.set(CAP_PROP_FRAME_WIDTH, 1920);
        camera.set(CAP_PROP_FRAME_HEIGHT, 1080);
        */

        // start computer vision
        Main.startComputerVision(null, null, null).run();

        System.exit(0);
    }

    private static Runnable startComputerVision(final JPanel cameraFeed,
                                                final JPanel processedFeed,
                                                final VideoCapture camera) {
        return () -> {
            // debug
            Scanner scanner = new Scanner(System.in);

            final var resourceProvider = new ResourceProvider();
            final var cameraParameter = CameraParameter.defaultParameter();
            
            CalibrationData calibrationData = null;
            
            final var useExistingCalibration = true;
            if (useExistingCalibration) {
                final var dir = "src/resources/";
                final var cameraMatrixFileName = dir + "camera-matrix.bin"; 
                final var distortionCoeffFileName = dir + "distortion-coefficients.bin";
                final var canLoadCalibration = 
                    Files.exists(Path.of(cameraMatrixFileName))
                    && Files.exists(Path.of(distortionCoeffFileName));
                if (!canLoadCalibration) {
                    LOGGER.warning("Cannot load exsiting calibration; fallback -> calibrate by images...");
                } else {
                    final var mats = CameraCalibrator.loadCalibration(
                        cameraMatrixFileName, 
                        distortionCoeffFileName);
                    calibrationData = new CalibrationData(
                        mats[0],
                        mats[1],
                        null, null, Double.NaN
                    );
                }
            }
            if (calibrationData == null) {
                LOGGER.info("Starting fresh calibration by images");
                final var imagePaths = resourceProvider
                    .findFilePathsFromResourcePath("chessboard/1920_1446");
                final var chessboardData = ChessboardData.init();
                    
                // for each image do find chessboard corners
                LOGGER.info("Searching for corners in " + imagePaths + "...");
                for (String imagePath: imagePaths) {
                    final var img = new File(imagePath);           
                    LOGGER.info("Corners iteration for file " + imagePath);                    
                    // find corners and store the result in the calibrator instance
                    final var hasCorners = CameraCalibrator.findCorners(
                        img.getAbsolutePath(), 
                        cameraParameter,
                        chessboardData,
                        (originalFrame, cornersFrame) -> {
                            // clear and repaint Jpanels
                            /*
                            cameraFeed.removeAll();
                            processedFeed.removeAll();
                            cameraFeed.repaint();
                            processedFeed.repaint();
                            try {
                                // Thread.sleep(500);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }

                            DetectionUtil.debugShowImage(
                                originalFrame, "original_" + imagePath
                            );
                             */

                            // draw the images to the panels
                            // DetectionUtil.drawImage(originalFrame, cameraFeed);
                            // DetectionUtil.drawImage(cornersFrame, processedFeed);
                        });                        
                    if (!hasCorners) {
                        LOGGER.warning("could not determine corners in image " + img.getAbsolutePath());
                        continue;
                    }
                }
            
                // calibrate with the infos
                calibrationData = CameraCalibrator.calibrate(
                    cameraParameter,
                    chessboardData
                );

                // debug 
                System.out.println("Press any key to continue.....");
                scanner.nextLine();
            }

            // distortion, ArUco region of interest and field detection
            final var dartsboardImagePaths = resourceProvider
                .findFilePathsFromResourcePath("dartsboard/1920_1446");
            LOGGER.info("distortion of " + dartsboardImagePaths + "...");
            for (String imagePath: dartsboardImagePaths) {
                final var undistortedImage = DetectionUtil.distortFunction(
                    imagePath, 
                    cameraParameter,
                    calibrationData,
                    false);

                // aruco detection of undistorted image and extraction of ROI
                LOGGER.info("trying to detect aruco markers in " + imagePath);
                final var roiImage = DetectionUtil.extractArucoROI(
                    undistortedImage,
                    Aruco.DICT_6X6_250,
                    600,
                    600,
                    true, // use true for more reliable extraction of a ROI 
                    true, 
                    false); 
                if (roiImage != null) {
                    // resize to 600x600 improved detection performance
                    Imgproc.resize(
                        roiImage,
                        roiImage,
                        new Size(600, 600)
                    );
                    
                    // contour detection in ROI image
                    /*
                    final var contourParamater = ContourParameter.defaultParameter();
                    */
                    /*
                    */
                    final var contourParamater = new ContourParameter(
                        0,
                        150,
                        250,
                        1,
                        1,
                        100,
                        0.01,
                        new Scalar(31, 240, 255),
                        1
                    );
                    final var contourDataList = DetectionUtil.findContours(
                        roiImage,
                        contourParamater,
                        false,
                        false
                    );
                    /* debug 
                    DetectionUtil.debugShowImage(
                        resizedROI, 
                        "afterDrawContour");
                    */

                    // extract outer most ellipse
                    for (var contourData : contourDataList) {
                        // final var dbgImage = roiImage.clone();
                        /*
                        LOGGER.info("ContourData:"  
                            + " length=" + contourData.approxSize() 
                            + "; area=" + contourData.area());
                        */
                        final var thresholdLow = 50_000;
                        final var thresholdHigh = 250_000;
                        final var withinThreshold = 
                            thresholdLow <= contourData.area() 
                            && thresholdHigh >= contourData.area();
                        if (withinThreshold) {        
                                final var contour2f = new MatOfPoint2f(); 
                                contourData.contour()
                                    .convertTo(contour2f, CvType.CV_32FC1);
                                final var rotatedRect = 
                                    Imgproc.fitEllipse(
                                        contour2f);
                                // LOGGER.info("ellipse: " + rotatedRect.toString());
                                /*
                                Imgproc.ellipse(
                                    dbgImage,
                                    rotatedRect,
                                    new Scalar(240, 1, 255),
                                    1
                                );
                                */

                                // TODO generate polar coordinate system using the found ellipse
                                final var polarCoordSysImage = roiImage.clone();

                                // draw bounding box of ellipse
                                LOGGER.info("ellipse bounding rect: " + rotatedRect.boundingRect());
                                Imgproc.rectangle(
                                    polarCoordSysImage,
                                    rotatedRect.boundingRect(),
                                    new Scalar(240, 1, 255),
                                    1
                                );
                                
                                // outer ellipse
                                Imgproc.ellipse(
                                    polarCoordSysImage,
                                    rotatedRect,
                                    new Scalar(40,240,255),
                                    1
                                );
                                // center
                                Imgproc.drawMarker(
                                    polarCoordSysImage, 
                                    rotatedRect.center,
                                    new Scalar(50,50,50),
                                    Imgproc.MARKER_CROSS, 
                                    600
                                );

                /*
                    * Dartboard ellipse radians 
                    *  - outer double              = 170 mm -> rect.x / 2
                    *  - inner double              = 170 mm - 8 mm -> (rect.x / 2) - ((rect.x / 2) * 4.70588235294f%)
                    *                                  diameter in percent [95.2941176470%]
                    *  - outer triple              = 107 mm -> (rect.x / 2) - ((rect.x / 2) * 62.9411764705f%)
                    *                                  diameter in percent [62.9411764705f%]
                    *  - inner triple              = 107 mm - 8 mm -> (rect.x / 2) - ((rect.x / 2) * 58.2352941176f%) - ((rect.x / 2) * 4.70588235294f%)
                    *                                  diameter in percent [58.2352941176%] 
                    *  - single bull               = center.x + (31,8 mm / 2) 
    *                                                   diameter in percent [7,05099778270%]
                    *  - bull's eye                = center.x + (12,7 mm / 2) 
                    *                                  diameter in percent [3.73529411764%]


                        assumptions:
                            - dartsboard generally has a size of 451mm, including the outer black part
                                -> we will use the width of the detected ellipse instead...

                            - 37.6940133037% of that is the radius of each quadrant
                                -> defines outer double
                            - 23,7250554323% of that is the radius of a quadrants triple multiplier
                                -> defines outer triple
                            - 1,77383592017% of that is the radius of each quadrants multiplier field size
                                -> defines multiplier
                            - 7,05099778270% of that is the diameter of outer bull
                                -> definies outer bull
                            - 2,815964523285 of that is the diameter of bullseye
                                -> definies outer bullseye

                            // TODO test assumptions
                    */
                                final float FACTOR_BULLSEYE = 2.815964523285f / 2; // factor of the bull field in a darts board; divided by two since we are in the first quadrant
                                final float FACTOR_BULL = 7.05099778270f / 2;

                                final float FACTOR_MULTIPLYER = 1.77383592017f;

                                final float FACTOR_QUADRANT_OUTER_TRIPLE = 23.7250554323f;
                                final float FACTOR_QUADRANT_INNER_TRIPLE = FACTOR_QUADRANT_OUTER_TRIPLE - FACTOR_MULTIPLYER;

                                final float FACTOR_QUADRANT_OUTER_DOUBLE = 37.6940133037f;
                                final float FACTOR_QUADRANT_INNER_DOUBLE = FACTOR_QUADRANT_OUTER_DOUBLE - FACTOR_MULTIPLYER;

                                // bullseye limit                                    
                                final var radiusBullsEyeLimit = 
                                    (int) (rotatedRect.size.width * (FACTOR_BULLSEYE / 100));
                                DetectionUtil.drawPolarCoordinateFactorXAxis(
                                    polarCoordSysImage,
                                    rotatedRect,
                                    radiusBullsEyeLimit,
                                    50,
                                    0,
                                    0,
                                    new Scalar(0,0,139)
                                );
                                
                                // inner bull limit 
                                final var radiusBullLimit = 
                                    (int) (rotatedRect.size.width * (FACTOR_BULL / 100));
                                DetectionUtil.drawPolarCoordinateFactorXAxis(
                                    polarCoordSysImage,
                                    rotatedRect,
                                    radiusBullLimit,
                                    50,
                                    0,
                                    0,
                                    new Scalar(250, 250, 250)
                                ); 

                                // TODO inner triple limit  
                                final var radiusInnerTripleLimit = (int) 
                                    // (rectRadius * 
                                    (rotatedRect.size.width *
                                        (FACTOR_QUADRANT_INNER_TRIPLE / 100));
                                    // (int) ((rotatedRect.size.width / 2) * (FACTOR_INNER_TRIPLE / 100));
                                DetectionUtil.drawPolarCoordinateFactorXAxis(
                                    polarCoordSysImage,
                                    rotatedRect,
                                    radiusInnerTripleLimit,
                                    50,
                                    0, 
                                    0,
                                    new Scalar(0, 0, 139)
                                );

                                // TODO outer triple limit
                                final var radiusOuterTripleLimit = (int) 
                                    //(rectRadius *
                                    (rotatedRect.size.width * 
                                        (FACTOR_QUADRANT_OUTER_TRIPLE / 100));
                                DetectionUtil.drawPolarCoordinateFactorXAxis(
                                    polarCoordSysImage,
                                    rotatedRect,
                                    radiusOuterTripleLimit,
                                    50,
                                    0, 
                                    0,
                                    new Scalar(255, 255, 255)
                                );

                                // TODO inner double limit
                                final var radiusInnerDoubleLimit = 
                                    (int) (rotatedRect.size.width * (FACTOR_QUADRANT_INNER_DOUBLE / 100));
                                DetectionUtil.drawPolarCoordinateFactorXAxis(
                                    polarCoordSysImage,
                                    rotatedRect,
                                    radiusInnerDoubleLimit,
                                    50,
                                    0,
                                    0,
                                    new Scalar(0, 0, 139)
                                ); 

                                // TODO outer double limit
                                DetectionUtil.drawPolarCoordinateFactorXAxis(
                                    polarCoordSysImage,
                                    rotatedRect,
                                    (int) (rotatedRect.size.width * (FACTOR_QUADRANT_OUTER_DOUBLE / 100)),
                                    50,
                                    0,
                                    0,
                                    new Scalar(250, 250, 250)
                                ); 

                                DetectionUtil.debugShowImage(
                                    polarCoordSysImage,
                                    "polar_" + imagePath.substring(
                                        imagePath.lastIndexOf("/") + 1,
                                        imagePath.length()));
                        } else {
                            LOGGER.info("Ignored ellipse: (" 
                                + imagePath.substring(
                                    imagePath.lastIndexOf("/") + 1,
                                    imagePath.length())
                                + "); area=" + contourData.area());
                        }
                    }
                }
                // debug 
                System.out.println("Press any key to continue.....");
                scanner.nextLine();
            }
            HighGui.destroyAllWindows();
            LOGGER.info("Completed");
        };
    }
}
package de.leidenheit;

import org.opencv.aruco.*;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.util.Map;
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
                // System.out.println("Press any key to continue.....");
                // scanner.nextLine();
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
                    960,
                    960,
                    true, // use true for more reliable extraction of a ROI 
                    false, 
                    false); 
                LOGGER.info(String.format("roi image size after extraction of ArUcos: %s", roiImage.size()));
                if (roiImage != null) {
                    // contour detection in ROI image
                    /*
                    final var contourParamater = ContourParameter.defaultParameter();
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

                    // extract outer most ellipse
                    for (var contourData : contourDataList) {
                        /*
                        LOGGER.info("ContourData:"  
                            + " length=" + contourData.approxSize() 
                            + "; area=" + contourData.area());
                        */
                        final var thresholdLow = 100_000;
                        final var thresholdHigh = 500_000;
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

                            // generate polar coordinate system using the found ellipse
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
                                960
                            );
                            final var limits = DetectionUtil.determineDartboardSectorLimits(
                                polarCoordSysImage, 
                                rotatedRect, 
                                true);
                            LOGGER.info("Limits: " + limits);
                            // debug
                            DetectionUtil.debugShowImage(
                                polarCoordSysImage,
                                "polar_" + imagePath.substring(
                                    imagePath.lastIndexOf("/") + 1,
                                    imagePath.length())
                            );
                            // draw polar coordinate system
                            // LOGGER.info("continue?");
                            // scanner.nextLine();

                            // polar coordiantes singleton
                            try {
                                final var polarCoordValueAngleRange = PolarCoordinateValueAngleRange.getInstance();
                                LOGGER.info("polar for 31° -> " + polarCoordValueAngleRange.findValueByAngle(31));
                                LOGGER.info("polar for 27° -> " + polarCoordValueAngleRange.findValueByAngle(27));
                                LOGGER.info("polar for 9° -> " + polarCoordValueAngleRange.findValueByAngle(9));
                                LOGGER.info("polar for 156° -> " + polarCoordValueAngleRange.findValueByAngle(156));
                                LOGGER.info("polar for 359.009° -> " + polarCoordValueAngleRange.findValueByAngle(359.009));
                                // draw polar coordinate system
                                final var pointLeftFieldBoundary = new Point();
                                final var pointRightFieldBoundary = new Point();
                                for (var entry : polarCoordValueAngleRange.getValueAngleRangeMap().entrySet()) {
                                    final double startAngle = entry.getKey().getMinValue();
                                    final double endAngle = entry.getKey().getMaxValue();    
                                    pointLeftFieldBoundary.x = (int) Math.round(
                                        rotatedRect.center.x + (rotatedRect.size.width / 1.75) * Math.cos(startAngle * Math.PI / -180.0));
                                    pointLeftFieldBoundary.y = (int) Math.round(
                                        rotatedRect.center.y + (rotatedRect.size.height / 1.75) * Math.sin(startAngle * Math.PI / -180.0));

                                    pointRightFieldBoundary.x = (int) Math.round(
                                        rotatedRect.center.x + (rotatedRect.size.width / 1.75) * Math.cos(endAngle * Math.PI / -180.0));
                                    pointRightFieldBoundary.y = (int) Math.round(
                                        rotatedRect.center.y + (rotatedRect.size.height / 1.75) * Math.sin(endAngle * Math.PI / -180.0));

                                    LOGGER.info(String.format(
                                        "drawLine for angles [%s][%s] to (%s,%s)", startAngle, endAngle, pointLeftFieldBoundary, pointRightFieldBoundary));
                                    Imgproc.line(
                                        polarCoordSysImage,
                                        rotatedRect.center,
                                        pointLeftFieldBoundary,
                                        new Scalar(200, 50, 200),
                                        1
                                    );
                                    Imgproc.line(
                                        polarCoordSysImage,
                                        rotatedRect.center,
                                        pointRightFieldBoundary,
                                        new Scalar(200, 50, 200),
                                        1
                                    );
                                    Imgproc.putText(
                                        polarCoordSysImage,
                                        String.valueOf(entry.getValue()),
                                        pointRightFieldBoundary,
                                        Imgproc.FONT_HERSHEY_DUPLEX,
                                        0.3,
                                        new Scalar(200, 50, 200)
                                    );
                                    // debug
                                    // DetectionUtil.debugShowImage(polar, "polar_sys");
                                    // LOGGER.info("press any...");
                                    // scanner.nextLine();
                                }
                                DetectionUtil.debugShowImage(polarCoordSysImage, "polar_sys");
                            } catch (UnexpectedException e) {
                                // ignored
                            }
                            LOGGER.info("continue?");
                            scanner.nextLine();
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
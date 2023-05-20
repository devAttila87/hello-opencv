package de.leidenheit;

import org.opencv.aruco.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVInterface;
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

        // load openCV
        // ubuntu arm64
        // System.load("C:\\Users\\Attila\\Documents\\development\\java\\hello-opencv\\lib\\arm64\\libopencv_java460.so");
        // windows x64
        System.load("C:\\Users\\Attila\\Documents\\development\\java\\hello-opencv\\lib\\x64\\opencv_java460.dll");
        LOGGER.info(String.format("OpenCV %s loaded successfully :-)", Core.VERSION));



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
                    // .findFilePathsFromResourcePath("chessboard/1920_1446");
                    .findFilePathsFromResourcePath("chessboard/temp");
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
                            DetectionUtil.debugShowImage(
                                originalFrame, "original_" + imagePath
                            );
                        });                        
                    if (!hasCorners) {
                        continue;
                    }
                }

                // calibrate with the infos
                calibrationData = CameraCalibrator.calibrate(
                    cameraParameter,
                    chessboardData
                );
            }

            // distortion, ArUco region of interest and field detection
            final var dartsboardImagePaths = resourceProvider
                // .findFilePathsFromResourcePath("dartsboard/1920_1446");
                .findFilePathsFromResourcePath("dartsboard/temp");
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
                    final var contourParameter = ContourParameter.defaultParameter();
                    */
                    final var contourParameter = new ContourParameter(
                        11,
                        50,
                        150,

                            6,
                        2,
                        50,
                        0.01,
                        new Scalar(31, 240, 255),
                        2
                    );
                    final var contourImage = roiImage.clone();
                    final var contourDataList = DetectionUtil.findContours(
                        contourImage,
                        contourParameter,
                        false,
                        false
                    );
                    LOGGER.info(String.format("Found contours in roiImage: %s", contourDataList.size()));
                    // extract outer most ellipse
                    for (var contourData : contourDataList) {
                        LOGGER.info("ContourData:"  
                            + " length=" + contourData.approxSize() 
                            + "; area=" + contourData.area());
                        final var thresholdLow = 500_000;
                        final var thresholdHigh = 2_500_000;
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
                            LOGGER.info(String.format("ellipse bounding rect: %s",  rotatedRect.boundingRect()));
                            // draw bounding box of ellipse
                            Imgproc.rectangle(
                                    contourImage,
                                    rotatedRect.boundingRect(),
                                    new Scalar(240, 1, 255),
                                    1
                            );
                            // outer ellipse
                            Imgproc.ellipse(
                                    contourImage,
                                    rotatedRect,
                                    new Scalar(40,240,255),
                                    1
                            );
                            // center
                            Imgproc.drawMarker(
                                    contourImage,
                                    rotatedRect.center,
                                    new Scalar(50,50,50),
                                    Imgproc.MARKER_CROSS,
                                    960
                            );
                            DetectionUtil.debugShowImage(contourImage, "before_warp_ellipse");
                            LOGGER.info("continue warping?");
                            scanner.nextLine();



                            // Erzeuge das Zielbild mit der quadratischen Größe
                            final var size = rotatedRect.size.width >= rotatedRect.size.height ?
                                rotatedRect.size.width : rotatedRect.size.height;
                            // final var size = roiImage.width() >= roiImage.height() ?
                            //    roiImage.width() : roiImage.height();
                            Mat destination = new Mat((int)size, (int)size, CvType.CV_8UC1);
                            // Definiere die Eckpunkte der Zielellipse
                            Point[] destinationPoints = new Point[4];
                            destinationPoints[0] = new Point(0, 0);
                            destinationPoints[1] = new Point(destination.cols(), 0);
                            destinationPoints[2] = new Point(destination.cols(), destination.rows());
                            destinationPoints[3] = new Point(0, destination.rows());
                            // Definiere die Eckpunkte der Quellellipse
                            Point[] sourcePoints = new Point[4];
                            sourcePoints[0] = new Point(rotatedRect.center.x - rotatedRect.size.width / 2, rotatedRect.center.y - rotatedRect.size.height / 2);
                            sourcePoints[1] = new Point(rotatedRect.center.x + rotatedRect.size.width / 2, rotatedRect.center.y - rotatedRect.size.height / 2);
                            sourcePoints[2] = new Point(rotatedRect.center.x + rotatedRect.size.width / 2, rotatedRect.center.y + rotatedRect.size.height / 2);
                            sourcePoints[3] = new Point(rotatedRect.center.x - rotatedRect.size.width / 2, rotatedRect.center.y + rotatedRect.size.height / 2);
                            // Führe die Perspektiventransformation durch
                            Mat transformationMatrix = Imgproc.getPerspectiveTransform(new MatOfPoint2f(sourcePoints), new MatOfPoint2f(destinationPoints));
                            Imgproc.warpPerspective(roiImage, destination, transformationMatrix, destination.size());
                            // debug 
                            DetectionUtil.debugShowImage(destination, "after_warp_ellipse");
                            System.out.println("continue with hough circle detection?");
                            scanner.nextLine();

                            /*
                            final var contourParameterWarp = new ContourParameter(
                                11,
                                100,
                                150,
                                1,
                                1,
                                100,
                                0.01,
                                new Scalar(31, 240, 255),
                                2
                            );
                            final var warpedContourDataList = DetectionUtil.findContours(
                                destination,
                                // contourParamater,
                                contourParameterWarp,
                                // ContourParameter.defaultParameter(),
                                true,
                                true
                            );
                            LOGGER.info(String.format("Found contours in warped image: %s", warpedContourDataList.size()));
                            for (var x : warpedContourDataList) {
                                LOGGER.info("Warping ContourData:"
                                        + " length=" + x.approxSize()
                                        + "; area=" + x.area());

                            final var warpedWithinThreshold =
                                    10_000 <= x.area()
                                            && 50_000 >= x.area();
                            if (warpedWithinThreshold) {
                                    LOGGER.info("warped ellipse valid threshold:" + x.area());
                                    final var warpedContour2f = new MatOfPoint2f(); 
                                    x.contour()
                                        .convertTo(warpedContour2f, CvType.CV_32FC1);
                                    final var warpedRotatedRect = 
                                        Imgproc.fitEllipse(
                                            warpedContour2f);
                                    LOGGER.info("warped ellipse bounding rect: " + warpedRotatedRect.boundingRect());
                                    // debug
                                    LOGGER.info("continue?");
                                    scanner.nextLine();

                                    // generate polar coordinate system using the found ellipse
                                    final var polarCoordSysImage = destination.clone();
                                    // draw bounding box of ellipse
                                    Imgproc.rectangle(
                                        polarCoordSysImage,
                                        warpedRotatedRect.boundingRect(),
                                        new Scalar(240, 1, 255),
                                        1
                                    );
                                    // outer ellipse
                                    Imgproc.ellipse(
                                        polarCoordSysImage,
                                        warpedRotatedRect,
                                        new Scalar(40,240,255),
                                        1
                                    );
                                    // center
                                    Imgproc.drawMarker(
                                        polarCoordSysImage, 
                                        warpedRotatedRect.center,
                                        new Scalar(50,50,50),
                                        Imgproc.MARKER_CROSS, 
                                        960
                                    );

                                    // TODO test me
                                    final var limits = DetectionUtil.determineDartboardSectorLimits(
                                        polarCoordSysImage, 
                                        warpedRotatedRect, 
                                        true);
                                    LOGGER.info("Limits: " + limits);
                                    //debug
                                    // LOGGER.info("continue?");
                                    // scanner.nextLine();

                                    // draw polar coordiantes from singleton
                                    DetectionUtil.drawPolarCoordinateSystem(
                                        polarCoordSysImage,    
                                        warpedRotatedRect,
                                        false
                                    );

                                    DetectionUtil.debugShowImage(polarCoordSysImage, "x_"
                                        + imagePath.substring(
                                            imagePath.lastIndexOf("/") + 1,
                                            imagePath.length())
                                    );
                                    LOGGER.info(String.format("warped image with polar coordinates %s (before=%s)", polarCoordSysImage.size(), rotatedRect.size));
                                    // debug
                                    LOGGER.info("continue?");
                                    scanner.nextLine();
                                } else {
                                    LOGGER.info("ellipse ignored due to threshold:" + x.area());
                                }
                            }
                             */
                            final var src = roiImage.clone();
                            final var gray = new Mat();
                            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

                            final var blurred = new Mat();
                            Imgproc.GaussianBlur(
                                    gray,
                                    blurred,
                                    new Size(11, 11),
                                    1
                            );
                            final var edges = new Mat();
                            Imgproc.Canny(
                                    blurred,
                                    edges,
                                    100,
                                    150);
                            final var kernel = Imgproc.getStructuringElement(
                                    Imgproc.MORPH_RECT,
                                    new Size(4, 4));
                            final var edges_dilate = new Mat();
                            Imgproc.dilate(
                                    edges,
                                    edges_dilate,
                                    kernel,
                                    new Point(),
                                    2
                            );
                            final var edges_erode = new Mat();
                            Imgproc.erode(
                                    edges_dilate,
                                    edges_erode,
                                    kernel,
                                    new Point(),
                                    1
                            );
                            DetectionUtil.debugShowImage(edges_erode, "prepared for hough circle detection");

                            Mat circles = new Mat();
                            Imgproc.HoughCircles(
                                    // edges_erode,
                                    gray,
                                    circles,
                                    Imgproc.HOUGH_GRADIENT,
                                    1.0,
                                    100,// (double)gray.rows()/16, // change this value to detect circles with different distances to each other
                                            150,
                                    100,
                                    0,
                                    0); // change the last two parameters

                            LOGGER.info(String.format("circles found: cols=%s; rows=%s", circles.cols(), circles.rows()));
                            // (min_radius & max_radius) to detect larger circles
                            for (int x = 0; x < circles.cols(); x++) {
                                double[] c = circles.get(0, x);
                                Point center = new Point(Math.round(c[0]), Math.round(c[1]));
                                // circle center
                                Imgproc.drawMarker(src, center, new Scalar(0, 100, 100), Imgproc.MARKER_CROSS, 960, 1);
                                // circle outline
                                int radius = (int) Math.round(c[2]);
                                Imgproc.circle(src, center, radius, new Scalar(255, 0, 255), 2, Imgproc.LINE_8, 0);

                                DetectionUtil.debugShowImage(src, "hough circle");
                                LOGGER.info(String.format("radius %s; press enter to continue....", radius));
                                scanner.nextLine();


                                /*
                                // generate polar coordinate system using the found ellipse
                                final var polarCoordSysImage = src.clone();
                                // draw bounding box of ellipse
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
                                        new Scalar(40, 240, 255),
                                        1
                                );
                                // center
                                Imgproc.drawMarker(
                                        polarCoordSysImage,
                                        rotatedRect.center,
                                        new Scalar(50, 50, 50),
                                        Imgproc.MARKER_CROSS,
                                        960
                                );
                                // debug
                                DetectionUtil.debugShowImage(polarCoordSysImage, "polar_sys");
                                 */
                            }










                            /*
                            // generate polar coordinate system using the found ellipse
                            final var polarCoordSysImage = src.clone();

                            // draw bounding box of ellipse
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

                            // TODO just a test here
                            final var rotatedRect = new RotatedRect();
                            rotatedRect.boundingRect().width = 366 * 2;
                            final var limits = DetectionUtil.determineDartboardSectorLimits2(
                                polarCoordSysImage, 
                                center,
                                true);
                            LOGGER.info("Limits: " + limits);
                            // debug
                            // LOGGER.info("continue?");
                            // scanner.nextLine();

                            // draw polar coordiantes from singleton
                            final var polarCoordValueAngleRange = PolarCoordinateValueAngleRange.getInstance();
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

                                // TODO just a test here
                                DetectionUtil.determineRadiusAndAngleFromPointRelativeToCenter(
                                    rotatedRect.center,
                                    pointRightFieldBoundary);
                            } 
                            // debug
                            DetectionUtil.debugShowImage(polarCoordSysImage, "polar_sys");

                             */
                        } else {
                            // ignored
                            /* 
                            LOGGER.info("Ignored ellipse: (" 
                                + imagePath.substring(
                                    imagePath.lastIndexOf("/") + 1,
                                    imagePath.length())
                                + "); area=" + contourData.area());
                            */
                        }
                    }
                }
                // debug 
                System.out.println("Press enter to continue...");
                scanner.nextLine();
            }
            HighGui.destroyAllWindows();
            LOGGER.info("Completed; press enter to quit...");
            scanner.nextLine();
        };
    }
}
package de.leidenheit;

import org.opencv.aruco.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.DictValue;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import javax.swing.border.Border;

import org.opencv.aruco.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.GenericSignatureFormatError;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.opencv.videoio.Videoio.CAP_DSHOW;
import static org.opencv.videoio.Videoio.CAP_PROP_FOCUS;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

public class Main {

    private static final Logger LOGGER = Logger.getLogger("HelloOpenCV");

    public static void main(String[] args) {
        // log everything
        LOGGER.setLevel(Level.INFO);

        // load openCV
        // OpenCV.loadLocally();
        LOGGER.info("OpenCV loaded successfully :-)");


                    
        // test of so library
        System.load("/home/leidenheit/hello-opencv/lib/libopencv_java460.so");


        // create gui
        final JPanel cameraFeed = new JPanel();
        cameraFeed.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final JPanel processedFeed = new JPanel();
        processedFeed.setBorder(BorderFactory.createLineBorder(Color.RED));
        /*
        final JFrame window = new JFrame("Hello OpenCV");
        window.setSize(new Dimension(1600, 540));
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.setLayout(new GridLayout(1, 1));
        window.add(cameraFeed);
        window.add(processedFeed);
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
        final VideoCapture camera = new VideoCapture(1, CAP_DSHOW);
        camera.set(CAP_PROP_FPS, 30);
        camera.set(CAP_PROP_FRAME_WIDTH, 1920);
        camera.set(CAP_PROP_FRAME_HEIGHT, 1080);

        // start computer vision
        Main.startComputerVision(cameraFeed, processedFeed, camera).run();
        return;
    }

    private static Runnable startComputerVision(final JPanel cameraFeed,
                                                final JPanel processedFeed,
                                                final VideoCapture camera) {
        return () -> {
            // debug
            Scanner scanner = new Scanner(System.in);

            /* not working */
            // instantiate a camera calibrator
            final var cameraCalibrator = new CameraCalibrator(1920, 1080);

            final var reuseCalibrationImages = true;
            if (!reuseCalibrationImages) {
                // TODO implement webcam
                throw new RuntimeException("Not yet implemented");
            } else {
                // read all files from /resource/chessboard into an array
                final var resProvider = new ResourceProvider();
                final var imagePaths = resProvider
                    .findFilePathsFromResourcePath("chessboard/1080p");

                // for each image do find chessboard corners
                LOGGER.info("Searching for corners in " + imagePaths + "...");
                for (String imagePath: imagePaths) {
                    final var img = new File(imagePath);                
                           
                    // find corners and store the result in the calibrator instance
                    final var hasCorners = cameraCalibrator.findCorners(
                        img.getAbsolutePath(), 
                        (originalFrame, cornersFrame) -> {
                            // clear and repaint Jpanels
                            cameraFeed.removeAll();
                            processedFeed.removeAll();
                            cameraFeed.repaint();
                            processedFeed.repaint();
                            try {
                                // Thread.sleep(500);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }

                            // draw the images to the panels
                            // DetectionUtil.drawImage(originalFrame, cameraFeed);
                            // DetectionUtil.drawImage(cornersFrame, processedFeed);
                        });                        
                    if (!hasCorners) {
                        LOGGER.warning("could not determine corners in image " + img.getAbsolutePath());
                        scanner.nextLine(); 
                        continue;
                    }

                    // debug 
                    // System.out.println("Press any key to continue.....");
                    // scanner.nextLine(); 
                }
            
                // calibrate with the infos
                final var calibrationResult = cameraCalibrator.calibrate();

                // aruco marker test
                /* working prototype YEAH :D
                final var arucoImagePaths = resProvider
                    .findFilePathsFromResourcePath("dartsboard/1080p");         
                for (String imagePath : arucoImagePaths) { 
                    final var imageWithArucoMarkers = Imgcodecs.imread(imagePath);
                    LOGGER.info("trying to detect aruco markers in " + imagePath);
                    final var img = Imgcodecs.imread(imagePath);
                    final var corners = new ArrayList<Mat>();
                    final var ids = new Mat();
                    final var rejectedImagePoints = new ArrayList<Mat>();



                    final var dict = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_250); 
                    final var detectorParams = DetectorParameters.create();
                    Aruco.detectMarkers(
                        img,
                        dict,
                        corners,
                        ids,
                        detectorParams
                        ,rejectedImagePoints
                    );
                    LOGGER.info("aruco detected marker: " 
                        + "\ncorners=" + corners.size()
                        + "\nrejections=" + rejectedImagePoints.size()
                    );
                    Aruco.drawDetectedMarkers(
                        img,
                        corners,
                        ids,
                        new Scalar(0,0, 161)
                    ); 
                    HighGui.imshow("before-aruco", imageWithArucoMarkers);
                    HighGui.waitKey(20);
                    HighGui.imshow("auruco", img);
                    HighGui.waitKey(20);
                    HighGui.destroyAllWindows();
                }
                */



                System.out.println("Press any key to continue.....");
                scanner.nextLine();


                // distortion test
                final var dartsboardImagePaths = resProvider
                    .findFilePathsFromResourcePath("dartsboard/1080p");

                LOGGER.info("distortion of " + dartsboardImagePaths + "...");
                for (String imagePath: dartsboardImagePaths) {
                    final var undistortedImage = cameraCalibrator.distortFunction(imagePath);

                    // debug 
                    // System.out.println("Press any key to continue.....");
                    // scanner.nextLine();


                    // aruco detection of undistorted
                    LOGGER.info("trying to detect aruco markers in " + imagePath);
                    final var markerCorners = new ArrayList<Mat>();
                    final var markerIds = new Mat();
                    final var rejectedImagePoints = new ArrayList<Mat>();

                    final var dict = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_250); 
                    final var detectorParams = DetectorParameters.create();
                    Aruco.detectMarkers(
                        undistortedImage,
                        dict,
                        markerCorners,
                        markerIds,
                        detectorParams
                        ,rejectedImagePoints
                    );
                    LOGGER.info("aruco detected marker: " 
                        + "\nmarkerCorners=" + markerCorners.size() + "; corners[0]=" + markerCorners.get(0).dump() 
                        // + "\ncorners[0]=" + markerCorners.get(0).get(0,0)[0]
                        // + "\ncorners[0]=" + markerCorners.get(1).get(0,0)[0]
                        // + "\ncorners[0]=" + markerCorners.get(1).get(0,0)[0]
                        // + "\ncorners[0]=" + markerCorners.get(2).get(0,0)[0]
                        + "\nmarkerIds=" + markerIds.dump() + "; rows=" + markerIds.rows() + "; cols=" + markerIds.cols() 
                        + "\nrejections=" + rejectedImagePoints.size()
                    );
                    Aruco.drawDetectedMarkers(
                        undistortedImage,
                        markerCorners,
                        markerIds,
                        new Scalar(0,0, 161)
                    ); 
                    // HighGui.imshow("aruco", undistortedImage);
                    // HighGui.waitKey(20);
                    /// HighGui.destroyWindow("aruco");

                    // test cropping the image based on the inner corners
                    // of aruco markers
                    // check if all corners are present in image
                    // debug 
                    // System.out.println("Press any key to continue.....");
                    // scanner.nextLine();
                    var markerIdsAsString = markerIds.dump();
                    // this regex removes [] newline tab space from a given string
                    
                    markerIdsAsString = markerIdsAsString.replaceAll("[\\[\\]\n\t ]", "");
                    final var expectedMarkerIds = java.util.List.of("0","1", "2", "3");
                    LOGGER.info("expected markerIds: " + expectedMarkerIds);
                    final var entries = Arrays.asList(markerIdsAsString.split(";"));
                    LOGGER.info("actual markerIds: " + entries);
                    final var validMarkerIds = markerIds.cols() == 1 && markerIds.rows() == 4 
                        && expectedMarkerIds.containsAll(entries);   
                    /**/
                    if (validMarkerIds) {
                        // TODO extract coordinates from aruco positions
                        // crop image using that coordinates
                        /*
                        int[] innerCorners = {2, 3, 0, 1};
                        int[] outerCorners = {0, 1, 2, 3};  
                        */

                        // analyzing markerIds matrix
                        /*
                        final var markerIdsRows = markerIds.rows();
                        final var markerIdsCols = markerIds.cols();
                        LOGGER.info("makerIdsColsRows="+markerIdsCols+","+markerIdsRows);
                        for (int i = 0; i < markerIdsCols; i++) {
                            for (int k = 0; k < markerIdsRows; k++) {
                                final var matrixValueArray = markerIds.get(k, i);
                                if (matrixValueArray.length > 1) {
                                    LOGGER.warning("matrix value array length > 1:\n"
                                        + "indices=" + i + "," + k
                                    );
                                }
                                LOGGER.info(
                                    "matrix col[" + i + "]:"
                                    + "\nrow[" + k + "]=" + markerIds.get(k, i)[0]
                                );
                            }
                        } 
                        */



                        // analyzing marker corners matrices
                        /*
                        for (Mat markerCornersEntry : markerCorners) {           
                            final var cornersRows = markerCornersEntry.rows();
                            final var cornerCols = markerCornersEntry.cols();
                            // LOGGER.info("markerCornersEntry->cornerColsRows="+cornerCols+","+cornersRows);
                            for (int i = 0; i < cornerCols; i++) {
                                for (int k = 0; k < cornersRows; k++) {
                                    final var matrixValueArray = markerCornersEntry.get(k, i);
                                    LOGGER.info("\n\n########## matrixValueArraySize=" + matrixValueArray.length);
                                    for(int j = 0; j<matrixValueArray.length; j++) {
                                        LOGGER.info("col[" + i + "]row[" + k + "]" 
                                            + "value["+ j +"]=" +markerCornersEntry.get(k, i)[j]
                                        );   
                                    }
                                
                                }
                            }
                        }
                         */

                        // point1
                        var index = entries.indexOf("0");
                        final var point1 = markerCorners.get(index)
                            .get(0, 0);
                        LOGGER.info("This is the first point=" + point1[0] + "," + point1[1]);

                        // point2
                        index = entries.indexOf("1");
                        final var point2 = markerCorners.get(index)
                            .get(0, 1);
                        LOGGER.info("This is the second point=" + point2[0] + "," + point2[1]);

                        // point3
                        index = entries.indexOf("2");
                        final var point3 = markerCorners.get(index)
                            .get(0, 2);
                        LOGGER.info("This is the third point=" + point3[0] + "," + point3[1]);

                        // point4
                        index = entries.indexOf("3");
                        final var point4 = markerCorners.get(index)
                            .get(0, 3);
                        LOGGER.info("This is the fourth point=" + point4[0] + "," + point4[1]);


                        // do homopgraphy
                        final var sourcePoints = new MatOfPoint2f();
                        sourcePoints.fromArray(
                            new Point(point1[0], point1[1]), 
                            new Point(point2[0], point2[1]),
                            new Point(point3[0], point3[1]),
                            new Point(point4[0], point4[1])
                        );
                        final var destPoints = new MatOfPoint2f();
                        destPoints.fromArray(
                            new Point(0, 0),
                            new Point(500, 0),
                            new Point(500, 500),
                            new Point(0, 500)
                        );
                        final var homoMat = Calib3d.findHomography(
                            sourcePoints,
                            destPoints
                        );
                        LOGGER.info("homography: " + homoMat.dump());

                        // warp perspective
                        final var warpPerspectiveImg = new Mat();
                        Imgproc.warpPerspective(
                            undistortedImage,
                            warpPerspectiveImg,
                            homoMat,
                            new Size(500, 500)
                        );

                        // point debug
                        Point[] dbgPoints = {
                            new Point(point1[0], point1[1]), 
                            new Point(point2[0], point2[1]),
                            new Point(point3[0], point3[1]),
                            new Point(point4[0], point4[1])
                        };
                        for (Point pt : dbgPoints) {
                            Imgproc.circle(warpPerspectiveImg,
                                pt, 
                                3, 
                                new Scalar(255, 0, 255), 
                                4);   
                        }

                        HighGui.imshow("warp", warpPerspectiveImg);
                        HighGui.waitKey(20);
                        HighGui.destroyWindow("warp");

                        // debug 
                        // System.out.println("Press any key to continue.....");
                        // scanner.nextLine();
                    } else {
                        LOGGER.warning("ArUco marker ids invalid: " 
                        + markerIds.dump() 
                        + "; rows=" + markerIds.rows() 
                        + "; cols=" + markerIds.cols());
                        // debug 
                        // System.out.println("Press any key to continue.....");
                        // scanner.nextLine();
                    }

                }
                HighGui.destroyAllWindows();
            }
             // */



            /*
            final var cameraCalibrator = new CameraCalibrator2();
            final var reuseCalibrationImages = true;
            if (!reuseCalibrationImages) {
                // TODO implement webcam
                throw new RuntimeException("Not yet implemented");
            } else {
                // read all files from /resource/chessboard into an array
                // final var resProvider = new ResourceProvider();
                // final var imagePaths = resProvider
                //    .findFilePathsFromResourcePath("chessboard/1080p");
                    // .findFilePathsFromResourcePath("chessboard");
                // for each image do find chessboard corners
                // for (String imagePath: imagePaths) {
                //     final var img = new File(imagePath);
                    final var imagePath = "src/resources/chessboard/1080p/*.jpg";
                    // final var imagePath = "src/resources/chessboard/same_size/*.jpg";
                    // final var imagePath = "src/resources/chessboard/*.jpg";
                    try {
                        cameraCalibrator.getPoints(
                            imagePath, 
                            "src/resources/points", 
                            new Size(9, 6));

                        final var calibrateArr = cameraCalibrator.loadCalibrate(
                            String.format("%scamera-matrix.bin", "src/resources/points"),
                            String.format("%sdist-coefs.bin", "src/resources/points"));
                        cameraCalibrator.undistortAll(
                            imagePath, 
                            "src/resources/_dbg_distorted_", 
                            calibrateArr[0],
                            calibrateArr[1]);
                    } catch (Exception e) {
                        //
                        LOGGER.warning(e.getMessage());
                    }                
                }
                */
            // }
            
            LOGGER.info("done");
            return;
        };
    }
}
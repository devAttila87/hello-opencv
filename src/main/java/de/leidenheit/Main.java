package de.leidenheit;

import org.opencv.aruco.*;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
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
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger("HelloOpenCV");

    public static void main(String[] args) {
        // log everything
        LOGGER.setLevel(Level.INFO);

        // load openCV
        System.load("/home/leidenheit/hello-opencv/lib/libopencv_java460.so");
        LOGGER.info("OpenCV loaded successfully :-)");

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

        System.exit(0);
    }

    private static Runnable startComputerVision(final JPanel cameraFeed,
                                                final JPanel processedFeed,
                                                final VideoCapture camera) {
        return () -> {
            // debug
            Scanner scanner = new Scanner(System.in);

            final var cameraParameter = CameraParameter.defaultParameter();
            final var reuseCalibrationImages = true;
            if (!reuseCalibrationImages) {
                // TODO implement webcam
                throw new RuntimeException("Not yet implemented");
            } else {
                final var resourceProvider = new ResourceProvider();
                final var imagePaths = resourceProvider
                    .findFilePathsFromResourcePath("chessboard/1080p");
                final var chessboardData = ChessboardData.init();
                    
                // for each image do find chessboard corners
                LOGGER.info("Searching for corners in " + imagePaths + "...");
                for (String imagePath: imagePaths) {
                    final var img = new File(imagePath);                
                    // find corners and store the result in the calibrator instance
                    final var hasCorners = CameraCalibrator.findCorners(
                        img.getAbsolutePath(), 
                        cameraParameter,
                        chessboardData,
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
                final var calibrationData = CameraCalibrator.calibrate(
                    cameraParameter,
                    chessboardData
                );

                // debug 
                // System.out.println("Press any key to continue.....");
                // scanner.nextLine();

                // distortion test
                final var dartsboardImagePaths = resourceProvider
                    .findFilePathsFromResourcePath("dartsboard/1080p");

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
                        1920,
                        1080,
                        false, 
                        false, 
                        false);
                    if (roiImage != null) {
                        // contour detection in ROI image
                        // final var contourParamater = ContourParameter.defaultParameter();
                        final var contourParamater = new ContourParameter(
                            11,
                            25,
                            75,
                            1,
                            1,
                            100,
                            0.01,
                            new Scalar(31, 240, 255),
                            3
                        );
                        final var contourDataList = DetectionUtil.findContours(
                            roiImage,
                            contourParamater,
                            false
                        );
                        // TODO fit ellipse
                        for (var contourData : contourDataList) {
                            if ( (200_000 / 4) < contourData.area() 
                                && (1_000_000 / 4) > contourData.area()) {
                                    LOGGER.info("ContourData:"  
                                    + " length=" + contourData.approxSize() 
                                    + "; area=" + contourData.area());
                                    
                                    final var contour2f = new MatOfPoint2f(); 
                                    contourData.contour()
                                        .convertTo(contour2f, CvType.CV_32FC1);
                                    final var rotatedRect = 
                                        Imgproc.fitEllipse(
                                            contour2f);
                                    
                                    Imgproc.ellipse(
                                        roiImage,
                                        rotatedRect,
                                        new Scalar(240, 1, 255),
                                        3
                                    );
                                    DetectionUtil.debugShowImage(
                                        roiImage, 
                                        "ellipse");
                                    
                                    // debug
                                    System.out.println("Press any key to continue.....");
                                    scanner.nextLine();
                                        
                            }
                        }
                    }
                    // debug 
                    System.out.println("Press any key to continue.....");
                    scanner.nextLine();
                }
                HighGui.destroyAllWindows();
            }
            LOGGER.info("Completed");
        };
    }
}
package de.leidenheit;

import nu.pattern.OpenCV;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import javax.swing.border.Border;

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
import java.util.Scanner;
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
        OpenCV.loadLocally();
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
        };
    }
}
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
        final JFrame window = new JFrame("Hello OpenCV");
        window.setSize(new Dimension(1200, 600));
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.setLayout(new GridLayout(1, 2));
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



        // create video capture object (index 0 is default camera)
        final VideoCapture camera = new VideoCapture(1, CAP_DSHOW);
        camera.set(CAP_PROP_FPS, 30);
        camera.set(CAP_PROP_FRAME_WIDTH, 640);
        camera.set(CAP_PROP_FRAME_HEIGHT, 480);

        // start computer vision
        Main.startComputerVision(cameraFeed, processedFeed, camera).run();
    }

    private static Runnable startComputerVision(final JPanel cameraFeed,
                                                final JPanel processedFeed,
                                                final VideoCapture camera) {
        return () -> {
            // debug
            Scanner scanner = new Scanner(System.in);

            // instantiate a camera calibrator
            final var cameraCalibrator = new CameraCalibrator(640, 480);

            final var reuseCalibrationImages = true;
            if (!reuseCalibrationImages) {
                // TODO implement webcam
                throw new RuntimeException("Not yet implemented");
            } else {
                // read all files from /resource/chessboard into an array
                final var resProvider = new ResourceProvider();
                final var imagePaths = resProvider
                    .findFilePathsFromResourcePath("chessboard/same_size");

                // for each image do find chessboard corners
                for (String imagePath: imagePaths) {
                    final var img = new File(imagePath);                
                           
                    // find corners and store the result in the calibrator instance
                    final var hasCorners = cameraCalibrator.findCorners(
                        img.getAbsolutePath(), 
                        (originalFrame, cornersFrame) -> {                                
                            LOGGER.info("Successfully found conrners: A=" + originalFrame.hashCode() + "; B=" + cornersFrame.hashCode());

                            // clear and repaint Jpanels
                            cameraFeed.removeAll();
                            processedFeed.removeAll();
                            cameraFeed.repaint();
                            processedFeed.repaint();
                            try {
                                Thread.sleep(250);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }

                            // draw the images to the panels
                            DetectionUtil.drawImage(originalFrame, cameraFeed);
                            DetectionUtil.drawImage(cornersFrame, processedFeed);
                        });
                    if (!hasCorners) {
                        LOGGER.warning("could not determine corners in image " + img.getAbsolutePath());
                        continue;
                    }

                    // debug 
                    // System.out.println("Press any key to continue.....");
                    // scanner.nextLine(); 
                }
                LOGGER.info("corners buffer size: " + cameraCalibrator.getCornersBufferSize());
                // debug 
                // LOGGER.info("corners buffer: \n" + cameraCalibrator.getCornersBuffer());
            
                // calibrate with the infos
                cameraCalibrator.calibrate();


                // dump values (currently for debugging only)
                resProvider.writeResource(
                    cameraCalibrator.getCornersBuffer(), 
                    "dump", 
                    "corners_buffer.dump");
                resProvider.writeResource(
                    cameraCalibrator.getCameraMatrix(), 
                    "dump", 
                    "camera_matrix.dump");
                resProvider.writeResource(
                    cameraCalibrator.getDistortionCoefficients(), 
                    "dump", 
                    "distortion_coefficients.dump");
                resProvider.writeResource(
                    cameraCalibrator.getAvgReprojectionError(), 
                    "dump", 
                    "avg_reprojection_errors.dump");

                

            }



            while (true) {
                break;
                
                /*
                // read frame from camera
                final var grabbed = camera.read(frame);
                DetectionUtil.drawImage(frame, cameraFeed);
                if (!grabbed) {
                    LOGGER.warning("no frame grabbed from camera");
                }





                // TODO test calibration
                if (!calibrated) {
                    try {
                        Thread.sleep(50);
                        final var frameToCalibrate = frame.clone();
                        calibrated = DetectionUtil.calibrateCamera(frameToCalibrate);
                        // draw the processed calibration image
                        DetectionUtil.drawImage(frameToCalibrate, processedFeed);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                */
            }
        };
    }
}
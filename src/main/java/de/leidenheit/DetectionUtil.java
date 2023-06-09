package de.leidenheit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.calib3d.Calib3d;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.text.WrappedPlainView;

public final class DetectionUtil {

    private static final Logger LOGGER = Logger.getLogger("DetectionUtil");

    /** 
     * Distorts an image using given calibration information.
     * 
     * @param imageFilePath
     * @param cameraParameter
     * @param calibrationData
     * @param debug 
     * @return Returns distorted image {@link Mat} which is resized 
     * to a given scale factor.
     */
    public static Mat distortFunction(
        final String imageFilePath, 
        final CameraParameter cameraParameter,
        final CalibrationData calibrationData,
        final boolean debug) {
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
            calibrationData.cameraMatrix(),
            calibrationData.distortionCoefficients(),
            dgbImageMat.size(), 
            1, 
            dgbImageMat.size());
        //LOGGER.info("\n#########\n\tOptimal Camera Matrix: " + optimalMatrix.dump()  
        //    + "\nROI=" + roi);

        Calib3d.undistort(
            dgbImageMat, 
            dgbUndistortedImageMat, 
            calibrationData.cameraMatrix(), 
            calibrationData.distortionCoefficients(),
            optimalMatrix);

        if (debug) {
            DetectionUtil.debugShowImage(
                dgbImageMat,
                "before_distortion"
            );
            DetectionUtil.debugShowImage(
                dgbUndistortedImageMat,
                "after_distortion"
            );
        }
        // resize
        Imgproc.resize(dgbUndistortedImageMat, dgbUndistortedImageMat, 
            new Size(
                dgbUndistortedImageMat.width()*cameraParameter.scaleFactor(), 
                dgbUndistortedImageMat.height()*cameraParameter.scaleFactor()));
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
     * Detects aruco markers of a given {@link Dictionary} in an undistorted {@link Mat} image
     * and crops the image roi to the given {@link Size}.
     * 
     * @param undistortedImage {@link Mat}
     * @param arucoDictionary  {@link Dictionary} // supports only Aruco.DICT_6X6_250
     * @param roiWidth // 1920
     * @param roiHeight // 1080
     * @param useOuterBoundary // true
     * @param drawMarkers {@link Size} // false
     * @param debug // false
     * 
     * @return Returns ROI {@link Mat} extract from aruco markers
     */
    public static Mat extractArucoROI(
        Mat undistortedImage,
        int arucoDictionary,
        int roiWidth,
        int roiHeight,
        boolean useOuterBoundary,
        boolean drawMarkers,
        boolean debug) {

        final var markerCorners = new ArrayList<Mat>();
        final var markerIds = new Mat();
        final var rejectedImagePoints = new ArrayList<Mat>();

        final var dict = Aruco.getPredefinedDictionary(arucoDictionary); 
        final var detectorParams = DetectorParameters.create();
        Aruco.detectMarkers(
            undistortedImage,
            dict,
            markerCorners,
            markerIds,
            detectorParams,
            rejectedImagePoints
        );
        LOGGER.info("aruco detected marker: " 
            + "\nmarkerCorners=" + markerCorners.size() + "; \t\ncorners[0]=" + markerCorners.get(0).dump() + "; \t\ncorners[1]=" + markerCorners.get(1).dump() + "; \t\ncorners[2]=" + markerCorners.get(2).dump() + "; \t\ncorners[3]=" + markerCorners.get(3).dump() 
            + "\nmarkerIds=" + markerIds.dump() + "; rows=" + markerIds.rows() + "; cols=" + markerIds.cols() 
            + "\nrejections=" + rejectedImagePoints.size()
        );
        if (drawMarkers) {
            Aruco.drawDetectedMarkers(
                undistortedImage,
                markerCorners,
                markerIds,
                new Scalar(0,0, 161)
            ); 
        }
        if (debug) {
            DetectionUtil.debugShowImage(
                    undistortedImage,
                    "undistorted_aruco_markers");
        }
        // check if all corners are present in image
        var markerIdsAsString = markerIds.dump();
        // this regex removes [] newline tab space from a given string
        markerIdsAsString = markerIdsAsString.replaceAll("[\\[\\]\n\t ]", "");
        final var expectedMarkerIds = java.util.List.of("0","1", "2", "3");
        LOGGER.info("expected markerIds: " + expectedMarkerIds);
        final var entries = Arrays.asList(markerIdsAsString.split(";"));
        LOGGER.info("actual markerIds: " + entries);
        final var validMarkerIds = markerIds.cols() == 1 && markerIds.rows() == 4 
            && expectedMarkerIds.containsAll(entries);   
        if (validMarkerIds) {
            // point1
            var index = useOuterBoundary 
                ? entries.indexOf("0")
                : entries.indexOf("2");
            final var point1 = markerCorners.get(index)
                .get(0, 0);
            // point2
            index = useOuterBoundary 
                ? entries.indexOf("1")
                : entries.indexOf("3");
            final var point2 = markerCorners.get(index)
                .get(0, 1);
            // point3
            index = useOuterBoundary 
                ? entries.indexOf("2")
                : entries.indexOf("0");
            final var point3 = markerCorners.get(index)
                .get(0, 2);
            // point4
            index = useOuterBoundary 
                ? entries.indexOf("3")
                : entries.indexOf("1");
            final var point4 = markerCorners.get(index)
                .get(0, 3);
            // homopgraphy
            final var sourcePoints = new MatOfPoint2f();
            sourcePoints.fromArray(
                new Point(point1[0], point1[1]), 
                new Point(point2[0], point2[1]),
                new Point(point3[0], point3[1]),
                new Point(point4[0], point4[1])
            );
            LOGGER.info("Source points for homography: " + sourcePoints.dump());
            final var destPoints = new MatOfPoint2f();
            destPoints.fromArray(
                new Point(0, 0),
                new Point(roiWidth-1, 0),
                new Point(roiWidth-1, roiHeight-1),
                new Point(0, roiHeight-1)
            );
            final var homoMat = Calib3d.findHomography(
                sourcePoints,
                destPoints
            );
            // warp perspective
            final var warpPerspectiveImg = new Mat(roiWidth, roiHeight, undistortedImage.type());
            Imgproc.warpPerspective(
                undistortedImage,
                warpPerspectiveImg,
                homoMat,
                warpPerspectiveImg.size()
            );
            if (debug) {
                DetectionUtil.debugShowImage(
                    warpPerspectiveImg, 
                    "warp");
            }
            if (!useOuterBoundary) {
                final var rotatedImg = new Mat(); 
                Core.rotate(
                    warpPerspectiveImg,
                    rotatedImg,  
                    Core.ROTATE_180);
                return rotatedImg;
            }
            return warpPerspectiveImg;
        } else {
            LOGGER.warning("ArUco marker ids invalid: " 
                + markerIds.dump() 
                + "; rows=" + markerIds.rows() 
                + "; cols=" + markerIds.cols());
        }
        return null;
    }

    /**
     * Finds contours in a given ROI {@link Mat}.
     * 
     * @param roi   {@link Mat}
     * @param contourParameter {@link ContourParameter}
     * @param debug
     * @return Returns a list of {@link MatOfPoint} containing found contours.
     */
    public static List<ContourData> findContours(
        Mat roi, 
        ContourParameter contourParameter,
        boolean drawContours, 
        boolean debug) {

        final var contourDataList = new ArrayList<ContourData>();

        // make roi gray to improve detection
        final var gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);        

        // apply gaussian filter to improve detection
        final var blurred = new Mat();
        Imgproc.GaussianBlur(
            gray,
            blurred,
            new Size(contourParameter.gaussFactor(), contourParameter.gaussFactor()),
            1
        );

        if (debug) {
            DetectionUtil.debugShowImage(
                blurred, 
                "contour_gauss");
        }
        
        // apply canny filter to improve detection
        final var edges = new Mat();
        Imgproc.Canny(
            blurred,
            edges,
            contourParameter.cannyThresholdLow(),
            contourParameter.cannyThresholdHigh());
        final var kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT, 
            new Size(4, 4));
        final var edges_dilate = new Mat();
        Imgproc.dilate(
            edges,
            edges_dilate,
            kernel,
            new Point(),
            contourParameter.dilateIterations()
        );
        final var edges_erode = new Mat();
        Imgproc.erode(
            edges_dilate,
            edges_erode,
            kernel,
            new Point(),
            contourParameter.erodeIterations()
        );
        if (debug) {
            DetectionUtil.debugShowImage(
                edges_erode, 
                "canny_erode");
        }

        // finally findContours
        final var image = edges_erode.clone();
        final var contours = new ArrayList<MatOfPoint>();
        final var hierarchy = new Mat();
        Imgproc.findContours(    
            image,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE 
        );

        final var areaThreshold = contourParameter.areaThreshold();
        for (MatOfPoint contour : contours) {
            final var area = Imgproc.contourArea(
                contour);
            if (area > areaThreshold) {
                final var contour2f = new MatOfPoint2f(); 
                contour.convertTo(contour2f, CvType.CV_32FC1);
                final var perimeter = Imgproc.arcLength(
                    contour2f, 
                    false);

                final var epsilon = contourParameter.epsilon();
                final var approximatedCurve = new MatOfPoint2f(); 
                Imgproc.approxPolyDP(
                    contour2f, 
                    approximatedCurve, 
                    epsilon * perimeter, 
                    false);

                final var boundingBoxRect = Imgproc.boundingRect(approximatedCurve);

                final var data = new ContourData(
                    approximatedCurve.elemSize(),
                    area,
                    approximatedCurve,
                    boundingBoxRect,
                    contour
                );
                contourDataList.add(data);
            }
        }

        if (drawContours) {
            Imgproc.drawContours(
                roi,
                contours,
                -1,
                contourParameter.drawColorBGRA(),
                contourParameter.drawThickness()
            );
        }
        if (debug) {
            DetectionUtil.debugShowImage(
                roi,
                "contours_after_area_peri_approx_bb");
        }
        return contourDataList;
    }

    // TODO modify to support also angles
    public static void drawPolarCoordinateFactorXAxis(
        Mat ellipseImage, 
        RotatedRect ellipseRotatedRectangle, 
        int xOffsetFromOrigin,
        int drawSize,
        int xAdjustment,
        int yAdjustment,
        Scalar colorScalar) {
        
        final var radius = (int) ellipseRotatedRectangle.center.x + xOffsetFromOrigin + xAdjustment;
        Imgproc.drawMarker(
            ellipseImage,
            new Point(radius, (int) ellipseRotatedRectangle.center.y + yAdjustment),
            colorScalar,
            Imgproc.MARKER_CROSS,
            drawSize
        );
    }

    // TODO modify to support also angles
    public static void drawPolarCoordinateFactorXAxis2(
            Mat ellipseImage,
            Point center,
            int xOffsetFromOrigin,
            int drawSize,
            int xAdjustment,
            int yAdjustment,
            Scalar colorScalar) {

        final var radius = (int) center.x + xOffsetFromOrigin + xAdjustment;
        Imgproc.drawMarker(
                ellipseImage,
                new Point(radius, (int) center.y + yAdjustment),
                colorScalar,
                Imgproc.MARKER_CROSS,
                drawSize
        );
    }

    /**
     * Uses {@link HighGui.imshow} to present an image resized into 640x480 pixels.
     * for debugging purposes.
     * 
     * @param matImage
     * @param windowName
     */
    public static void debugShowImage(final Mat matImage, final String windowName) {
        
        // final Size dSize = new Size(960, 960);
        // final Mat matResized = new Mat();
        // Imgproc.resize(matImage, matResized, dSize);
        // HighGui.imshow(windowName, matResized);
        HighGui.imshow(windowName, matImage);
        HighGui.waitKey(25);
        HighGui.destroyWindow(windowName);
    }

    /**
     * Returns the limits of the basic dartboard sectors based on calculation factors
     * applied to a given dartboard ellipse.
     *
     * @param ellipseImage {@link Mat}
     * @param ellipseBoundary {@link RotatedRect}
     * @param debug
     * @return {@link DartboardSectorLimits}
     */
    public static DartboardSectorLimits determineDartboardSectorLimits2(
            Mat ellipseImage,
            Point center,
            int radius,
            boolean debug) {

        final var width = radius * 2;
        final var radiusBullsEyeLimit = (int) (width * (DartboardRadianFactor.BULLSEYE / 100));
        final var radiusBullLimit = (int) (width * (DartboardRadianFactor.BULL / 100));
        final var radiusInnerTripleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_INNER_TRIPLE / 100));
        final var radiusOuterTripleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_OUTER_TRIPLE / 100));
        final var radiusInnerDoubleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_INNER_DOUBLE / 100));
        final var radiusOuterDoubleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_OUTER_DOUBLE / 100));

        if (debug) {
            DetectionUtil.drawPolarCoordinateFactorXAxis2(
                    ellipseImage,
                    center,
                    radiusBullsEyeLimit,
                    100,
                    0,
                    0,
                    new Scalar(0, 0, 139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis2(
                    ellipseImage,
                    center,
                    radiusBullLimit,
                    100,
                    0,
                    0,
                    new Scalar(0, 0, 139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis2(
                    ellipseImage,
                    center,
                    radiusInnerTripleLimit,
                    100,
                    0,
                    0,
                    new Scalar(0, 0, 139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis2(
                    ellipseImage,
                    center,
                    radiusOuterTripleLimit,
                    100,
                    0,
                    0,
                    new Scalar(0, 0, 139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis2(
                    ellipseImage,
                    center,
                    radiusInnerDoubleLimit,
                    100,
                    0,
                    0,
                    new Scalar(0, 0, 139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis2(
                    ellipseImage,
                    center,
                    radiusOuterDoubleLimit,
                    100,
                    0,
                    0,
                    new Scalar(0, 0, 139)
            );
        }
        return new DartboardSectorLimits(
                radiusBullsEyeLimit,
                radiusBullLimit,
                radiusInnerTripleLimit,
                radiusOuterTripleLimit,
                radiusInnerDoubleLimit,
                radiusOuterDoubleLimit);
    }

    /**
     * Returns the limits of the basic dartboard sectors based on calculation factors 
     * applied to a given dartboard ellipse.
     * 
     * @param ellipseImage {@link Mat}
     * @param ellipseBoundary {@link RotatedRect}
     * @param debug
     * @return {@link DartboardSectorLimits}
     */
    public static DartboardSectorLimits determineDartboardSectorLimits(
        Mat ellipseImage, 
        RotatedRect ellipseBoundary, 
        boolean debug) {

        final var width = ellipseBoundary.boundingRect().width;
        final var radiusBullsEyeLimit = (int) (width * (DartboardRadianFactor.BULLSEYE / 100));
        final var radiusBullLimit = (int) (width * (DartboardRadianFactor.BULL / 100));
        final var radiusInnerTripleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_INNER_TRIPLE / 100));
        final var radiusOuterTripleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_OUTER_TRIPLE / 100));
        final var radiusInnerDoubleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_INNER_DOUBLE / 100));
        final var radiusOuterDoubleLimit = (int) (width * (DartboardRadianFactor.QUADRANT_OUTER_DOUBLE / 100));

        if (debug) {
            DetectionUtil.drawPolarCoordinateFactorXAxis(
                ellipseImage,
                ellipseBoundary,
                radiusBullsEyeLimit,
                100,
                0,
                0,
                new Scalar(0,0,139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis(
                ellipseImage,
                ellipseBoundary,
                radiusBullLimit,
                100,
                0,
                0,
                new Scalar(0,0,139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis(
                ellipseImage,
                ellipseBoundary,
                radiusInnerTripleLimit,
                100,
                0,
                0,
                new Scalar(0,0,139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis(
                ellipseImage,
                ellipseBoundary,
                radiusOuterTripleLimit,
                100,
                0,
                0,
                new Scalar(0,0,139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis(
                ellipseImage,
                ellipseBoundary,
                radiusInnerDoubleLimit,
                100,
                0,
                0,
                new Scalar(0,0,139)
            );
            DetectionUtil.drawPolarCoordinateFactorXAxis(
                ellipseImage,
                ellipseBoundary,
                radiusOuterDoubleLimit,
                100,
                0,
                0,
                new Scalar(0,0,139)
            );
        }
        return new DartboardSectorLimits(
            radiusBullsEyeLimit, 
            radiusBullLimit, 
            radiusInnerTripleLimit, 
            radiusOuterTripleLimit, 
            radiusInnerDoubleLimit, 
            radiusOuterDoubleLimit);
    }

    /**
     * Determines the radius and angle from a given point relative to a given center point.
     * 
     * @param center {@link Point}
     * @param point {@link Point}
     * @return {@link double[radius, angle]}
     */
    public static double[] determineRadiusAndAngleFromPointRelativeToCenter(
        final Point center, 
        final Point point) {
        
        double radius = -1.0f; // error state
        double angle = 0.0f;

        if (center.x >= 0 && center.y >= 0 
            && point.x >= 0 && point.y >= 0) {

            radius = Math.sqrt(
                Math.pow(point.x - center.x, 2)
                + Math.pow(point.y - center.y, 2)
            );
            LOGGER.info(String.format("Determined radius=%s", radius));

            if (point.y < center.y) {
                // quadrant I
                if (point.x < center.x) {
                    LOGGER.info("Determining angle in quadrant I");
                    // angle acos
                    angle = Math.acos(Math.abs(point.y - center.y) / radius) + Math.PI / 2;
                // quadrant II 
                } else {
                    LOGGER.info("Determining angle in quadrant II");
                    // angle asin
                    angle = Math.asin(Math.abs(point.y - center.y) / radius);
                }
            } else {
                // quadrant III
                if (point.x > center.x) {
                    LOGGER.info("Determining angle in quadrant III");
                    // angle acos
                    angle = Math.acos(Math.abs(point.y - center.y) / radius) + Math.PI + Math.PI / 2;
                } else {
                    // quadrant IV
                    LOGGER.info("Determining angle in quadrant IV");
                    // angle asin
                    angle = Math.asin(Math.abs(point.y - center.y) / radius) + Math.PI;
                }
            }
            // convert radiant to degrees
            angle = angle * (180 / Math.PI);
            LOGGER.info(String.format("Determined angle=%s", angle));
        } else {
            LOGGER.info(String.format("Cannot determin angle an radius due to invalid input points: (%s)(%s)",
                center, point));
        }

        return new double[]{radius, angle};
    }

    /**
     * Draws a dartboard representation as polar coordinate system on a given image. 
     * 
     * @param image {@link Mat}
     * @param rotatedRectEllipse {@link RotatedRect}
     * @param debug
     */
    public static void drawPolarCoordinateSystem(
            Mat image, 
            RotatedRect rotatedRectEllipse, 
            boolean debug) {
        final var polarCoordValueAngleRange = PolarCoordinateValueAngleRange.getInstance();
        final var pointLeftFieldBoundary = new Point();
        final var pointRightFieldBoundary = new Point();
        for (var entry : polarCoordValueAngleRange.getValueAngleRangeMap().entrySet()) {
            final double startAngle = entry.getKey().getMinValue();
            final double endAngle = entry.getKey().getMaxValue();    
            pointLeftFieldBoundary.x = (int) Math.round(
                rotatedRectEllipse.center.x + 
                    (rotatedRectEllipse.size.width / 1.75) * Math.cos(startAngle * Math.PI / -180.0));
            pointLeftFieldBoundary.y = (int) Math.round(
                rotatedRectEllipse.center.y + 
                    (rotatedRectEllipse.size.height / 1.75) * Math.sin(startAngle * Math.PI / -180.0));
            pointRightFieldBoundary.x = (int) Math.round(
                rotatedRectEllipse.center.x + 
                    (rotatedRectEllipse.size.width / 1.75) * Math.cos(endAngle * Math.PI / -180.0));
            pointRightFieldBoundary.y = (int) Math.round(
                rotatedRectEllipse.center.y + 
                    (rotatedRectEllipse.size.height / 1.75) * Math.sin(endAngle * Math.PI / -180.0));

            LOGGER.info(String.format("drawLine for angles [%s][%s] to (%s,%s)", 
                startAngle, endAngle, pointLeftFieldBoundary, pointRightFieldBoundary));
            Imgproc.line(
                image,
                rotatedRectEllipse.center,
                pointLeftFieldBoundary,
                new Scalar(200, 50, 200),
                1
            );
            Imgproc.line(
                image,
                rotatedRectEllipse.center,
                pointRightFieldBoundary,
                new Scalar(200, 50, 200),
                1
            );
            Imgproc.putText(
                image,
                String.valueOf(entry.getValue()),
                pointRightFieldBoundary,
                Imgproc.FONT_HERSHEY_DUPLEX,
                0.3,
                new Scalar(200, 50, 200)
            );
            if (debug) {
                DetectionUtil.debugShowImage(image, "debug_polarcoordsys_" + entry.getValue());
            }
        }
    }

    /**
     * Estimates the most outer ellipse shape in an given {@link Mat} image.
     * 
     * @param imageWithContours {@link Mat}
     * @param contourParameter {@link ContourParameter}
     * @param warpToSquareLikeShape 
     * @param debug
     * 
     * @return {@link RotatedRect}
    public static RotatedRect findMostOuterEllipse(
        Mat imageWithContours,
        ContourParameter contourParameter,
        boolean warpToSquareLikeShape,
        boolean debug 
    ) {
        final var contourDataList = DetectionUtil.findContours(
            imageWithContours,
            contourParameter,
            debug,
            debug
        );
        LOGGER.info(String.format("Found contours: %s", contourDataList.size()));
        // extract outer most ellipse
        for (var contourData : contourDataList) {
            if (debug) {
                LOGGER.info("ContourData:"  
                    + " length=" + contourData.approxSize() 
                    + "; area=" + contourData.area());
            }
            final var thresholdLow = 100_000; // TODO as parameter
            final var thresholdHigh = 500_000; // TODO as parameter
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
                LOGGER.info("Most outer ellipse bounding rect: " + rotatedRect.boundingRect());

                final var isNotSquareLikeShape = rotatedRect.size.width != rotatedRect.size.height; 
                // warping most outer ellipse to a square like shape 
                if (warpToSquareLikeShape && isNotSquareLikeShape) {
                    // create squared target image
                    final var size = rotatedRect.size.width >= rotatedRect.size.height ?
                        rotatedRect.size.width : rotatedRect.size.height;
                    Mat destination = new Mat((int)size, (int)size, CvType.CV_8UC1);

                    // define points of the squared target ellipse
                    Point[] destinationPoints = new Point[4];
                    destinationPoints[0] = new Point(0, 0);
                    destinationPoints[1] = new Point(destination.cols(), 0);
                    destinationPoints[2] = new Point(destination.cols(), destination.rows());
                    destinationPoints[3] = new Point(0, destination.rows());
                    
                    // grab points of the found most outer ellipse
                    Point[] sourcePoints = new Point[4];
                    sourcePoints[0] = new Point(rotatedRect.center.x - rotatedRect.size.width / 2, 
                        rotatedRect.center.y - rotatedRect.size.height / 2);
                    sourcePoints[1] = new Point(rotatedRect.center.x + rotatedRect.size.width / 2, 
                        rotatedRect.center.y - rotatedRect.size.height / 2);
                    sourcePoints[2] = new Point(rotatedRect.center.x + rotatedRect.size.width / 2, 
                        rotatedRect.center.y + rotatedRect.size.height / 2);
                    sourcePoints[3] = new Point(rotatedRect.center.x - rotatedRect.size.width / 2, 
                        rotatedRect.center.y + rotatedRect.size.height / 2);

                    // apply perspective transformation
                    Mat transformationMatrix = Imgproc.getPerspectiveTransform(
                        new MatOfPoint2f(sourcePoints), 
                        new MatOfPoint2f(destinationPoints)
                    );
                    Imgproc.warpPerspective(
                        imageWithContours, 
                        destination, 
                        transformationMatrix, 
                        destination.size());

                    final var contourParamaterWarp = new ContourParameter(
                        13,
                        50,
                        150,
                        0,
                        0,
                        50,
                        0.01,
                        new Scalar(31, 240, 255),
                        1
                    );
                    final var warpedContourDataList = DetectionUtil.findContours(
                        destination,
                        ContourParameter.defaultParameter(),
                        debug,
                        debug
                    );

                    LOGGER.info(String.format("Found contours in warped image: %s", 
                        warpedContourDataList.size()));        
                    for (var contour : warpedContourDataList) {
                        final var warpedWithinThreshold = 
                            185_000 <= contour.area() 
                            && 1_000_000 >= contour.area();
                        if (warpedWithinThreshold) {        
                            LOGGER.info("Warped ellipse valid threshold:" + contour.area());
                            final var warpedContour2f = new MatOfPoint2f(); 
                            contour.contour()
                                .convertTo(warpedContour2f, CvType.CV_32FC1);
                            final var warpedRotatedRect = 
                                Imgproc.fitEllipse(
                                    warpedContour2f);
                            LOGGER.info("warped ellipse bounding rect: " + 
                                warpedRotatedRect.boundingRect());
                        } else {
                            LOGGER.info("ellipse ignored due to threshold:" + contour.area());
                        }
                    }    
                }
                return rotatedRect;
            } else {
                // ignored
            }
        }
        return null;
    }
     */

    private DetectionUtil() {
        // hide constructor
    }
}

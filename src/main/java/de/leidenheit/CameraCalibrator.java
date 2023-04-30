package de.leidenheit;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class CameraCalibrator {

    private static final Logger LOGGER = Logger.getLogger(CameraCalibrator.class.toString());
    private static final int CALIBRATION_FLAGS = 0
        + Calib3d.CALIB_FIX_PRINCIPAL_POINT // marginal incluence on avgReprojection
        + Calib3d.CALIB_ZERO_TANGENT_DIST // marginal incluence on avgReprojection
        // + Calib3d.CALIB_FIX_ASPECT_RATIO // significant incluence on avgReprojection: > 1.x
        + Calib3d.CALIB_FIX_K4 // marginal incluence on avgReprojection
        + Calib3d.CALIB_FIX_K5; // marginal incluence on avgReprojection;

    /** 
     * Calibrates a camera based on objectpoints and imagepoints determined 
     * by chessboard detection.
     * 
     * @param cameraParameter {@link CameraParameter}
     * @param chessboardData {@link ChessboardData}
     * @return {@link CalibrationData}
     */
    public static CalibrationData calibrate(
        final CameraParameter cameraParameter,
        final ChessboardData chessboardData) {

        ArrayList<Mat> rvecs = new ArrayList<Mat>();
        ArrayList<Mat> tvecs = new ArrayList<Mat>();
        Mat reprojectionErrors = new Mat();

        LOGGER.info("\nobjectPoints= " + chessboardData.objectPoints().size() 
            + "\nimagePoints=" + chessboardData.imagePoints().size());
         
        final var distortionCoefficients = 
            Mat.zeros(5, 1, CvType.CV_64FC1);
        final var cameraMatrix = 
            Mat.eye(3, 3, CvType.CV_64FC1);
        Calib3d.calibrateCamera(
            chessboardData.objectPoints(),
            chessboardData.imagePoints(),
            new Size(
                cameraParameter.cameraResolutionWidth(), 
                cameraParameter.cameraResolutionHeight()),
            cameraMatrix, 
            distortionCoefficients, 
            rvecs, 
            tvecs,
            CALIBRATION_FLAGS);
        final double avgReprojectionErrors = computeReprojectionErrors(
            chessboardData,
            cameraMatrix,
            new MatOfDouble(distortionCoefficients),
            rvecs, 
            tvecs, 
            reprojectionErrors);

        final var calibrationData = new CalibrationData(
            cameraMatrix,
            distortionCoefficients,
            rvecs,
            tvecs,
            avgReprojectionErrors
        );

        LOGGER.info("CalibrationSuccessful=" + calibrationData.isCalibrationValid()
        //    + "\n\nobjectPoints=" + objectPoints
        //    + "\n\nrvecs=" + rvecs
        //    + "\n\ntvecs=" + tvecs
        //    + "\ndistortionCoefficients=" + this.mDistortionCoefficients
            + "\n\navgReprojectionErrors=" + calibrationData.avgReprojectionErrors());

        return calibrationData;
    }

    /** 
     * Searches for corners in a given chessboard image.
     * 
     * @param imageFilePath Absolute filepath {@link String} to the image.
     * @param cameraParameter {@link CameraParameter}
     * @param chessboardData {@link ChessboardData}
     * @param biConsumerOriginalAndProcessedFrame {@link BiConsumer}
     * @return Returns true if corners were found in the given image, 
     *  otherwise false.
     */
    public static boolean findCorners(
        final String imageFilePath, 
        final CameraParameter cameraParameter,
        final ChessboardData chessboardData, 
        final BiConsumer<Mat, Mat> biConsumerOriginalAndProcessedFrame) {
		final var corners3f = getCorner3f(cameraParameter);
        
        // read image and convert into gray frame mat
        final var rgbaFrame = Imgcodecs.imread(imageFilePath, -1);
        var grayFrame = new Mat();
        Imgproc.cvtColor(rgbaFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // apply gauss blur before resize to avoid alising error
        final var kSize = new Size(3, 3);
        final double sigmaX = 1;
        Imgproc.GaussianBlur(grayFrame, grayFrame, kSize, sigmaX);

        // actual find corners
        final var patternSize = new Size(
            cameraParameter.calibrationPatternWidth(), 
            cameraParameter.calibrationPatternHeight());
        final var corners = new MatOfPoint2f();
        final var cornersFound = Calib3d.findChessboardCorners(
                grayFrame,
                patternSize,
                corners
                // TODO experiment with flags
                // Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK
                // , -1
        );
        if (cornersFound) {
            // LOGGER.info("-> Corners found; starting optimization...");

            // termination criteria for Subpixel Optimization
            final TermCriteria termCriteria =  new TermCriteria(
                    TermCriteria.EPS + TermCriteria.MAX_ITER,
                    60,
                    0.001);
            // optimize image
            Imgproc.cornerSubPix(
                    grayFrame,
                    corners,
                    new Size(10.5, 10.5), // when no resize consider 22, 22
                    new Size(-1, -1),
                    termCriteria);
            // LOGGER.info("--> optimized");

            // add 3D world and 2D representation
            chessboardData.objectPoints().add(corners3f);
            chessboardData.imagePoints().add(corners);

            // draw chessboard corners
            Calib3d.drawChessboardCorners(
                grayFrame,
                patternSize, 
                corners,
                cornersFound);

            // apply info text
            Imgproc.putText(
                grayFrame, 
                "Captured: " + chessboardData.imagePoints().size(),
                new Point(32, 32), 
                Imgproc.FONT_HERSHEY_DUPLEX, 
                1,
                new Scalar(255, 255, 0), 
                2);
            
            // callback
            final var resizedOrignal = new Mat();
            final var resizedGray = new Mat();
            final var previewSize = new Size(
                rgbaFrame.width() * cameraParameter.scaleFactor(), 
                rgbaFrame.height() * cameraParameter.scaleFactor());
            Imgproc.resize(rgbaFrame, resizedOrignal, previewSize);
            Imgproc.resize(grayFrame, resizedGray, previewSize);

            biConsumerOriginalAndProcessedFrame.accept(
                resizedOrignal,
                resizedGray);
        }
        return cornersFound;
    }

    private static MatOfPoint3f getCorner3f(final CameraParameter cameraParameter) {
		final var width = cameraParameter.calibrationPatternWidth();
		final var height = cameraParameter.calibrationPatternHeight();
        final var squareSize = cameraParameter.calibrationChessboardSquareSizeInMillimeter();
		final var corners3f = new MatOfPoint3f();
        final var point3 = new Point3[(int) (height * width)];
		int cnt = 0;
		for (int i = 0; i < height; ++i) {
			for (int j = 0; j < width; ++j, cnt++) {
				point3[cnt] = new Point3(j * squareSize, i * squareSize, 0.0d);
			}
		}
		corners3f.fromArray(point3);
		return corners3f;
	}

    private static double computeReprojectionErrors(
        ChessboardData chessboardData,
        Mat cameraMatrix,
        MatOfDouble distortionCoefficients,
        List<Mat> rvecs,
        List<Mat> tvecs,
        Mat perViewErrors) {
        
        MatOfPoint2f cornersProjected = new MatOfPoint2f();
        double totalError = 0;
        double error;
        float viewErrors[] = new float[chessboardData.objectPoints().size()];

        int totalPoints = 0;
        for (int i = 0; i < chessboardData.objectPoints().size(); i++) {
            MatOfPoint3f points = new MatOfPoint3f(
                chessboardData.objectPoints().get(i));
            Calib3d.projectPoints(
                points, 
                rvecs.get(i), 
                tvecs.get(i), 
                cameraMatrix, 
                distortionCoefficients,
                cornersProjected);
            error = Core.norm(
                chessboardData.imagePoints().get(i), 
                cornersProjected, 
                Core.NORM_L2);

            int n = chessboardData.objectPoints().get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError += error * error;
            totalPoints += n;
        }
        perViewErrors.create(chessboardData.objectPoints().size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    private CameraCalibrator() {
        // hide constructor
    }
}

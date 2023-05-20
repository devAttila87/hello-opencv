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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        LOGGER.info("objectPoints=" + chessboardData.objectPoints().size() 
            + "; imagePoints=" + chessboardData.imagePoints().size());
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

        LOGGER.info("Saving calibration to files...");
        // Save off camera matrix
        saveDoubleMat(cameraMatrix, String.format("%scamera-matrix.bin", "src/resources/"));
        // Save off distortion coefficients
        saveDoubleMat(distortionCoefficients, String.format("%sdistortion-coefficients.bin", "src/resources/"));

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
                , Calib3d.CALIB_CB_ADAPTIVE_THRESH 
                    + Calib3d.CALIB_CB_NORMALIZE_IMAGE 
                    + Calib3d.CALIB_CB_FAST_CHECK
                // , -1
        );
        if (cornersFound) {
            // termination criteria for Subpixel Optimization
            final TermCriteria termCriteria =  new TermCriteria(
                    TermCriteria.EPS + TermCriteria.MAX_ITER,
                    60,
                    0.001);
            // optimize image
            Imgproc.cornerSubPix(
                    grayFrame,
                    corners,
                    new Size(10.5, 10.5), // when no resize consider 22, 22 otherwise 10.5, 10.5
                    new Size(-1, -1),
                    termCriteria);

            // add 3D world and 2D representation
            chessboardData.objectPoints().add(corners3f);
            chessboardData.imagePoints().add(corners);

            // draw chessboard corners
            Calib3d.drawChessboardCorners(
                rgbaFrame,
                patternSize, 
                corners,
                cornersFound);

            // apply info text
            Imgproc.putText(
                rgbaFrame, 
                "Captured: " + chessboardData.imagePoints().size(),
                new Point(32, 32), 
                Imgproc.FONT_HERSHEY_DUPLEX, 
                3,
                new Scalar(255, 255, 0), 
                3);
            
            // callback
            final var resizedOrignal = new Mat();
            final var resizedGray = new Mat();
            final var previewSize = new Size(
                rgbaFrame.width() * cameraParameter.scaleFactor(), 
                rgbaFrame.height() * cameraParameter.scaleFactor());
            Imgproc.resize(rgbaFrame, resizedOrignal, previewSize);
            Imgproc.resize(grayFrame, resizedGray, previewSize);

			// debug
			DetectionUtil.debugShowImage(resizedOrignal, "corners_" + imageFilePath);

            biConsumerOriginalAndProcessedFrame.accept(
                resizedOrignal,
                resizedGray);
        } else {
            LOGGER.info("No corners found in " + imageFilePath);
        }
        return cornersFound;
    }

	/**
     * Credits sgjava
	 * Load calibration Mats.
	 *
	 * @param camMtxFileName
	 *            Camera matrix file name.
	 * @param distCoFileName
	 *            Distortion coefficients file name.
	 * @return Mat array consisting of cameraMatrix and distCoeffs.
	 */
	public static Mat[] loadCalibration(final String camMtxFileName, final String distCoFileName) {
		final var cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);
		loadDoubleMat(cameraMatrix, camMtxFileName);
		final var distCoeffs = Mat.zeros(5, 1, CvType.CV_64F);
		loadDoubleMat(distCoeffs, distCoFileName);
		return new Mat[] { cameraMatrix, distCoeffs };
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

    /**
     * Credits sgjava
	 * Save Mat of type Double. This has to be done since FileStorage is not
	 * being generated with the OpenCV Java bindings. This method will be slow
	 * with large arrays, but since the calibration parameters are small it's no
	 * big deal.
	 *
	 * @param mat
	 *            Mat to save.
	 * @param fileName
	 *            File to write.
	 */
	private static void saveDoubleMat(final Mat mat, final String fileName) {
		LOGGER.info(String.format("Saving double Mat: %s", fileName));
		final var count = mat.total() * mat.channels();
		final var buff = new double[(int) count];
		mat.get(0, 0, buff);
		try (final var out = new DataOutputStream(new FileOutputStream(fileName))) {
			for (int i = 0; i < buff.length; ++i) {
				out.writeDouble(buff[i]);
			}
		} catch (IOException e) {
			LOGGER.warning(String.format("Exception: %s", e.getMessage()));
		}
	}

	/**
     * Credits sgjava
	 * Load pre-configured Mat from a file.
	 *
	 * @param mat
	 *            Mat configured the same as the saved Mat. This Mat will be
	 *            overwritten with the data in the file. This value is modified
	 *            by JNI code.
	 * @param fileName
	 *            File to read.
	 */
	private static void loadDoubleMat(final Mat mat, final String fileName) {
		LOGGER.info(String.format("Loading double Mat: %s", fileName));
		final var count = mat.total() * mat.channels();
		final List<Double> list = new ArrayList<>();
		try (final var inStream = new DataInputStream(new FileInputStream(fileName))) {
			// Read all Doubles into List
			for (var i = 0; i < count; ++i) {
				// LOGGER.info(String.format("%d", i));
				list.add(inStream.readDouble());
			}
		} catch (IOException e) {
			if (e.getMessage() == null) {
				LOGGER.warning(String.format("EOF reached for: %s", fileName));
			} else {
				LOGGER.warning(String.format("Exception: %s", e.getMessage()));
			}
		}
		// Set byte array to size of List
		final var buff = new double[list.size()];
		// Convert to primitive array
		for (var i = 0; i < buff.length; i++) {
			buff[i] = list.get(i);
		}
		mat.put(0, 0, buff);
	}

    private CameraCalibrator() {
        // hide constructor
    }
}

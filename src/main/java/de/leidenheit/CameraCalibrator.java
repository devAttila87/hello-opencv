package de.leidenheit;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
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

    private final Size mPatternSize = new Size(9, 6);
    private final int mCornersSize = (int) (this.mPatternSize.width * this.mPatternSize.height);
    private boolean mCornersFound = false;
    private MatOfPoint2f mCorners = new MatOfPoint2f();
    private List<Mat> mCornersBuffer = new ArrayList<Mat>();
    private boolean mIsCalibrated = false;

    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    private int mFlags;
    private double mRms;
    private double mSquareSize = 30; // mm
    private Size mImageSize;

    private double mScaleFactor = 0.2; // used to resize images 

    public CameraCalibrator(int width, int height) {
        this.mImageSize = new Size(width, height);
        this.mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT + Calib3d.CALIB_ZERO_TANGENT_DIST
                + Calib3d.CALIB_FIX_ASPECT_RATIO + Calib3d.CALIB_FIX_K4 + Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(this.mCameraMatrix);
        this.mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(this.mDistortionCoefficients);
        LOGGER.info("Instantiated new " + this.getClass());
    }

    /*
    public void processFrame(Mat grayFrame, Mat rgbaFrame) {
        // findCorners(grayFrame);
        renderFrame(rgbaFrame);
    }
     */

    public void calibrate() {
        ArrayList<Mat> rvecs = new ArrayList<Mat>();
        ArrayList<Mat> tvecs = new ArrayList<Mat>();
        Mat reprojectionErrors = new Mat();
        ArrayList<Mat> objectPoints = new ArrayList<Mat>();
        objectPoints.add(Mat.zeros(this.mCornersSize, 1, CvType.CV_32FC3));
        calcBoardCornerPositions(objectPoints.get(0));
        for (int i = 1; i < this.mCornersBuffer.size(); i++) {
            objectPoints.add(objectPoints.get(0));
        }

        Calib3d.calibrateCamera(objectPoints, this.mCornersBuffer, this.mImageSize, this.mCameraMatrix,
                this.mDistortionCoefficients, rvecs, tvecs, this.mFlags);

        this.mIsCalibrated = Core.checkRange(this.mCameraMatrix) 
            && Core.checkRange(this.mDistortionCoefficients);

        this.mRms = computeReprojectionErrors(objectPoints, rvecs, tvecs, reprojectionErrors);

        LOGGER.info("CalibrationSuccessful=" + this.mIsCalibrated
            + "\n\nobjectPoints=" + objectPoints
            + "\n\nrvecs=" + rvecs
            + "\n\ntvecs=" + tvecs
            + "\ndistortionCoefficients=" + this.mDistortionCoefficients
            + "\n\nrms=" + this.mRms
        );
    }

    public void clearCorners() {
        this.mCornersBuffer.clear();
    }

    private void calcBoardCornerPositions(Mat corners) {
        final int cn = 3;
        float positions[] = new float[this.mCornersSize * cn];

        for (int i = 0; i < this.mPatternSize.height; i++) {
            for (int j = 0; j < this.mPatternSize.width * cn; j += cn) {
                positions[(int) (i * this.mPatternSize.width * cn + j + 0)] = (2 * (j / cn) + i % 2)
                        * (float) this.mSquareSize;
                positions[(int) (i * this.mPatternSize.width * cn + j + 1)] = i * (float) this.mSquareSize;
                positions[(int) (i * this.mPatternSize.width * cn + j + 2)] = 0;
            }
        }
        corners.create(this.mCornersSize, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }

    private double computeReprojectionErrors(List<Mat> objectPoints, List<Mat> rvecs, List<Mat> tvecs,
                                             Mat perViewErrors) {
        MatOfPoint2f cornersProjected = new MatOfPoint2f();
        double totalError = 0;
        double error;
        float viewErrors[] = new float[objectPoints.size()];

        MatOfDouble distortionCoefficients = new MatOfDouble(this.mDistortionCoefficients);
        int totalPoints = 0;
        for (int i = 0; i < objectPoints.size(); i++) {
            MatOfPoint3f points = new MatOfPoint3f(objectPoints.get(i));
            Calib3d.projectPoints(points, rvecs.get(i), tvecs.get(i), this.mCameraMatrix, distortionCoefficients,
                    cornersProjected);
            error = Core.norm(this.mCornersBuffer.get(i), cornersProjected, Core.NORM_L2);

            int n = objectPoints.get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError += error * error;
            totalPoints += n;
        }
        perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    public boolean findCorners(String imageFilePath, BiConsumer<Mat, Mat> biConsumerOriginalAndProcessedFrame) {
        // read image and convert into gray frame mat
        final var rgbaFrame = Imgcodecs.imread(imageFilePath, -1);
        var grayFrame = new Mat();
        Imgproc.cvtColor(rgbaFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // apply gauss blur before resize to avoid alising error
        final var kSize = new Size(3, 3);
        final double sigmaX = 1;
        Imgproc.GaussianBlur(grayFrame, grayFrame, kSize, sigmaX);

        // resize blurred image 
        final var dSize = new Size(
            grayFrame.width() * mScaleFactor,
            grayFrame.height() * mScaleFactor);
        final var resizedGrayFrame = new Mat();    
        final var resizedRgbaFrame = new Mat();
        Imgproc.resize(grayFrame, resizedGrayFrame, dSize);
        Imgproc.resize(rgbaFrame, resizedRgbaFrame, dSize);

        // actual find corners
        LOGGER.info("Searching for corners in " + imageFilePath + "...");
        this.mCornersFound = Calib3d.findChessboardCorners(
                resizedGrayFrame,
                mPatternSize,
                this.mCorners,
                // TODO experiment with flags
                // Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK
                -1
        );
        if (this.mCornersFound) {
            LOGGER.info("-> Corners found; starting optimization...");

            // termination criteria for Subpixel Optimization
            final TermCriteria termCriteria =  new TermCriteria(
                    TermCriteria.EPS + TermCriteria.MAX_ITER,
                    60,
                    0.001);
            // optimize image
            Imgproc.cornerSubPix(
                    resizedGrayFrame,
                    this.mCorners,
                    new Size(11, 11), // when no resize consider 22, 22
                    new Size(-1, -1),
                    termCriteria);
            LOGGER.info("--> optimized");

            // draw chessboard corners
            final var resizedRgbaWithCornersFrame = resizedRgbaFrame.clone();
            Calib3d.drawChessboardCorners(
                resizedRgbaWithCornersFrame,
                mPatternSize, 
                mCorners,
                mCornersFound);

            // finally fill corners buffer
            addCorners();

            // apply info text
            Imgproc.putText(
                resizedRgbaWithCornersFrame, 
                "Captured: " + this.mCornersBuffer.size(),
                new Point(32, 32), 
                Imgproc.FONT_HERSHEY_DUPLEX, 
                1,
                new Scalar(255, 255, 0), 
                2);
            
            // callback
            biConsumerOriginalAndProcessedFrame.accept(
                resizedRgbaFrame,
                resizedRgbaWithCornersFrame);
        }
        return this.mCornersFound;
    }

    public void addCorners() {
        if (this.mCornersFound) {
            this.mCornersBuffer.add(this.mCorners.clone());
        }
    }

    /*
    private void drawPoints(Mat rgbaFrame) {
        Calib3d.drawChessboardCorners(
                rgbaFrame,
                this.mPatternSize,
                this.mCorners,
                this.mCornersFound);
    }
    */

    /*
    private void renderFrame(Mat rgbaFrame) {
        drawPoints(rgbaFrame);

        Imgproc.putText(rgbaFrame, "Captured: " + this.mCornersBuffer.size(),
                new Point(rgbaFrame.cols() / 3 * 2, rgbaFrame.rows() * 0.1), Imgproc.FONT_HERSHEY_SIMPLEX, 1.0,
                new Scalar(255, 255, 0));
    }
    */

    public Mat getCameraMatrix() {
        return this.mCameraMatrix;
    }

    public void setCameraMatrix(Mat cameraMatrix) {
        this.mCameraMatrix = cameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return this.mDistortionCoefficients;
    }

    public void setDistortionCoefficients(Mat distortionCoefficients) {
        this.mDistortionCoefficients = distortionCoefficients;
    }

    public int getCornersBufferSize() {
        return this.mCornersBuffer.size();
    }

    public MatOfPoint2f getCorners() {
        return this.mCorners;
    }

    public List<Mat> getCornersBuffer() {
        return this.mCornersBuffer;
    } 

    public void setCornersBuffer(List<Mat> cornersBuffer) {
        this.mCornersBuffer = cornersBuffer;
    }

    public double getAvgReprojectionError() {
        return this.mRms;
    }

    public boolean isCalibrated() {
        return this.mIsCalibrated;
    }

    public void setCalibrated() {
        this.mIsCalibrated = true;
    }

    public void setScaleFactor(double scaleFactor) {
        this.mScaleFactor = scaleFactor;
    }

    public double getScaleFactor() {
        return this.mScaleFactor;
    }
}

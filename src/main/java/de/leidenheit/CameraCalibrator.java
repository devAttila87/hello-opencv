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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class CameraCalibrator {

    private static final Logger LOGGER = Logger.getLogger(CameraCalibrator.class.toString());

    private final Size mPatternSize = new Size(9, 6);
    private final int mCornersSize = (int) (this.mPatternSize.width * this.mPatternSize.height);
    private boolean mCornersFound = false;
    // private MatOfPoint2f mCorners = new MatOfPoint2f();
    // private List<Mat> mCornersBuffer = new ArrayList<Mat>();
    private boolean mIsCalibrated = false;
    private List<Mat> mObjectPoints = new ArrayList<Mat>();
    private List<Mat> mImagePoints = new ArrayList<Mat>();
		

    
    // private Mat mCameraMatrix = new Mat();
    // private Mat mDistortionCoefficients = new Mat();
    private Mat mCameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
    
    private MatOfDouble mDistortionCoefficients = new MatOfDouble(); 
    // Mat.zeros(5, 1, CvType.CV_64FC1);
    private int mFlags;
    private double mAvgReprojectionErrors;
    private double mSquareSize = 30d; // mm
    private Size mImageSize;

    private double mScaleFactor = 0.5d; // used to resize images 

    public CameraCalibrator(int width, int height) {
        
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
        
        this.mImageSize = new Size(width, height);
        this.mFlags = 0
            + Calib3d.CALIB_FIX_PRINCIPAL_POINT // marginal
            + Calib3d.CALIB_ZERO_TANGENT_DIST // marginal
            // + Calib3d.CALIB_FIX_ASPECT_RATIO increases by 1.x
            + Calib3d.CALIB_FIX_K4 // marginal
            + Calib3d.CALIB_FIX_K5; // marginal
            ;
        // Mat.eye(3, 3, CvType.CV_64FC1).copyTo(this.mCameraMatrix);
        // this.mCameraMatrix.put(0, 0, 1.0);
        // Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(this.mDistortionCoefficients);
        // LOGGER.info("Instantiated new " + this.getClass());
    }

    /*
    public void processFrame(Mat grayFrame, Mat rgbaFrame) {
        // findCorners(grayFrame);
        renderFrame(rgbaFrame);
    }
     */

    public CalibrationData calibrate() {

        ArrayList<Mat> rvecs = new ArrayList<Mat>();
        ArrayList<Mat> tvecs = new ArrayList<Mat>();
        Mat reprojectionErrors = new Mat();

        LOGGER.info("\nobjectPoints= " + this.mObjectPoints.size() 
            + "\nimagePoints=" + this.mImagePoints.size());
        
        Calib3d.calibrateCamera(
            this.mObjectPoints,
            this.mImagePoints, 
            this.mImageSize, 
            this.mCameraMatrix, 
            this.mDistortionCoefficients, 
            rvecs, 
            tvecs,
            this.mFlags
            );
        this.mIsCalibrated = Core.checkRange(this.mCameraMatrix) && Core.checkRange(this.mDistortionCoefficients);
        this.mAvgReprojectionErrors = computeReprojectionErrors(this.mObjectPoints, rvecs, tvecs, reprojectionErrors);
        LOGGER.info("CalibrationSuccessful=" + this.mIsCalibrated
        //    + "\n\nobjectPoints=" + objectPoints
        //    + "\n\nrvecs=" + rvecs
        //    + "\n\ntvecs=" + tvecs
        //    + "\ndistortionCoefficients=" + this.mDistortionCoefficients
            + "\n\navgReprojectionErrors=" + this.mAvgReprojectionErrors);

        return new CalibrationData(
            mCameraMatrix, 
            mDistortionCoefficients,
            rvecs,
            tvecs,
            mAvgReprojectionErrors,
            mIsCalibrated);
    }

    public Mat distortFunction(String imageFilePath) {
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
        final var roi = new Rect();
        final var optimalMatrix = Calib3d.getOptimalNewCameraMatrix(
            mCameraMatrix, 
            mDistortionCoefficients, 
            dgbImageMat.size(), 
            1, 
            dgbImageMat.size(),
            roi);
        //LOGGER.info("\n#########\n\tOptimal Camera Matrix: " + optimalMatrix.dump()  
        //    + "\nROI=" + roi);

        Calib3d.undistort(
            dgbImageMat, 
            dgbUndistortedImageMat, 
            mCameraMatrix, 
            mDistortionCoefficients,
            optimalMatrix);
        
        /*

        // crop the image based on ROI
        int x = (int) roi.tl().x;
        int y = (int) roi.tl().y;
        int w = (int) (roi.br().x - roi.tl().x);
        int h = (int) (roi.br().y - roi.tl().y);
        Mat cropped = new Mat(dgbUndistortedImageMat, new org.opencv.core.Rect(x, y, w, h));
        HighGui.imshow("cropped", cropped);
        HighGui.waitKey(20);
        HighGui.destroyWindow("cropped");

        // resize
        Imgproc.resize(dgbImageMat, dgbImageMat, 
            new Size(
                dgbImageMat.width()*mScaleFactor, 
                dgbImageMat.height()*mScaleFactor));
                
        Imgproc.resize(dgbUndistortedImageMat, dgbUndistortedImageMat, 
            new Size(
                dgbUndistortedImageMat.width()*mScaleFactor, 
                dgbUndistortedImageMat.height()*mScaleFactor));

        
        HighGui.imshow("before_dist", dgbImageMat);
        HighGui.waitKey(20);
        HighGui.destroyWindow("before_dist");
        HighGui.imshow("after_dist", dgbUndistortedImageMat);
        HighGui.waitKey(20);
        HighGui.destroyWindow("after_dist");
        */

        return dgbUndistortedImageMat;
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
            error = Core.norm(this.mImagePoints.get(i), cornersProjected, Core.NORM_L2);

            int n = objectPoints.get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError += error * error;
            totalPoints += n;
        }
        perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    public MatOfPoint3f getCorner3f() {
		final var corners3f = new MatOfPoint3f();
		final var point3 = new Point3[(int) (mPatternSize.height * mPatternSize.width)];
		int cnt = 0;
		for (int i = 0; i < mPatternSize.height; ++i) {
			for (int j = 0; j < mPatternSize.width; ++j, cnt++) {
				point3[cnt] = new Point3(j * mSquareSize, i * mSquareSize, 0.0d);
			}
		}
		corners3f.fromArray(point3);
		return corners3f;
	}

    public boolean findCorners(String imageFilePath, BiConsumer<Mat, Mat> biConsumerOriginalAndProcessedFrame) {
		final var corners3f = getCorner3f();
        
        // read image and convert into gray frame mat
        final var rgbaFrame = Imgcodecs.imread(imageFilePath, -1);
        var grayFrame = new Mat();
        Imgproc.cvtColor(rgbaFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // apply gauss blur before resize to avoid alising error
        final var kSize = new Size(3, 3);
        final double sigmaX = 1;
        Imgproc.GaussianBlur(grayFrame, grayFrame, kSize, sigmaX);

        // actual find corners
        final var corners = new MatOfPoint2f();
        this.mCornersFound = Calib3d.findChessboardCorners(
                grayFrame,
                mPatternSize,
                corners
                // TODO experiment with flags
                // Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK
                // , -1
        );
        if (this.mCornersFound) {
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
            this.mObjectPoints.add(corners3f);
            this.mImagePoints.add(corners);

            // draw chessboard corners
            Calib3d.drawChessboardCorners(
                grayFrame,
                mPatternSize, 
                corners,
                mCornersFound);

            // apply info text
            Imgproc.putText(
                grayFrame, 
                "Captured: " + this.mImagePoints.size(),
                new Point(32, 32), 
                Imgproc.FONT_HERSHEY_DUPLEX, 
                1,
                new Scalar(255, 255, 0), 
                2);
            
            // callback
            final var resizedOrignal = new Mat();
            final var resizedGray = new Mat();
            final var previewSize = new Size(
                rgbaFrame.width() * mScaleFactor, 
                rgbaFrame.height() * mScaleFactor);
            Imgproc.resize(rgbaFrame, resizedOrignal, previewSize);
            Imgproc.resize(grayFrame, resizedGray, previewSize);

            biConsumerOriginalAndProcessedFrame.accept(
                resizedOrignal,
                resizedGray);
        }
        return this.mCornersFound;
    }

    public Mat getCameraMatrix() {
        return this.mCameraMatrix;
    }

    public void setCameraMatrix(Mat cameraMatrix) {
        this.mCameraMatrix = cameraMatrix;
    }

    public MatOfDouble getDistortionCoefficients() {
        return this.mDistortionCoefficients;
    }

    public void setDistortionCoefficients(MatOfDouble distortionCoefficients) {
        this.mDistortionCoefficients = distortionCoefficients;
    }

    public double getAvgReprojectionError() {
        return this.mAvgReprojectionErrors;
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

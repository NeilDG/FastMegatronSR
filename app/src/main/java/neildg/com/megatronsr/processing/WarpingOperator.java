package neildg.com.megatronsr.processing;

import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import neildg.com.megatronsr.constants.FilenameConstants;
import neildg.com.megatronsr.io.ImageReader;
import neildg.com.megatronsr.io.ImageWriter;
import neildg.com.megatronsr.preprocessing.BitmapURIRepository;
import neildg.com.megatronsr.ui.ProgressDialogHandler;

/**
 * Created by NeilDG on 3/6/2016.
 */
public class WarpingOperator {
    private final static String TAG = "WarpingOperator";

    private MatOfKeyPoint refKeypoint;
    private List<MatOfDMatch> goodMatchList;
    private List<MatOfKeyPoint> keyPointList;

    private Mat referenceMat;

    private Mat warpedMat = new Mat();
    private Mat outputMat = new Mat();

    public WarpingOperator(MatOfKeyPoint refKeypoint, List<MatOfDMatch> goodMatchList, List<MatOfKeyPoint> keyPointList) {
        this.goodMatchList = goodMatchList;
        this.keyPointList = keyPointList;
        this.refKeypoint = refKeypoint;

        this.referenceMat = ImageReader.getInstance().imReadOpenCV(FilenameConstants.INITIAL_HR_PREFIX_STRING + 0 + ".jpg");
        this.outputMat = new Mat(this.referenceMat.size(), this.referenceMat.type());
        this.referenceMat.copyTo(this.outputMat);

        ImageWriter.getInstance().saveMatrixToImage(this.outputMat, "holderimage");
    }

    public void perform() {
        ProgressDialogHandler.getInstance().hideDialog();

        Log.d(TAG,"Homography warping");

        int numImages = BitmapURIRepository.getInstance().getNumImages();
        for (int i = 1; i < numImages; i++) {
            Mat comparingMat = ImageReader.getInstance().imReadOpenCV(FilenameConstants.INITIAL_HR_PREFIX_STRING + i + ".jpg");

            ProgressDialogHandler.getInstance().showDialog("Image warping", "Warping image " + i + " to reference image.");

            this.warpedMat = new Mat();
            this.warpImage(this.goodMatchList.get(i - 1), this.keyPointList.get(i - 1), comparingMat);

            //TODO: test only
            this.warpedMat.setTo(Scalar.all(255));
            this.overrideMat(this.warpedMat, this.outputMat);

            ImageWriter.getInstance().saveMatrixToImage(this.outputMat, "blend_" + i);
            this.warpedMat.release();
            //Core.add(this.warpedMat, this.outputMat, this.outputMat);
            ProgressDialogHandler.getInstance().hideDialog();
        }

        ImageWriter.getInstance().saveMatrixToImage(this.outputMat, FilenameConstants.HR_PROCESSED_STRING);
    }

    private void warpImage(MatOfDMatch goodMatch, MatOfKeyPoint candidateKeypoint, Mat candidateMat) {
        MatOfPoint2f matOfPoint1 = new MatOfPoint2f();
        MatOfPoint2f matOfPoint2 = new MatOfPoint2f();

        KeyPoint[] keyPoints1 = this.refKeypoint.toArray();
        KeyPoint[] keyPoints2 = candidateKeypoint.toArray();

        List<Point> pointList1 = new ArrayList<>();
        List<Point> pointList2 = new ArrayList<>();

        DMatch[] dMatchArray = goodMatch.toArray();

        for(int i = 0; i < dMatchArray.length; i++) {
            Log.d(TAG, "DMATCHES" + dMatchArray[i].toString());

            pointList1.add(keyPoints1[dMatchArray[i].queryIdx].pt);
            pointList2.add(keyPoints2[dMatchArray[i].trainIdx].pt);
        }

        matOfPoint1.fromList(pointList1); matOfPoint2.fromList(pointList2);

        //((M0.type() == CV_32F || M0.type() == CV_64F) && M0.rows == 3 && M0.cols == 3)

        Log.d(TAG, "Homography pre info: matOfPoint1 ROWS: " + matOfPoint1.rows() + " matOfPoint1 COLS: " + matOfPoint1.cols());
        Log.d(TAG, "Homography pre info: matOfPoint2 ROWS: " + matOfPoint2.rows() + " matOfPoint2 COLS: " + matOfPoint2.cols());

        Mat mask = new Mat();
        Mat homography = Calib3d.findHomography(matOfPoint2,matOfPoint1, Calib3d.RANSAC,1);
        mask.release();
        Log.d(TAG, "Homography info: ROWS: " + homography.rows() + " COLS: " + homography.cols());

        Imgproc.warpPerspective(candidateMat, this.warpedMat, homography, this.warpedMat.size(), Imgproc.INTER_CUBIC, Core.BORDER_TRANSPARENT, Scalar.all(0));
    }

    private void overrideMat(Mat warpedMat, Mat destinationMat) {
        for(int row = 0; row < warpedMat.rows(); row++) {
            for(int col = 0; col < warpedMat.cols(); col++) {
                double[] pixelData = warpedMat.get(row,col);
                destinationMat.put(row,col,pixelData);
            }
        }
    }

    private void copyToMat(Mat warpedMat, Mat destinationMat) {
        Mat mask = new Mat(); this.warpedMat.copyTo(mask);
        mask.convertTo(mask, CvType.CV_8U);

        warpedMat.copyTo(destinationMat, mask);
    }
}
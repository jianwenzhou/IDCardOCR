package org.opencv.admin.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import org.opencv.admin.ThreadPoolProxy;
import org.opencv.admin.bean.IDCardBean;
import org.opencv.admin.util.ImageUtil;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

/**
 * 作者：Zhou on 2019/6/3 17:04
 * 简介：
 */
public class BackCard {
    private static final float DEFAULT_CARD_WIDTH = 648; //720P下的卡片宽度
    private static final int DEFAULT_CARD_MORPHOLOGY = 20; //720P下的形态内核大小
    private Rect mIssuanceAddressRect;
    private Rect mPeriodValidityRect;
    private CyclicBarrier mBackBarrier;
    private IDCardBean mCardBean;
    private ThreadPoolProxy mThreadPool;
    private int mSrcWidth;
    private int mSrcHeight;

    public BackCard(CyclicBarrier backBarrier, IDCardBean cardBean, ThreadPoolProxy threadPool) {
        mBackBarrier = backBarrier;
        mCardBean = cardBean;
        mThreadPool = threadPool;

    }

    private double getAvg(Bitmap bitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
        Scalar mean = Core.mean(src);
        Log.e("OCR", "mean=" + mean);
        return mean.val[0];
    }

    public Bitmap changeContrast(Bitmap src) {
//        double avg = getAvg(src);
        Bitmap dst = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_4444);
//        float contrast = (float) ((avg) / 128.0);
        float contrast = 2;

        ColorMatrix cMatrix = new ColorMatrix();
        cMatrix.set(new float[]{contrast, 0, 0, 0, 0, 0,
                contrast, 0, 0, 0,// 改变对比度
                0, 0, contrast, 0, 0, 0, 0, 0, 1, 0});
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cMatrix));
        Canvas canvas = new Canvas(dst);
        // 在Canvas上绘制一个已经存在的Bitmap。这样，dstBitmap就和srcBitmap一摸一样了
        canvas.drawBitmap(src, 0, 0, paint);
        int w = dst.getWidth();
        int h = dst.getHeight();
        Bitmap crop = Bitmap.createBitmap(dst,//原图
                w/20,//图片裁剪横坐标开始位置
                h/20,//图片裁剪纵坐标开始位置
                w - (w/10),//要裁剪的宽度
                h - (h/10));//要裁剪的高度
        dst.recycle();

        return crop;
    }

    public int handlerImageBack(Bitmap bitmap) {
        Bitmap changeBit = changeContrast(bitmap);
        mSrcWidth = changeBit.getWidth();
        mSrcHeight = changeBit.getHeight();
        float cardRatio = mSrcWidth / DEFAULT_CARD_WIDTH;
//        放大  灰化  二值化  锐化
        Mat bit = new Mat();
        Mat gary = new Mat();
        Mat threshold = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(changeBit, bit);
        Imgproc.cvtColor(bit, gary, Imgproc.COLOR_BGRA2GRAY);

        //otsu 二值化
        Imgproc.threshold(gary, threshold, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);

        //取反-变成黑底白字
        Core.bitwise_not(threshold, dst);

        int size = (int) (DEFAULT_CARD_MORPHOLOGY * cardRatio);

        //过滤噪点
//        originalImageFilterRect(threshold, dst);
        Mat structuringElement = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(size, size / 2));
        Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_DILATE, structuringElement);
        //过滤无效区域
//        morphologyImageFilterRectBack(dst);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(dst, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.size() == 0) {
            Log.e("OCR","contours 为0");
            setBitmap(threshold);
            return RecognitionCard.STABLE_ERROR;
        }

        Iterator<MatOfPoint> iterator = contours.iterator();
        while (iterator.hasNext()) {
            MatOfPoint matOfPoint = iterator.next();
            Rect rec = Imgproc.boundingRect(matOfPoint);
            if (rec.width > mSrcWidth / 3 && rec.y > mSrcHeight / 2 && rec.height > mSrcHeight / 15) {
                //有效期限
                Log.e("OCR", "有效期限 rec ====" + rec);
                mPeriodValidityRect = rec;
                Mat periodValidity = new Mat();
                gary.submat(mPeriodValidityRect).copyTo(periodValidity);
                Imgproc.rectangle(gary, new Point(mPeriodValidityRect.x, mPeriodValidityRect.y), new Point(mPeriodValidityRect.x + mPeriodValidityRect.width, mPeriodValidityRect.y + mPeriodValidityRect.height), new Scalar(0, 0, 255));
                Bitmap val = Bitmap.createBitmap(periodValidity.width(), periodValidity.height(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(periodValidity, val);
                Worker worker = new Worker(mBackBarrier, val, Worker.CARD_PERIOD_VALIDITY_FLAG, mCardBean);
                mThreadPool.execute(worker);
                iterator.remove();
                periodValidity.release();
            }
        }

        if (mPeriodValidityRect == null) {
            Log.e("OCR", "mPeriodValidityRect为null");
            setBitmap(threshold);
            return RecognitionCard.STABLE_ERROR;
        }

        for (int i = 0; i < contours.size(); i++) {
            Rect rec = Imgproc.boundingRect(contours.get(i));
            if ((rec.x >= mPeriodValidityRect.x - 15 && rec.x <= mPeriodValidityRect.x + 15)) {
                if (rec.width > mSrcWidth / 5 && rec.y > mSrcHeight / 2 && rec.height > mSrcHeight / 15) {
                    //签发机关
                    Log.e("OCR", "签发机关 rec ====" + rec);
                    mIssuanceAddressRect=rec;
                    Mat issuanceAddressMat = new Mat();
                    gary.submat(rec).copyTo(issuanceAddressMat);
                    Imgproc.rectangle(gary, new Point(rec.x, rec.y), new Point(rec.x + rec.width, rec.y + rec.height), new Scalar(0, 0, 255));
                    Bitmap iss = Bitmap.createBitmap(issuanceAddressMat.width(), issuanceAddressMat.height(), Bitmap.Config.ARGB_4444);
                    Utils.matToBitmap(issuanceAddressMat, iss);
                    Worker worker = new Worker(mBackBarrier, iss, Worker.CARD_ISSUANCE_ADDRESS_FLAG, mCardBean);
                    mThreadPool.execute(worker);
                    issuanceAddressMat.release();
                }
            }
        }
        if (mIssuanceAddressRect == null) {
            Log.e("OCR", "mIssuanceAddressRect为null");
            setBitmap(threshold);
            return RecognitionCard.STABLE_ERROR;
        }
        setBitmap(gary);
        bit.release();
//        gary.release();
        threshold.release();
        dst.release();
        structuringElement.release();

        return RecognitionCard.RECOGNIT_OK;
    }

    private void setBitmap(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(mat,bmp);
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(sdPath + File.separator + "points.jpg");
        ImageUtil.save(bmp,file);
    }

    private void morphologyImageFilterRectBack(Mat mat) {

        Mat hireachy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mat, contours, hireachy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        int minH = mat.rows();
        int minW = mat.cols();

        int matArea = mat.rows() * mat.cols();
        for (int i = 0; i < contours.size(); i++) {
            Rect rec = Imgproc.boundingRect(contours.get(i));
            int area = rec.width * rec.height;

            //面积太小的过滤
            if (area < matArea / 500) {
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }
            //面积太大的过滤
            if (area > matArea / 6) {
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }
            //高度太高的过滤
            if (rec.height > (minH / 8)) {
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }

            //高度太低的过滤
            if (rec.height <= (minH / 15)) {
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }

            //宽度太小的过滤
            if (rec.height <= (minW / 40)) {
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
            }

        }
        hireachy.release();
    }

    private void originalImageFilterRect(Mat src, Mat dst) {
        Mat hireachy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(dst, contours, hireachy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int i = 0; i < contours.size(); i++) {
            Rect rec = Imgproc.boundingRect(contours.get(i));
            int area = rec.width * rec.height;
            //面积太小的过滤
            if (area < (mSrcWidth * mSrcHeight) / 20000) {
                Imgproc.drawContours(src, contours, i, new Scalar(255, 255, 255), -1);
                Imgproc.drawContours(dst, contours, i, new Scalar(0, 0, 0), -1);
            }
        }
    }

}

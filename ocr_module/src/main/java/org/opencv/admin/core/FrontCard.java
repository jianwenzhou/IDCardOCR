package org.opencv.admin.core;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.NonNull;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

/**
 * 作者：Zhou on 2019/6/3 17:04
 * 简介：
 */
public class FrontCard {
    private static final float DEFAULT_CARD_WIDTH = 648; //720P下的卡片宽度
    private static final int DEFAULT_CARD_MORPHOLOGY = 18; //720P下的形态内核大小
    private Rect mAddressRect;
    private CyclicBarrier mFrontBarrier;
    private IDCardBean mCardBean;
    private ThreadPoolProxy mThreadPool;
    private int mSrcWidth;
    private int mSrcHeight;
    private Rect mCardNumber;

    public FrontCard(CyclicBarrier frontBarrier, IDCardBean cardBean, ThreadPoolProxy threadPool) {
        mFrontBarrier = frontBarrier;
        mCardBean = cardBean;
        mThreadPool = threadPool;
    }

    public int handlerImageFront(Bitmap bitmap) {
        long x = System.currentTimeMillis();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Log.e("OCR", "bitmap=" + bitmap);
        Bitmap crop = Bitmap.createBitmap(bitmap,//原图
                w / 20,//图片裁剪横坐标开始位置
                h / 20,//图片裁剪纵坐标开始位置
                w - (w / 10),//要裁剪的宽度
                h - (h / 10));//要裁剪的高度
        mSrcWidth = crop.getWidth();
        mSrcHeight = crop.getHeight();
        float cardRatio = mSrcWidth / DEFAULT_CARD_WIDTH;
        List<Rect> rects = new ArrayList<>();
//        放大  灰化  二值化  锐化
        Mat bit = new Mat();
        Mat gary = new Mat();
        Mat threshold = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(crop, bit);
        Imgproc.cvtColor(bit, gary, Imgproc.COLOR_BGRA2GRAY);
        //otsu 二值化
        Imgproc.threshold(gary, threshold, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);

        //取反-变成黑底白字
        Core.bitwise_not(threshold, dst);

        int size = (int) (DEFAULT_CARD_MORPHOLOGY * cardRatio);
        //过滤噪点
        originalImageFilterRect(threshold, dst);
        //膨胀
        Log.e("OCR", "膨胀size =" + size);
        Mat structuringElement = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(size, size));
        Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_DILATE, structuringElement);
        //过滤无效区域
        morphologyImageFilterRectFront(threshold, dst);
        //寻找兴趣区域
        List<MatOfPoint> newContours = findNeedRect(gary, dst);

        if (mAddressRect == null || mCardNumber == null) {
            Log.e("OCR", "没有找到地址或者号码区域");
            setBitmap(threshold);
            return RecognitionCard.STABLE_ERROR;
        }

        Rect rect = new Rect();
        rect.x = 0;
        rect.y = 0;
        rect.width = mAddressRect.x + mAddressRect.width;
        rect.height = mAddressRect.y + mAddressRect.height;
        for (int i = 0; i < newContours.size(); i++) {
            Rect rec = Imgproc.boundingRect(newContours.get(i));
            if (rec.x > rect.width || rec.y > rect.height) {
                Imgproc.drawContours(threshold, newContours, i, new Scalar(255), -1);
            }
        }
        Mat cutting = threshold.clone();
        //取反-变成黑底白字
        Core.bitwise_not(cutting, cutting);
        Mat s = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(size / 2, size));
        Imgproc.morphologyEx(cutting, cutting, Imgproc.MORPH_DILATE, s);
        List<MatOfPoint> points = new ArrayList<>();
        Imgproc.findContours(cutting, points, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Log.e("OCR", "points =" + points.size());
        for (int i = 0; i < points.size(); i++) {
            Rect rec = Imgproc.boundingRect(points.get(i));
            if ((rec.x >= mAddressRect.x - 20 && rec.x <= mAddressRect.x + 20)) {
                rects.add(rec);
            }
        }
        if (rects.size() < 4) {
            Log.e("OCR", "相等X点的rect少于四个");
            setBitmap(threshold);
            return RecognitionCard.STABLE_ERROR;
        }
        Collections.sort(rects, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Rect r1 = (Rect) o1;
                Rect r2 = (Rect) o2;
                return r1.y - r2.y;
            }
        });

        int state = findValidArea(rects, gary, points);
        setBitmap(gary);
        bit.release();
//        gary.release();
        threshold.release();
        dst.release();
        structuringElement.release();
        long y = System.currentTimeMillis();
        Log.e("OCR", "第二阶段 耗时：=" + (y - x) / 1000.0f);
        return state;
    }

    private int findValidArea(List<Rect> rects, Mat mat, List<MatOfPoint> newContours) {
        Rect name = rects.get(0);
        if (name != null) {
            //姓名
            Log.e("OCR", "姓名 =" + name);

            Mat nameMat = new Mat();
            mat.submat(name).copyTo(nameMat);
            Imgproc.rectangle(mat, new Point(name.x, name.y), new Point(name.x + name.width, name.y + name.height), new Scalar(0, 0, 255));
            Bitmap bitmap = Bitmap.createBitmap(nameMat.width(), nameMat.height(), Bitmap.Config.ARGB_4444);
            Utils.matToBitmap(nameMat, bitmap);
            Worker worker = new Worker(mFrontBarrier, bitmap, Worker.CARD_NAME_FLAG, mCardBean);
            mThreadPool.execute(worker);
            nameMat.release();
        } else {
            Log.e("OCR", "姓名为null");
            return RecognitionCard.STABLE_ERROR;
        }

        Rect sex = rects.get(1);
        if (sex != null) {
            //性别
            Log.e("OCR", "性别 =" + sex);

            Mat sexMat = new Mat();
            mat.submat(sex).copyTo(sexMat);
            Imgproc.rectangle(mat, new Point(sex.x, sex.y), new Point(sex.x + sex.width, sex.y + sex.height), new Scalar(0, 0, 255));
            Bitmap bitmap = Bitmap.createBitmap(sexMat.width(), sexMat.height(), Bitmap.Config.ARGB_4444);
            Utils.matToBitmap(sexMat, bitmap);
            Worker worker = new Worker(mFrontBarrier, bitmap, Worker.CARD_SEX_FLAG, mCardBean);
            mThreadPool.execute(worker);
            sexMat.release();

        } else {
            Log.e("OCR", "性别为null");
            return RecognitionCard.STABLE_ERROR;
        }

        List<Rect> sexs = new ArrayList<>();
        for (int i = 0; i < newContours.size(); i++) {
            Rect rec = Imgproc.boundingRect(newContours.get(i));
            if ((rec.y >= sex.y - 20 && rec.y <= sex.y + 20)) {
                sexs.add(rec);
            }
        }
        Collections.sort(sexs, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Rect r1 = (Rect) o1;
                Rect r2 = (Rect) o2;
                return r1.x - r2.x;
            }
        });
        if (sexs.size() != 6) {
            Log.e("OCR", "性别轮廓不为6");
            return RecognitionCard.STABLE_ERROR;
        }
        //民族
        Rect nation = sexs.get(5);
        Log.e("OCR", "民族 =" + nation);
        Mat ethnicMat = new Mat();
        mat.submat(nation).copyTo(ethnicMat);
        Imgproc.rectangle(mat, new Point(nation.x, nation.y), new Point(nation.width + nation.x, nation.height + nation.y), new Scalar(0, 0, 255));
        Bitmap bitmap = Bitmap.createBitmap(ethnicMat.width(), ethnicMat.height(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(ethnicMat, bitmap);
        Worker worker = new Worker(mFrontBarrier, bitmap, Worker.CARD_ETHNIC_FLAG, mCardBean);
        mThreadPool.execute(worker);
        ethnicMat.release();

        return RecognitionCard.RECOGNIT_OK;
    }

    @NonNull
    private List<MatOfPoint> findNeedRect(Mat gary, Mat mat) {
        List<MatOfPoint> newContours = new ArrayList<>();
        Imgproc.findContours(mat, newContours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        int minh = mat.rows();
        int minW = mat.cols();
        for (int i = 0; i < newContours.size(); i++) {
            Rect rec = Imgproc.boundingRect(newContours.get(i));
            if (rec.width > (minW / 2) && rec.width < (minW * 2 / 3) && rec.height < (minh / 8) && rec.height > (minh / 12)) {
//                Log.e("OCR", "身份证号码 =" + rec);
                //身份证卡号
                mCardNumber = rec;
                Mat cardIDImage = new Mat();
                gary.submat(rec).copyTo(cardIDImage);
                Imgproc.rectangle(gary, new Point(rec.x, rec.y), new Point(rec.x + rec.width, rec.y + rec.height), new Scalar(0, 0, 255));
                Bitmap bitmap = Bitmap.createBitmap(cardIDImage.width(), cardIDImage.height(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(cardIDImage, bitmap);
                Worker worker = new Worker(mFrontBarrier, bitmap, Worker.CARD_NUMBER_FLAG, mCardBean);
                mThreadPool.execute(worker);
                cardIDImage.release();
            }
            if ((rec.width >= (minW * 2 / 5) && rec.width <= (minW / 2)) && rec.height < (minh / 2)) {
//                Log.e("OCR", "地址 =" + rec);
                //住址
                mAddressRect = rec;
                Mat addressImage = new Mat();
                gary.submat(rec).copyTo(addressImage);
                Mat resizeMat = new Mat();
                Imgproc.resize(addressImage, resizeMat, new Size(addressImage.width() * 2, addressImage.height() * 2));
                Imgproc.rectangle(gary, new Point(rec.x, rec.y), new Point(rec.x + rec.width, rec.y + rec.height), new Scalar(0, 0, 255));
                Bitmap bitmap = Bitmap.createBitmap(resizeMat.width(), resizeMat.height(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(resizeMat, bitmap);
                Worker worker = new Worker(mFrontBarrier, bitmap, Worker.CARD_ADDRESS_FLAG, mCardBean);
                mThreadPool.execute(worker);
                addressImage.release();
            }

        }
        return newContours;
    }

    //原图过滤,过滤小噪点
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

    private void morphologyImageFilterRectFront(Mat threshold, Mat mat) {

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
                Imgproc.drawContours(threshold, contours, i, new Scalar(255, 255, 255), -1);
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }
            //面积太大的过滤
            if (area > matArea / 6) {
                Imgproc.drawContours(threshold, contours, i, new Scalar(255, 255, 255), -1);
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }
            //高度太高的过滤
            if (rec.height > (minH / 3)) {
                Imgproc.drawContours(threshold, contours, i, new Scalar(255, 255, 255), -1);
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }

            //高度太低的过滤
            if (rec.height <= (minH / 20)) {
                Imgproc.drawContours(threshold, contours, i, new Scalar(255, 255, 255), -1);
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
                continue;
            }

            //宽度太小的过滤
            if (rec.height <= (minW / 40)) {
                Imgproc.drawContours(threshold, contours, i, new Scalar(255, 255, 255), -1);
                Imgproc.drawContours(mat, contours, i, new Scalar(0, 0, 0), -1);
            }

        }
    }

    private void setBitmap(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(mat,bmp);
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(sdPath + File.separator + "points.jpg");
        ImageUtil.save(bmp,file);
    }
}

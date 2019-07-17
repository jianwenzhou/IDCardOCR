package org.opencv.admin.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.admin.ThreadPoolProxy;
import org.opencv.admin.ThreadPoolProxyFactory;
import org.opencv.admin.bean.IDCardBean;
import org.opencv.admin.camera.MaskView;

import java.util.concurrent.CyclicBarrier;

/**
 * 作者：Zhou on 2019/5/31 15:30
 * 简介：身份证识别处理类
 */
public class RecognitionCard {

    @SuppressLint("StaticFieldLeak")
    private volatile static RecognitionCard mSingleton = null;
    public static final int RECOGNIT_OK = 0;//成功
    public static final int FUZZY_ERROR = 100;//模糊
    public static final int REFLECTIVE_ERROR = 101;//反光
    public static final int REVERSE_ERROR = 102;//反转
    public static final int STABLE_ERROR = 103;//手机不稳定
    public static final int FAR_ERROR = 104;//手机离证件远
    public static final int INCOMPLETE_ERROR = 105;//残缺

    private static Activity activity;
    private IDCardBean mIDCardBean;
    private long startTime;
    private CyclicBarrier mFrontBarrier;
    private CyclicBarrier mBackBarrier;
    private RecognitionCompleteListener mCompleteListener;

    private Runnable frontRunnable = new Runnable() {
        @Override
        public void run() {
            long endTime = System.currentTimeMillis();

            Log.e("OCR", "识别时长：" + (endTime - startTime) / 1000f);
            if (mCompleteListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean frontSuccess = mIDCardBean.isFrontSuccess();
                        if (frontSuccess) {
                            mCompleteListener.onRecognitionCompleteListener(mIDCardBean);
                        } else {
                            mCompleteListener.onRecognitionErrorListener(FUZZY_ERROR);
                        }
                    }
                });
            }

        }
    };

    private Runnable backRunnable = new Runnable() {
        @Override
        public void run() {
            long endTime = System.currentTimeMillis();

            Log.e("OCR", "识别时长：" + (endTime - startTime) / 1000f);
            if (mCompleteListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean frontSuccess = mIDCardBean.isBackSuccess();
                        Log.e("OCR", "frontSuccess="+frontSuccess);

                        if (frontSuccess) {
                            mCompleteListener.onRecognitionCompleteListener(mIDCardBean);
                        } else {
                            mCompleteListener.onRecognitionErrorListener(FUZZY_ERROR);
                        }
                    }
                });
            }

        }
    };
    private static ThreadPoolProxy threadPool;


    private RecognitionCard() {
    }

    public static RecognitionCard getInstance(Activity activity) {
        RecognitionCard.activity = activity;
        threadPool = ThreadPoolProxyFactory.createThreadProxy(5);
        if (mSingleton == null) {
            synchronized (RecognitionCard.class) {
                if (mSingleton == null) {
                    mSingleton = new RecognitionCard();
                }
            }
        }
        return mSingleton;

    }


    /**
     * 回调识别结果
     */
    public interface RecognitionCompleteListener {
        void onRecognitionCompleteListener(IDCardBean cardBean);

        void onRecognitionErrorListener(int state);
    }


    public void startRecognition(int cardType, Bitmap bitmap, RecognitionCompleteListener completeListener) {
        mCompleteListener = completeListener;
        startTime = System.currentTimeMillis();
        mIDCardBean = new IDCardBean();
        int state = 0;
        if (cardType == MaskView.MASK_TYPE_ID_CARD_FRONT) {
            mFrontBarrier = new CyclicBarrier(5, frontRunnable);
            FrontCard frontCard = new FrontCard(mFrontBarrier, mIDCardBean, threadPool);
            state = frontCard.handlerImageFront(bitmap);
        } else if (cardType == MaskView.MASK_TYPE_ID_CARD_BACK) {
            mBackBarrier = new CyclicBarrier(2, backRunnable);
            BackCard backCard = new BackCard(mBackBarrier, mIDCardBean, threadPool);
            state = backCard.handlerImageBack(bitmap);
        }
        Log.e("OCR", "识别状态码="+state);
        if (state != 0) {
            if (mFrontBarrier != null) {
                mFrontBarrier.reset();
            }
            if (mBackBarrier != null) {
                mBackBarrier.reset();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            if (mCompleteListener != null) {
                mCompleteListener.onRecognitionErrorListener(state);
            }
        }
        bitmap.recycle();
    }


}




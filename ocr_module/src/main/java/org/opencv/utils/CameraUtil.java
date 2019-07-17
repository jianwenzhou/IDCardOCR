package org.opencv.utils;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * 作者：smile on 2018/1/11 13:23
 * 简介：摄像头的工具类
 */
public class CameraUtil {

    /**
     *
     * @param surfaceWidth 屏幕宽度
     * @param surfaceHeight 屏幕高度
     * @param preSizeList 相机支持的size
     * @return 合适size
     */
    public static Camera.Size getCloselyPreSize(int surfaceWidth, int surfaceHeight, List<Camera.Size> preSizeList) {

        int calcWidth = 0;
        int calcHeight = 0;

        Log.e("OpenCV",surfaceWidth+":"+surfaceHeight);

        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Camera.Size size : preSizeList) {
            if ((size.width == surfaceWidth) && (size.height == surfaceHeight)) {
                return size;
            }
        }

        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            int width = size.width;
            int height = size.height;
            if (width <= surfaceWidth && height <= surfaceHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = width;
                    calcHeight = height;
                    retSize = size;
                }
            }
        }

        return retSize;
    }

}

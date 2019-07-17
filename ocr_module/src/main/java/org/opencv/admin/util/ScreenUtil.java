package org.opencv.admin.util;

import android.content.Context;

/**
 * 作者：Zhou on 2018/5/10 10:52
 * 简介：获取屏幕宽高
 */
public class ScreenUtil {

    /**
     * 获取屏幕的宽度（单位：px）
     *
     * @return 屏幕宽
     */
    public static int getScreenWidth(Context context) {
        return context.getApplicationContext().getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕的高度（单位：px）
     *
     * @return 屏幕高
     */
    public static int getScreenHeight(Context context) {
        return context.getApplicationContext().getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 获取屏幕密度
     *
     * @return 屏幕密度
     */
    public static float getScreenDensity(Context context) {
        return context.getApplicationContext().getResources().getDisplayMetrics().density;
    }

    /**
     * 获取屏幕密度DPI
     *
     * @return 屏幕密度DPI
     */
    public static int getScreenDensityDpi(Context context) {
        return context.getApplicationContext().getResources().getDisplayMetrics().densityDpi;
    }



}

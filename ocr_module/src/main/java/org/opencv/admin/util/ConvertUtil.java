package org.opencv.admin.util;

import android.app.Application;
import android.content.Context;

/**
 * 作者：Zhou on 2018/1/25 15:33
 * 简介：转换工具类
 */
public class ConvertUtil {

    /**
     * dp转px
     *
     * @param dpValue dp值
     * @return px值
     */
    public static int dp2px(Context context, final float dpValue) {
        Context app = context;
        if (!(context instanceof Application)) {
            app = context.getApplicationContext();
        }
        final float scale = app.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * px转dp
     *
     * @param pxValue px值
     * @return dp值
     */
    public static int px2dp(Context context,final float pxValue) {
        Context app = context;
        if (!(context instanceof Application)) {
            app = context.getApplicationContext();
        }
        final float scale = app.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * sp转px
     *
     * @param spValue sp值
     * @return px值
     */
    public static int sp2px(Context context,final float spValue) {
        Context app = context;
        if (!(context instanceof Application)) {
            app = context.getApplicationContext();
        }
        final float fontScale = app.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * px转sp
     *
     * @param pxValue px值
     * @return sp值
     */
    public static int px2sp(Context context,final float pxValue) {
        Context app = context;
        if (!(context instanceof Application)) {
            app = context.getApplicationContext();
        }
        final float fontScale = app.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }



}

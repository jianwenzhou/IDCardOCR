package org.opencv.admin.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

/**
 * 作者：Zhou on 2018/4/21 11:41
 * 简介：权限检查
 */
public class CheckPermission {

    /**
     *
     * @param cxt
     * @param permissions
     * @return false == 缺少
     */
    public static boolean isLackPermission(Context cxt, @Nullable String[] permissions) {
        boolean isLack = true;
        for (String s : permissions
                ) {
            if (ActivityCompat.checkSelfPermission(cxt, s) != PackageManager.PERMISSION_GRANTED) {
                isLack = false;
                break;
            }
        }
        return isLack;
    }

}

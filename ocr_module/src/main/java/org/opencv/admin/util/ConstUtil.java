package org.opencv.admin.util;

import android.os.Environment;

import java.io.File;

/**
 * 作者：Zhou on 2018/7/25 16:58
 * 简介：
 */
public class ConstUtil {

    //扫图类型
    public static final int BANK_CARD_TYPE = 0;//银行卡
    public static final int ID_CARD_TYPE = 1;//身份证

    // 获取sdcard的根路径
    public static final String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final File mFile = new File(sdPath + File.separator + "points.jpg");

}

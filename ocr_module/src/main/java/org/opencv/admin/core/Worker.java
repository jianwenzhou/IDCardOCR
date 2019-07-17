package org.opencv.admin.core;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.admin.bean.IDCardBean;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * 子线程类，专注识别
 */

public class Worker implements Runnable {
    private static final String DATAPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    static final int CARD_NUMBER_FLAG = 0; //身份证号
    static final int CARD_ADDRESS_FLAG = 1;//住址
    static final int CARD_NAME_FLAG = 2;//姓名
    static final int CARD_SEX_FLAG = 3;//性别
    static final int CARD_ETHNIC_FLAG = 5;//民族
    static final int CARD_PERIOD_VALIDITY_FLAG = 6;//有效期
    static final int CARD_ISSUANCE_ADDRESS_FLAG = 7;//签发机关


    private CyclicBarrier barrier;
    private Bitmap bit;
    private int flag;
    private IDCardBean cardBean;

    Worker(CyclicBarrier barrier, Bitmap bit, int flag, IDCardBean cardBean) {
        this.barrier = barrier;
        this.bit = bit;
        this.flag = flag;
        this.cardBean = cardBean;
    }

    @Override
    public void run() {
        TessBaseAPI api = new TessBaseAPI();
        if (flag == CARD_NUMBER_FLAG) {
            api.init(DATAPATH, "rich_chi_number",TessBaseAPI.OEM_TESSERACT_ONLY);
        } else if (flag == CARD_SEX_FLAG) {
            api.init(DATAPATH, "rich_chi_sex",TessBaseAPI.OEM_TESSERACT_ONLY);
        } else if (flag == CARD_ETHNIC_FLAG) {
            api.init(DATAPATH, "rich_chi_nation",TessBaseAPI.OEM_TESSERACT_ONLY);
        } else if (flag == CARD_PERIOD_VALIDITY_FLAG) {
            api.init(DATAPATH, "rich_chi_validity",TessBaseAPI.OEM_TESSERACT_ONLY);
        } else {
            api.init(DATAPATH, "cs2015",TessBaseAPI.OEM_TESSERACT_ONLY);
            api.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "ˇ!@#$%^&*()__…′+=-[]}{;:——‘’“”'\"\\|`,./<>?【】；、。，丶《》~丿〕丨o〔〈"); // 识别黑名单
        }

        //tesseract API 设置 可以提高识别精度和时间
//        chop_enable	Ť	Chop启用。
//        use_new_state_cost	F	使用新的州成本启发法进行分段状态评估
//        segment_segcost_rating	F	将分段成本纳入单词评级？
//        enable_new_segsearch	0	启用新的细分搜索路径。它可以解决将一个字符分成两个字符的问题
//        language_model_ngram_on	0	打开/关闭字符ngram模型的使用。
//        textord_force_make_prop_words	F	强制对所有行进行比例分词。 // 导致 702 识别成为 m2
//        edges_max_children_per_outline	40	字符轮廓内的最大子项数。如果某些KANJI字符无法识别（拒绝），请增加此值。
        api.setVariable("chop_enable", TessBaseAPI.VAR_FALSE);
        api.setVariable("use_new_state_cost", TessBaseAPI.VAR_FALSE);
        api.setVariable("segment_segcost_rating", TessBaseAPI.VAR_FALSE);
        api.setVariable("enable_new_segsearch", "0");
        api.setVariable("language_model_ngram_on", "0");
        api.setVariable("textord_force_make_prop_words", TessBaseAPI.VAR_TRUE);
        api.setVariable("edges_max_children_per_outline", "40");
//        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_OSD_ONLY);

        api.setImage(bit);
        String text = api.getUTF8Text();
        final String s = text.replaceAll("\\s*", "");
        Log.e("OCR", "flag:" + flag);
        Log.e("OCR", "s:" + s);
        switch (flag) {
            case CARD_NAME_FLAG:
                cardBean.name = s;
                break;
            case CARD_ADDRESS_FLAG:
                cardBean.address = s;
                break;
            case CARD_NUMBER_FLAG:
                if (s.length() == 18) {
                    cardBean.idNumber = s;
                    String dateStr = s.substring(6, 14);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                    try {
                        Date date = format.parse(dateStr);
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        cardBean.birthData = formatter.format(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CARD_SEX_FLAG:
                if (s.length() == 1) {
                    cardBean.sex = s;
                }
                break;
            case CARD_ETHNIC_FLAG:
                cardBean.ethnic = s;
                break;
            case CARD_PERIOD_VALIDITY_FLAG:
                Log.e("OCR", "有效期Length:" + s.length());
                if (s.length() == 21) {
                    StringBuilder sb = new StringBuilder(s);
                    sb.replace(4, 5, ".");
                    sb.replace(7, 8, ".");
                    sb.replace(10, 11, "-");
                    sb.replace(15, 16, ".");
                    sb.replace(18, 19, ".");
                    cardBean.validity = sb.toString();
                    Log.e("OCR", "有效期:" + sb.toString());
                }
                if (s.length() == 17) {
                    cardBean.validity = s;
                }

                if (s.length() > 17 && s.length() < 21) {
                    cardBean.validity = s.replaceAll(".", "");
                }

                break;
            case CARD_ISSUANCE_ADDRESS_FLAG:
                cardBean.issuance = s;
                Log.e("OCR", "签发地址:" + s);
                break;
        }
        api.end();
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        bit.recycle();
    }


}
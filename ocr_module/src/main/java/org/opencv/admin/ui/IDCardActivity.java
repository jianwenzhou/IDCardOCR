package org.opencv.admin.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.admin.bean.IDCardBean;
import org.opencv.admin.camera.CameraView;
import org.opencv.admin.camera.MaskView;
import org.opencv.admin.core.RecognitionCard;
import org.opencv.admin.util.ImageUtil;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;

import cn.mobile.opencv_module.R;


public class IDCardActivity extends AppCompatActivity implements View.OnClickListener, RecognitionCard.RecognitionCompleteListener {


    static {
        System.loadLibrary("opencv_java3");
    }

    private int mState;
    private CameraView mCameraView;
    private ProgressDialog mPDialog;
    private static final String ID_CARD_TYPE = "IDCardType";//身份证类型
    public static final int ID_CARD_FRONT = 1;//身份证正面
    public static final int ID_CARD_BACK = 2;//身份证反面
    private static final int REQUEST_SYSTEM_PIC = 500;
    public static final int FRONT_RESULT_CODE = 200;//返回结果码
    public static final int BACK_RESULT_CODE = 300;//返回结果码
    public static final String RESULT_BEAN = "cardBean";//返回结果name

    public static final String[] MYPERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};
    private static final int mRequestCode = 101;

    /**
     * @param context
     *         上下文
     * @param requestCode
     *         请求码
     * @param IDCardType
     *         身份证正反面常量
     */
    public static void launch(Activity context, int requestCode, int IDCardType) {
        Intent intent = new Intent(context, IDCardActivity.class);
        intent.putExtra(ID_CARD_TYPE, IDCardType);
        context.startActivityForResult(intent, requestCode);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置全屏
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_id_card);

        ImageView photoBtn = findViewById(R.id.take_photo_button);

        mCameraView = findViewById(R.id.camera_view);

        photoBtn.setOnClickListener(this);


        mState = getIntent().getIntExtra(ID_CARD_TYPE, MaskView.MASK_TYPE_ID_CARD_FRONT);
        Log.e("OCR", "mState = = =" + mState);
        if (mState == MaskView.MASK_TYPE_ID_CARD_FRONT) {
            mCameraView.setMaskType(MaskView.MASK_TYPE_ID_CARD_FRONT, this);
        } else if (mState == MaskView.MASK_TYPE_ID_CARD_BACK) {
            mCameraView.setMaskType(MaskView.MASK_TYPE_ID_CARD_BACK, this);
        }

        changeAppBrightness(200);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("OCR", "onResume11111111");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {

            if (isLackPermission(this, MYPERMISSIONS)) {
                //权限都已授予时运行
                mCameraView.start();
            } else {
                //申请权限
                ActivityCompat.requestPermissions(this, MYPERMISSIONS, mRequestCode);
            }
        } else {
            //and6.0以下直接运行
            mCameraView.start();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == mRequestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                //权限授予成功
                mCameraView.start();
            } else {
                //权限授予失败
                Log.e("OCR", "onRequestPermissionsResult111111");
                Toast.makeText(this, "权限授予失败,无法正常运行！", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected boolean isLackPermission(Context cxt, @Nullable String[] permissions) {
        boolean isLack = true;
        assert permissions != null;
        for (String s : permissions) {
            if (ActivityCompat.checkSelfPermission(cxt, s) != PackageManager.PERMISSION_GRANTED) {
                isLack = false;
                break;
            }
        }
        return isLack;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.stop();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.take_photo_button) {
            photo();
        }
    }

    /**
     * 打开手机系统相册
     */
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SYSTEM_PIC);
    }


    private void photo() {
        // 拍照
        File file = new File(getCacheDir(), "idcard.jpg");
        mCameraView.takePicture(file, new CameraView.OnTakePictureCallback() {
            @Override
            public void onPictureTaken(final Bitmap bitmap) {
                startRecoging(bitmap);
            }
        });
    }

    private void startRecoging(Bitmap bitmap) {
        showProgress();
        if (bitmap == null) {
            hideProgress();
            return;
        }
        if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            hideProgress();
            return;
        }
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        double laplacian = ImageUtil.getLaplacian(mat);
        Log.e("OCR", "清晰度laplacian=" + laplacian);
        if (laplacian < 4.0) {
            hideProgress();
            showToast("图片模糊，请重新拍照！");
        } else {
            RecognitionCard instance = RecognitionCard.getInstance(IDCardActivity.this);
            instance.startRecognition(mState, bitmap,this);
        }
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(IDCardActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPDialog = new ProgressDialog(IDCardActivity.this);
                // 设置进度条风格，风格为圆形
                mPDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mPDialog.setCancelable(false);// 设置是否可以通过点击Back键取消
                mPDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
                // 设置ProgressDialog 标题
                mPDialog.setTitle("身份证识别");
                // 设置ProgressDialog 提示信息
                mPDialog.setMessage("正在识别中……");
                mPDialog.show();
            }
        });

    }

    private void hideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPDialog != null) {
                    mPDialog.dismiss();
                }
            }
        });

    }


    public void changeAppBrightness(int brightness) {
        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (brightness == -1) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            lp.screenBrightness = (brightness <= 0 ? 1 : brightness) / 255f;
        }
        window.setAttributes(lp);
    }

    private String getScanMessage(int status) {
        String message;
        switch (status) {
            case 0:
                message = "";
                break;
            case RecognitionCard.FUZZY_ERROR:
                message = "身份证模糊，请重新尝试";
                break;
            case RecognitionCard.REFLECTIVE_ERROR:
                message = "身份证反光，请重新尝试";
                break;
            case RecognitionCard.REVERSE_ERROR:
                message = "请将身份证前后反转再进行识别";
                break;
            case RecognitionCard.STABLE_ERROR:
                message = "请拿稳镜头和身份证";
                break;
            case RecognitionCard.FAR_ERROR:
                message = "请将镜头靠近身份证";
                break;
            case RecognitionCard.INCOMPLETE_ERROR:
                message = "请将身份证完整置于取景框内";
                break;
            default:
                message = "请将身份证置于取景框内";
        }

        return message;
    }


    /**
     * 识别成功
     * @param cardBean 识别信息
     */
    @Override
    public void onRecognitionCompleteListener(IDCardBean cardBean) {
        hideProgress();
        Intent intent = new Intent();
        intent.putExtra(RESULT_BEAN, cardBean);

        if (mState == MaskView.MASK_TYPE_ID_CARD_FRONT) {
            IDCardActivity.this.setResult(FRONT_RESULT_CODE, intent);
        } else if (mState == MaskView.MASK_TYPE_ID_CARD_BACK) {
            IDCardActivity.this.setResult(BACK_RESULT_CODE, intent);
        }
        IDCardActivity.this.finish();
    }

    /**
     * 识别失败
     * @param state 错误码
     */
    @Override
    public void onRecognitionErrorListener(int state) {
        hideProgress();
        showToast(getScanMessage(state));
    }
}

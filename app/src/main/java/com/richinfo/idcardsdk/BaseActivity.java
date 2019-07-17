package com.richinfo.idcardsdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

/**
 * 作者：Zhou on 2019/4/16 15:58
 * 简介：
 */
public class BaseActivity extends AppCompatActivity {

    private int PERMISSIONS_REQUEST_CODE;
    private CheckPermissionListener mPermissionListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    protected void checkPermissions(String[] permissions, int requestCode, CheckPermissionListener listener) {
        PERMISSIONS_REQUEST_CODE = requestCode;
        mPermissionListener = listener;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {

            if (isLackPermission(this, permissions)) {
                //权限都已授予时运行
                if (mPermissionListener != null) {
                    mPermissionListener.onSuccess();
                }
            } else {
                //申请权限
                ActivityCompat.requestPermissions(this, permissions, requestCode);
            }
        } else {
            //and6.0以下直接运行
            if (mPermissionListener != null) {
                mPermissionListener.onSuccess();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                //权限授予成功
                if (mPermissionListener != null) {
                    mPermissionListener.onSuccess();
                }
            } else {
                //权限授予失败
                if (mPermissionListener != null) {
                    mPermissionListener.onFailure();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    protected interface CheckPermissionListener {
        void onSuccess();

        void onFailure();
    }

    protected void setCheckPermissionListener(CheckPermissionListener listener) {

        mPermissionListener = listener;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
}

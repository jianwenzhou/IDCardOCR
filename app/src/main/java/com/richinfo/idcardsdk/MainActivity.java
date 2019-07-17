package com.richinfo.idcardsdk;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.admin.bean.IDCardBean;
import org.opencv.admin.ui.IDCardActivity;

public class MainActivity extends BaseActivity {

    public static final String[] MYPERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};

    public static final int requestCode = 100;
    private TextView mTv;
    private ImageView mIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTv = findViewById(R.id.textView);
        mIv = findViewById(R.id.imageView);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDCardActivity.launch(MainActivity.this, requestCode, IDCardActivity.ID_CARD_FRONT);
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDCardActivity.launch(MainActivity.this, requestCode, IDCardActivity.ID_CARD_BACK);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
//        checkPermissions(MYPERMISSIONS, 5050, new CheckPermissionListener() {
//            @Override
//            public void onSuccess() {
//
//            }
//
//            @Override
//            public void onFailure() {
//                Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == IDCardActivity.FRONT_RESULT_CODE) {
            IDCardBean cardBean = data.getParcelableExtra(IDCardActivity.RESULT_BEAN);
            mTv.setText("正面" + cardBean.toString());
            Log.e("OCR", " Main cardBean=" + cardBean);
        } else if (resultCode == IDCardActivity.BACK_RESULT_CODE) {
            IDCardBean cardBean = data.getParcelableExtra(IDCardActivity.RESULT_BEAN);
            mTv.setText("反面"+cardBean.toString());
            Log.e("OCR", " Main cardBean=" + cardBean);
        }

    }
}

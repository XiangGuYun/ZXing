package com.zxing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;
import com.uuzuche.lib_zxing.camera.BitmapLuminanceSource;
import com.uuzuche.lib_zxing.decoding.DecodeFormatManager;

import java.util.Hashtable;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE = 2;

    ImageView imageView;//显示生成的二维码图片
    EditText etInfo;//输入二维码信息
    private boolean needLogo;//是否需要显示LOGO
    private Bitmap mBitmap;//生成的二维码图片
    private Switch switchLogo;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ZXingLibrary.initDisplayOpinion(this);//实际中应写在Application中
        new RxPermissions(this)
                .request(Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) { // Always true pre-M
                        // I can control the camera now
                    } else {
                        // Oups permission denied
                    }
                });
        imageView = findViewById(R.id.ivPic);
        etInfo = findViewById(R.id.etForGenerate);
        switchLogo = findViewById(R.id.switchLogo);
        switchLogo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            needLogo = isChecked;
            Toast.makeText(this, String.valueOf(needLogo),Toast.LENGTH_LONG).show();
        });
    }

    /**
     * 打开默认的二维码扫描界面
     * @param view
     */
    public void open(View view) {
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    Toast.makeText(this, "解析结果:" + result, Toast.LENGTH_LONG).show();
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(MainActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }else if(requestCode == REQUEST_IMAGE){
            if(null != data){
                try {
                    String path = getPath(data);
                    Toast.makeText(this, "path is "+path, Toast.LENGTH_LONG).show();
                    CodeUtils.analyzeBitmap(path, new CodeUtils.AnalyzeCallback() {
                        @Override
                        public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
                            Toast.makeText(MainActivity.this,
                                    "解析结果:" + result, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onAnalyzeFailed() {
                            Toast.makeText(MainActivity.this,
                                    "解析二维码失败", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解析本地二维码图片
     * @param view
     */
    public void decodeLocalPic(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    public String getPath(Intent data){
        //这里开始的第二部分，获取图片的路径：
        String[] imgPath = {MediaStore.Images.Media.DATA};

        //好像是android多媒体数据库的封装接口，具体的看Android文档
        Cursor cursor = managedQuery(data.getData(), imgPath, null, null, null);

        //按我个人理解 这个是获得用户选择的图片的索引值
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        //将光标移至开头 ，这个很重要，不小心很容易引起越界
        cursor.moveToFirst();

        //最后根据索引值获取图片路径
        String path = cursor.getString(column_index);
        return path;
    }

    /**
     * 生成二维码图片
     * @param view
     */
    public void generateZXingPic(View view) {
        String textContent = etInfo.getText().toString();
        if (TextUtils.isEmpty(textContent)) {
            Toast.makeText(this, "您的输入为空!", Toast.LENGTH_SHORT).show();
            return;
        }
        etInfo.setText("");
        if(needLogo){
            mBitmap = CodeUtils.createImage(textContent, 400, 400, BitmapFactory
                    .decodeResource(getResources(), R.mipmap.ic_launcher));
        }else {
            mBitmap = CodeUtils.createImage(textContent, 400, 400, null);
        }
        imageView.setImageBitmap(mBitmap);
    }
}

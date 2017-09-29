package com.example.hoyoungpc.myapplication;

/**
 * Created by O_sung on 2016. 10. 9..
 */

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;


public class TempActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/font3.TTF");//폰트 설정하기
        setContentView(R.layout.activity_sub);

        Intent intent = new Intent(TempActivity.this, CameraActivity.class);
        startActivity(intent);
        finish();

    }
}

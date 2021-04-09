package com.heyanle.holo.crash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.heyanle.holo.R;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by HeYanLe on 2020/2/26 0026.
 * https://github.com/heyanLE
 */
public class CrashActivity extends AppCompatActivity {

    public static String INTENT_KEY = "heyanle_nul_intent_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nul);

        //MainModel.getInstance().isOpen(false).xP(0).yP(0.5f).apply();

        initView();

        Toolbar toolbar = findViewById(R.id.activity_nul_tolbar);
        setSupportActionBar(toolbar);

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
    }

    /**
     * 初始化View
     */
    private void initView(){

        TextView tvModel = findViewById(R.id.activity_nul_tv_model);
        TextView tvAndroidVersion = findViewById(R.id.activity_nul_tv_androidVersion);
        TextView tvELog = findViewById(R.id.activity_nul_tv_eLog);

        /*
        手机型号
         */
        tvModel.setText("手机型号："+android.os.Build.MODEL);

        /*
        安卓版本
         */
        tvAndroidVersion.setText("安卓版本："+android.os.Build.VERSION.RELEASE);

        /*
        报错信息
         */
        Intent intent = getIntent();
        if (intent == null) return;

        String eLog = intent.getStringExtra(INTENT_KEY);

        tvELog.setText(eLog);


    }
}

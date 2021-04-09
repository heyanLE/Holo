package com.heyanle.holo.crash;

import android.content.Context;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private Context mContext;

    public CrashHandler(Context context) {
        mContext = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {

        /*
        打印报错信息
         */
        StringWriter stringWriter = new StringWriter();
        try {
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            Throwable th = e.getCause();
            while (th != null) {
                th.printStackTrace(printWriter);
                th = th.getCause();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        /*
        Log输出（只有DeBug模式才会输出）
         */
        e.printStackTrace();

        /*
        启动崩溃界面Activity
         */
        Intent intent = new Intent();
        intent.setClass(mContext, CrashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(CrashActivity.INTENT_KEY, stringWriter.toString());
        mContext.startActivity(intent);

    }
}
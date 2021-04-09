package com.heyanle.holo.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry




abstract class BaseService : Service(), LifecycleOwner {

    private val mLifecycleRegistry :LifecycleRegistry by lazy{
        LifecycleRegistry(this)
    }

    override fun onCreate() {
        super.onCreate()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDestroy() {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return onRealBind(intent)
    }

    abstract fun onRealBind(intent: Intent?): IBinder?

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }


}
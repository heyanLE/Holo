package com.heyanle.holo.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun<T> LiveData<T>.observeWithNotify(owner: LifecycleOwner, observer: Observer<T>){
    observe(owner, observer)
    if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){
        value?.let {
            observer.onChanged(value)
        }
    }
}

fun<T> LiveData<T>.observeForeverWithNotify(observer: Observer<T>){
    observeForever(observer)
    observer.onChanged(value)
}
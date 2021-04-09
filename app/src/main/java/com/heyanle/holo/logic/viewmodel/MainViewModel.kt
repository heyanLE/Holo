package com.heyanle.holo.logic.viewmodel

import android.os.Messenger
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.heyanle.holo.entity.Prescription
import com.heyanle.holo.entity.StatusInfo

/**
 * Created by HeYanLe on 2021/2/6 0006 22:12.
 * https://github.com/heyanLE
 */

class MainViewModel : ViewModel(){

    val toolbarTitle = MutableLiveData<String>()

    var messenger: Messenger? = null

    var nowSelect = MutableLiveData<Int>()

    init {
        nowSelect.value = 0
    }

}
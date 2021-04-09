package com.heyanle.holo.entity

import com.heyanle.holo.ui.view.LineChartView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by HeYanLe on 2021/2/11 0011 12:53.
 * https://github.com/heyanLE
 */

data class StatusInfo(
        var title: String = "测试4",
        var type: String = "2002",
        var currentUpModelTem: Float = 0F,
        var targetUpModelTem: Float = 0F,
        var currentDownModelTem: Float = 0F,
        var targetDownModelTem: Float = 0F,
        var currentPressure: Float = 0F,
        var targetPressure: Float = 0F,
        var currentTaskTime: Long = 0L,
        var targetSoakingTime: Long = 0L,
        var targetCoolingTem: Float = 0F,
        var time: Long = 0L,
){

    fun getTimeString(): String{
        val simpleDateFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return simpleDateFormat.format(Date(time))
    }
}
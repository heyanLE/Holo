package com.heyanle.holo.entity

import com.heyanle.holo.ui.view.NewLineChartView
import java.util.ArrayList

data class ShowStatus (
        var deviceType:String = "1001",
        var list: ArrayList<NewLineChartView.TemData> = arrayListOf(),
        var workTime: Long = 0L,
        var pressList: ArrayList<Float> = arrayListOf(),
        var targetPre:Float = 0F,
        var targetUp:Float = 0F,
        var targetDown: Float = 0F,
        var soakingTime: Int = 0,
        var coolingTem: Float = 0F,
        var startTime:Long = System.currentTimeMillis()
){
    fun currentUpTem():Float{
        if(list.isEmpty()){
            return 0F
        }
        return list.last().upModelTem
    }

    fun currentDownTem():Float{
        if(list.isEmpty()){
            return 0F
        }
        return list.last().downModelTem
    }

    fun currentPressure():Float{
        if(pressList.isEmpty()){
            return 0F
        }
        return pressList.last()
    }


}
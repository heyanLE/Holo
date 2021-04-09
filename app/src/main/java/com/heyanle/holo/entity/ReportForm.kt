package com.heyanle.holo.entity

import com.heyanle.holo.ui.view.LineChartView
import com.heyanle.holo.ui.view.NewLineChartView
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLEngineResult
import kotlin.math.max
import kotlin.math.min

data class ReportForm(
        var deviceType:String = "1001",
        var statusInfo: StatusInfo = StatusInfo(),
        var list: ArrayList<NewLineChartView.TemData> = arrayListOf(),
        var prescription: Prescription = Prescription(),
        var startTime:Long = 0L,
        var endTime: Long = -1L,
        var maxPre: Float = -1F,
        var minPre: Float = Float.MAX_VALUE,
        var workTime: Long = 0
){

    fun getEndTimeString(): String{
        val date = endTime.also { Date().time = it }
        val simpleDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return simpleDateFormatter.format(date)

    }
    fun getSoakingTime():String{
        val soakingTime = prescription.soakingTime
        val h = (soakingTime/(60*60)).toInt()
        val m = ((soakingTime%(60*60))/60).toInt()
        val s = (soakingTime%60).toInt()
        return "${if(h<10) {"0${h}"}else {"$h"}}:${if(m<10) {"0${m}"}else {"$m"}}:${if(s<10) {"0${s}"}else {"$s"}}"
    }

    fun getAllTime():String{
        val time = workTime
        val hour = if (time/(1*60*60) >= 10) "${(time/(1*60*1000)).toInt()}" else "0${(time/(1*60*1000)).toInt()}"
        val mi = if (((time%(1*60*60))/(1*60*1000)) >= 10) "${((time%(1*60*60*1000))/(1*60*1000)).toInt()}" else "0${((time%(1*60*60*1000))/(1*60*1000)).toInt()}"
        val s = if((time%(1*60)) >= 10)"${(time%(1*60*1000)/1000).toInt()}" else "0${(time%(1*60*1000)/1000).toInt()}"
        return "${hour}:${mi}:${s}"
    }

    fun getMaxUpTem(): Float{
        var r = 0F
        for(t in list){
            r = max(t.upModelTem, r)
        }
        return r
    }

    fun getMinUpTem(): Float{
        var r = Float.MAX_VALUE
        for(t in list){
            r = min(t.upModelTem, r)
        }
        return r
    }

    fun getMaxDownTem(): Float{
        var r = 0F
        for(t in list){
            r = max(t.downModelTem, r)
        }
        return r
    }

    fun getMinDownTem(): Float{
        var r = Float.MAX_VALUE
        for(t in list){
            r = min(t.downModelTem, r)
        }
        return r
    }

    fun newTem(upModel:Float, downModel: Float, time: Long){
        val tem = NewLineChartView.TemData()
        tem.upModelTem = upModel
        tem.downModelTem = downModel
        tem.time = time
        list.add(tem)
    }

    fun newPre(pressure: Float){
        maxPre = max(maxPre, pressure)
        minPre = min(minPre, pressure)
    }



}
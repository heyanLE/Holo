package com.heyanle.holo.entity

/**
 * Created by HeYanLe on 2021/2/7 0007 21:22.
 * https://github.com/heyanLE
 */
data class Prescription(
        var unique : String = "",
        var trackType: String = "",
        var pressure: Float = 1.5f,
        var upModelTemperature: Float = 185F,
        var downModelTemperature: Float = 185F,
        var coolingTemperature: Float = 65F,

        var soakingTime: Int = 180, // 单位 s
        var isPreheatingPreloading: Boolean =false,
        var preloading: Float = 1.2F, // 预压
        var preheatingTemperature: Float = 62F,
        var preheatingSoakingTime: Int = 62,
) {



    fun getSoakingTimeString(): String{
        val h = (soakingTime/(60*60)).toInt()
        val m = ((soakingTime%(60*60))/60).toInt()
        val s = (soakingTime%60).toInt()
        return "${h}h ${m}m ${s}s"
    }


}
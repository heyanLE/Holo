package com.heyanle.holo.entity

/**
 * Created by HeYanLe on 2021/5/6 11:00.
 * https://github.com/heyanLE
 */
data class Notes (
        var time: Long = -1L,
        var upTem: Float = 0F,
        var downTem: Float = 0F,
        var pressure: Float = 0F,
        var eventType: Int = 0
        )
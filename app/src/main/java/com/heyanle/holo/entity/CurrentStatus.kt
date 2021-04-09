package com.heyanle.holo.entity

data class CurrentStatus(
        var time: String,
        var upModelTem: Float = 0F,
        var downModelTem: Float = 0F,
        var pressure: Float = 0F
) {

}
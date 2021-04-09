package com.heyanle.holo.entity

/**
 * Created by HeYanLe on 2021/2/14 0014 20:00.
 * https://github.com/heyanLE
 */

data class FactorySettingInfo(
        var mainBoardTime: String = "2021-02-14 20:01",
        var nowTime: String = "2021-02-14 20:01",
        var boardTem: Float = 40.8F,
        var disk: Float = 0.48F,
        var currentIp: String = "127.0.0.1",
        var ip : String = "127.0.0.1",
        var deviceId: String = "2011",
        var deviceType: String = "风冷机3代",
        var customerId: String = "2100",
        var customerName: String = "瑞安市红龙",
){

    fun getDisk(): String{
        return "${(disk*10000).toInt()/100F}%"
    }
}
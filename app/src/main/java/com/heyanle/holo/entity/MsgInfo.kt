package com.heyanle.holo.entity

import com.google.gson.annotations.SerializedName

class MsgInfo {

    @SerializedName("FinterID")
    var finterId = "0"

    var intro = "温州红龙工业设备制造有限公司"
    var adress = "浙江省瑞安市开发区大道3588号E幢兴东小微创业园3号楼一楼"
    var tel = "（86）0577-65682111"
    var phone = "（86）0577-65916651"
    var web1 = "www.18816.cn"
    var web2 = "www.holobelt.com"
    var facebook = "holopress"
    var weatsapp = "Levin chen"
    var youtube = "holo press"
    @SerializedName("WebSite")
    var wechat = "holo"
    @SerializedName("AddTime")
    var addTime:String? = null
    @SerializedName("PicID")
    var picId:String? = null

}
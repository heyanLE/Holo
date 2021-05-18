package com.heyanle.holo.entity

import com.heyanle.holo.language.LanguageManager

/**
 * Created by HeYanLe on 2021/2/6 0006 22:02.
 * https://github.com/heyanLE
 */

class DeviceDescribe {

    var brand:String = "暂无"
    var alias:String = "暂无"
    var productUse: String = "暂无"
    var type:String = "暂无"
    var useI:String = "暂无"
    var useII: String = "暂无"
    var img:String = ""

    init {
        if(LanguageManager.nowIndex == 1){
            brand = "Not yet"
            alias = "Not yet"
            productUse = "Not yet"
            type = "Not yet"
            useI = "Not yet"
            useII = "Not yet"
        }
    }

}
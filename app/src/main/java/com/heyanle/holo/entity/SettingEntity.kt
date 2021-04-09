package com.heyanle.holo.entity


open class SettingEntity<T> (
        open var value:T, open var title: String, open var address: Int
        ,open var layoutId: Int){

    var onCovert: (Prescription) -> T = {
        value
    }

    fun covert(prescription: Prescription){
        value = onCovert(prescription)
    }


}

class FloatSetting(override var value: Float, override var title: String,override var address: Int, override var layoutId: Int)
    :SettingEntity<Float>(value, title, address, layoutId){

    var max:Float = 0F
    var min:Float = 0F
    }

class IntSetting(override var value: Int, override var title: String,override var address: Int, override var layoutId: Int)
    :SettingEntity<Int>(value, title, address, layoutId){

    var max:Int = 0
    var min:Int = 0
    }

class TimeSetting(override var value: Int, override var title: String,override var address: Int, override var layoutId: Int)
    :SettingEntity<Int>(value, title, address, layoutId){

    var max:Int = 0
    var min:Int = 0

    fun getText(): String{
        return "${value/60}m ${value%60}s"
    }
}

class BooleanSetting(override var value: Boolean, override var title: String,override var address: Int, override var layoutId: Int)
    :SettingEntity<Boolean>(value, title, address, layoutId)
package com.heyanle.holo.logic.model

import android.content.Context
import android.content.SharedPreferences
import com.heyanle.holo.HoloApplication
import kotlin.reflect.KProperty

/**
 * Created by HeYanLe on 2021/2/6 0006 15:29.
 * https://github.com/heyanLE
 */
object SPModel {

    const val SP_NAME = "HOLO"

    var username by SP.string("HOLO_USERNAME", "")
    var password by SP.string("HOLO_PASSWORD", "")

    var addressMap by SP.string("HOLO_ADDRESS_LIST", "{}")

    var token by SP.string("TOKEN","33fd5eb8-f424-4ab2-a315-4a0e70896e70")

    var china by SP.int("CHINA",0)

}

class SP<T>(private val key: String, private val defValue: Any) {


    companion object{
        fun string(key: String, defValue: String): SP<String> = SP(key, defValue)
        fun int(key: String, defValue: Int): SP<Int> = SP(key, defValue)
        fun long(key: String, defValue: Long): SP<Long> = SP(key, defValue)
        fun float(key: String, defValue: Float): SP<Float> = SP(key, defValue)
        fun boolean(key: String, defValue: Boolean): SP<Boolean> = SP(key, defValue)
    }


    private val sharedPreferences: SharedPreferences by lazy {
        HoloApplication.INSTANCE.getSharedPreferences(
            SPModel.SP_NAME,
            Context.MODE_PRIVATE
        )
    }


    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        when(defValue){
            is String -> sharedPreferences.getString(key, defValue)!!
            is Int -> sharedPreferences.getInt(key, defValue)
            is Long -> sharedPreferences.getLong(key, defValue)
            is Float -> sharedPreferences.getFloat(key, defValue)
            is Boolean -> sharedPreferences.getBoolean(key, defValue)
            else -> sharedPreferences.getString(key, defValue.toString())!!
        } as T

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
            = sharedPreferences.edit().apply {
        when(value){
            is String -> putString(key, value)!!
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Boolean -> putBoolean(key, value)
            else -> putString(key, value.toString())!!
        }
    }.apply()


}
package com.heyanle.holo.language

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.google.gson.Gson
import com.heyanle.holo.logic.model.SP
import java.util.*


/**
 * Created by HeYanLe on 2021/2/7 0007 20:23.
 * https://github.com/heyanLE
 */
object LanguageManager {

    val languageList = arrayListOf<String>("简体中文", "English")
    var nowIndex by SP.int("Language", 0)

    fun language(context: Context): Context{
        val locale = getLocale()
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            configuration.setLocale(locale)
        }
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        return context.createConfigurationContext(configuration)
    }


    fun getLocale(): Locale =
            when(nowIndex){
                0 -> Locale.CHINESE
                1 -> Locale.US
                else -> getSystemLocale()
            }

    fun getSystemLocale()        :Locale=
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                LocaleList.getDefault().get(0)
            else Locale.getDefault()


}
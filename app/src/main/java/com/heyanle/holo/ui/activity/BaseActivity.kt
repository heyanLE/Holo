package com.heyanle.holo.ui.activity

import android.R
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.heyanle.holo.language.LanguageManager
import java.util.*


/**
 * Created by HeYanLe on 2021/2/6 0006 15:27.
 * https://github.com/heyanLE
 */

open class BaseActivity : AppCompatActivity(){
    // BaseActivity继承AppCompatActivity
    override fun attachBaseContext(newBase: Context?) {
        val context = LanguageManager.language(newBase!!)
        val configuration = context.resources.configuration
        // 此处的ContextThemeWrapper是androidx.appcompat.view包下的
        // 你也可以使用android.view.ContextThemeWrapper，但是使用该对象最低只兼容到API 17
        // 所以使用 androidx.appcompat.view.ContextThemeWrapper省心
        val wrappedContext: ContextThemeWrapper = object : ContextThemeWrapper(context,
                R.style.Theme) {
            override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
                overrideConfiguration?.setTo(configuration)
                super.applyOverrideConfiguration(overrideConfiguration)
            }
        }
        super.attachBaseContext(wrappedContext)
    }


    @TargetApi(Build.VERSION_CODES.N)
    private fun createConfigurationResources(context: Context, language: Int): Context? {
        val resources: Resources = context.resources
        val configuration: Configuration = resources.getConfiguration()
        val dm: DisplayMetrics = resources.getDisplayMetrics()
        val locale: Locale
        if (language < 0) {
            // 如果没有指定语言使用系统首选语言
            locale = getSystemPreferredLanguage()
        } else {
            // 指定了语言使用指定语言，没有则使用首选语言
            locale = LanguageManager.getLocale()
        }
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, dm)
        return context
    }

    /**
     * 获取系统首选语言
     *
     * @return Locale
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    open fun getSystemPreferredLanguage(): Locale {
        val locale: Locale
        locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault()[0]
        } else {
            Locale.getDefault()
        }
        return locale
    }
}
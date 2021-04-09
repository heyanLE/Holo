package com.heyanle.holo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import com.heyanle.holo.logic.model.SP
import java.util.*


class LocatedUtils {

    var locatedString by SP.string("", "中国")

    private fun getLocated(context: Context){
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            located(context, locationManager, LocationManager.GPS_PROVIDER)
        }else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            located(context, locationManager, LocationManager.NETWORK_PROVIDER)
        }

    }

    private fun located(context: Context, locationManager: LocationManager, locationProvider: String){

    }


    /**
     * 方法二
     */
    /**
     * 方法二
     */
    /** 查询手机的 MCC+MNC  */
    private fun getSimOperator(c: Context): String? {
        val tm = c.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            return tm.simOperator
        } catch (e: Exception) {
        }
        return null
    }


    /** 因为发现像华为Y300，联想双卡的手机，会返回 "null" "null,null" 的字符串  */
    private fun isOperatorEmpty(operator: String?): Boolean {
        if (operator == null) {
            return true
        }
        return operator == "" || operator.toLowerCase(Locale.US).contains("null")
    }


    /** 判断是否是国内的 SIM 卡，优先判断注册时的mcc  */
    fun isChinaSimCard(c: Context): Boolean {
        val mcc = getSimOperator(c)
        return if (isOperatorEmpty(mcc)) {
            false
        } else {
            mcc!!.startsWith("460")
        }
    }

}
package com.heyanle.holo

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDex
import com.heyanle.holo.crash.CrashHandler
import com.heyanle.holo.entity.*
import com.heyanle.holo.language.LanguageManager
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.view.NewLineChartView
import com.heyanle.modbus.ByteUtil
import com.heyanle.modbus.ModbusRtuMaster
import com.swallowsonny.convertextlibrary.readUInt16BE
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by HeYanLe on 2021/2/6 0006 15:20.
 * https://github.com/heyanLE
 */

class HoloApplication : Application(){



    val handler = Handler(Looper.getMainLooper())

    //val reportFromList = MutableLiveData<ArrayList<StatusInfo>>()

    val deviceDescribe = MutableLiveData<DeviceDescribe>()

    val deviceId = MutableLiveData<String>()

    val deviceN = MutableLiveData<String>()

    val token = MutableLiveData<String>()

    val userTitle = MutableLiveData<String>()

    // 当前可用配方字段
    val prescriptionSetting = MutableLiveData<ArrayList<SettingEntity<*>>>()

    val modbusRtuMaster = ModbusRtuMaster()

    val isClick = MutableLiveData<Boolean>()

    // 当前机型
    val currentType = MutableLiveData<String>()

    // 当前设备是否启动
    val nowDeviceRun = MutableLiveData<Boolean>()

    // 当前配方
    val currentPrescription = MutableLiveData<Prescription>()

    // 配方收藏列表
    val prescriptionList = MutableLiveData<ArrayList<Prescription>>()

    // 出厂设置
    val factorySetting = MutableLiveData<FactorySettingInfo>()

    var reportFromList = MutableLiveData<ArrayList<ReportForm>>()

    val status = MutableLiveData<StatusInfo>()


    var showReportForm = ReportForm()
    var runReportForm = MutableLiveData<ReportForm>()

    var connect = MutableLiveData<Boolean>()

    var dataBuff = hashMapOf<Long, CurrentStatus>()

    var isRunClick = MutableLiveData<Boolean>()
    var isStopClick = MutableLiveData<Boolean>()

    var nowShowStatus = MutableLiveData<ShowStatus>()

    var realShowStatus = MutableLiveData<ShowStatus>()


    init {

    }

    companion object{
        lateinit var INSTANCE: HoloApplication
        const val DEBUG = false
    }

    override fun attachBaseContext(base: Context?) {
        base?.apply {
            val i = base.getSharedPreferences(SPModel.SP_NAME,
                    Context.MODE_PRIVATE)!!.getInt("Language",0)
            val locale = when(i){
                0 -> Locale.CHINESE
                1 -> Locale.ENGLISH
                else -> LanguageManager.getSystemLocale()
            }
            val configuration = Configuration(resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocales(LocaleList(locale))
            } else {
                configuration.setLocale(locale)
            }
            resources.updateConfiguration(configuration, resources.displayMetrics)
            super.attachBaseContext(createConfigurationContext(configuration))
        }

    }

    override fun onCreate() {
        super.onCreate()


        MultiDex.install(this);
        INSTANCE = this

        val config: Configuration = resources.configuration
        val dm: DisplayMetrics = resources.displayMetrics
        config.setLocale(LanguageManager.getLocale())
        resources.updateConfiguration(config, dm)

        LanguageManager.language(this)

        currentType.value = "2040"
        currentPrescription.value = Prescription().apply {
            trackType = SPModel.trackType
        }
        prescriptionList.value = arrayListOf()
        reportFromList.value = arrayListOf()
        factorySetting.value = FactorySettingInfo()
        deviceDescribe.value = DeviceDescribe()

        token.value = SPModel.token
        deviceId.value = SPModel.deviceIndex

        isStopClick.value = false

        //tem.value = arrayListOf()
        prescriptionSetting.value = arrayListOf()
        status.value = StatusInfo()

        nowDeviceRun.value = false
        runReportForm.value = ReportForm()
        deviceId.value = ""
        isClick.value = false
        deviceN.value = SPModel.deviceId
        connect.value = false
        isRunClick.value = false
        nowShowStatus.value = ShowStatus()
        realShowStatus.value = ShowStatus()

        currentPrescription.observeForever{
            SPModel.trackType = it.trackType
        }
        deviceN.observeForever {
            SPModel.deviceId = it
        }
        deviceId.observeForever {
            SPModel.deviceIndex = it
        }

        //ConnectionModel.load()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        //Log.i("HoloApplication", modbusRtuMaster.bytes2Hex(modbusRtuMaster.readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 4)))
//        val z = (1 ushr 16) and 0xFFFF
//        val o = 1 and 0xFFFF
//
//
//        Log.i("HoloApplication", ByteUtil.toHexString(HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
//                BluetoothService.SLAVE_ADDRESS, 18,2)))
//
//        Log.i("HoloApplication", ByteUtil.toHexString(HoloApplication.INSTANCE.modbusRtuMaster
//                .writeHoldingRegisters(BluetoothService.SLAVE_ADDRESS, 4000,2, intArrayOf(z.toInt(), o.toInt()))))
//        Log.i("HoloApplication", ByteUtil.toHexString(HoloApplication.INSTANCE.modbusRtuMaster
//                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 4002)))
//        Log.i("HoloApplication", ByteUtil.toHexString(HoloApplication.INSTANCE.modbusRtuMaster
//                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 4003)))
//        Log.i("HoloApplication", ByteUtil.toHexString(HoloApplication.INSTANCE.modbusRtuMaster
//                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 4004)))
//        Log.i("HoloApplication", ByteUtil.toHexString(HoloApplication.INSTANCE.modbusRtuMaster
//                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 4005)))
//        Log.i("HoloApplication", ByteUtil.toHexString(HoloApplication.INSTANCE.modbusRtuMaster
//                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 4010)))


        BluetoothQueueNew.init(this)

        if(DEBUG){
            //BluetoothQueueThread().start()
        }


    }


    inner class BluetoothQueueThread(): Thread(){
        var stopFlags = false
        lateinit var handler: Handler
        var last = false

        override fun run() {
            super.run()
            Looper.prepare()

            handler = Handler(Looper.myLooper()!!)
            handler.post (object : Runnable{
                override fun run() {
                    if(!stopFlags){
                        runCatching {
                            BluetoothQueueNew.run()
                        }
                        handler.post(this)
                    }else{
                        Looper.myLooper()?.quitSafely()
                    }
                }
            })

            Looper.loop()

        }
    }

}
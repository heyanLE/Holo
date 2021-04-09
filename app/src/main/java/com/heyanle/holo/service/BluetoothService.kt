package com.heyanle.holo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.entity.Prescription
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.entity.ShowStatus
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.ui.main.MainActivity
import com.heyanle.modbus.ByteUtil
import com.inuker.bluetooth.library.Code.REQUEST_SUCCESS
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener
import com.inuker.bluetooth.library.connect.response.BleConnectResponse
import com.inuker.bluetooth.library.connect.response.BleWriteResponse
import com.inuker.bluetooth.library.model.BleGattProfile
import com.inuker.bluetooth.library.receiver.listener.BluetoothStateChangeListener
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.swallowsonny.convertextlibrary.readUInt16BE
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.atomic.AtomicLong


class BluetoothService : Service(){


    private val bluetoothAdapter: BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private val bluetoothLeScanner : BluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothGattCharacteristic: BluetoothGattCharacteristic? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    private var bluetoothQueueThread: BluetoothQueueThread? =null
    set(value) {
        field?.stopFlags = true
        field = value
    }
    private var bluetoothThread: BluetoothCheckThread? =null
        set(value) {
            field?.stopFlags = true
            field = value
        }



    var scanRelayMessenger: Messenger? =null
    var connectingRelayMessenger: Messenger? = null

    var bluetoothStatusListener: Messenger? =null

    private val receiver = object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if(it.action == BluetoothAdapter.ACTION_STATE_CHANGED){
                    val blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                    if(blueState == BluetoothAdapter.STATE_TURNING_OFF){
                        scanRelayMessenger?.send(Message().apply {
                            what = MSG_WHAT_SCAN
                            arg1 = 0
                        })
                    }
                }
            }
        }
    }

    companion object{

        @JvmStatic
        var s = ""

        const val MSG_WHAT_SCAN_NEW_DEVICE = 4

        const val MSG_WHAT_CONNECTING_MSG = 3

        const val MSG_WHAT_SCAN = 0
        const val MSG_NEW_COMMAND = 1
        const val MSG_NEW_COMMAND_P = 12
        const val MSG_WHAT_CONNECT = 2
        const val MSG_WHAT_START_QUEUE = 7
        const val MSG_REAL_NEW_COMMAND = 10
        const val MSG_TEST = 11

        const val MSG_WHAT_BLUETOOTH_LISTENER_BIND = 5
        const val MSG_WHAT_BLUETOOTH_LISTENER_UNBIND = 6


        const val SLAVE_ADDRESS = 254
        const val DELAY = 10*60*1000L
    }





    private val handler = object:Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                MSG_TEST -> {
                    bluetoothAdapter.getRemoteDevice("34:14:B5:BC:C1:55")
                            .connectGatt(this@BluetoothService, false, object: BluetoothGattCallback(){
                                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                                    super.onConnectionStateChange(gatt, status, newState)
                                    if(newState == BluetoothProfile.STATE_CONNECTED){
                                        gatt!!.discoverServices()
                                    }
                                }
                                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                                    super.onServicesDiscovered(gatt, status)
                                    gatt?.let {
                                        val c = it.getService(UUID.fromString(BluetoothQueueNew.SERVICE_UUID))
                                                .getCharacteristic(UUID.fromString(BluetoothQueueNew.CHARACTERISTIC_UUID))
                                        it.setCharacteristicNotification(c, true)
                                        val descriptor = c.getDescriptor(UUID.fromString(BluetoothQueueNew.DESCRIPTOR_UUID));
                                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                                        it.readCharacteristic(c)
                                        it.writeDescriptor(descriptor)
                                        bluetoothGatt = it
                                        bluetoothGattCharacteristic = c
                                        HoloApplication.INSTANCE.handler.postDelayed({
                                            bluetoothGattCharacteristic!!.value = HoloApplication.INSTANCE.modbusRtuMaster
                                                    .readHoldingRegisters(BluetoothService.SLAVE_ADDRESS, 2013, 8)
                                            bluetoothGatt?.writeCharacteristic(bluetoothGattCharacteristic)
                                        }, 5000)
                                    }
                                }

                                override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                                    super.onCharacteristicChanged(gatt, characteristic)
                                    Log.i("BluetoothSErvdd", characteristic!!.uuid.toString())
                                }
                            })

                }
                MSG_WHAT_START_QUEUE -> {
                    bluetoothQueueThread = BluetoothQueueThread()
                    bluetoothQueueThread?.stopFlags = false
                    bluetoothQueueThread?.start()
                    msg.replyTo?.send(Message().apply {
                        what = MSG_WHAT_START_QUEUE
                    })
                }
                MSG_WHAT_BLUETOOTH_LISTENER_BIND -> {
                    runCatching {
                        bluetoothStatusListener = msg.replyTo
                    }
                }
                MSG_WHAT_BLUETOOTH_LISTENER_UNBIND -> {
                    runCatching {
                        bluetoothStatusListener = null
                    }
                }
                MSG_WHAT_SCAN -> {

                    if(BluetoothQueueNew.bluetoothClient.isBluetoothOpened){
                        if(BluetoothQueueNew.bluetoothClient.isBleSupported){
                            runCatching {
                                BluetoothQueueNew.bluetoothClient.disconnect(BluetoothQueueNew.MAC)
                            }
                            msg.replyTo.send(Message().apply {
                                what = MSG_WHAT_SCAN
                                arg1 = 1
                            })
                            scanRelayMessenger = msg.replyTo
                            BluetoothQueueNew.search(object: SearchResponse{
                                override fun onSearchStarted() {

                                }

                                override fun onDeviceFounded(device: SearchResult?) {
                                    device?.device?.let {
                                        scanRelayMessenger?.send(Message().apply {
                                            what = MSG_WHAT_SCAN_NEW_DEVICE
                                            obj = it
                                        })
                                    }
                                }

                                override fun onSearchStopped() {
                                }

                                override fun onSearchCanceled() {
                                    scanRelayMessenger?.send(Message().apply {
                                        what = MSG_WHAT_SCAN
                                        arg1 = 0
                                    })
                                }
                            })
                        }else{
                            msg.replyTo.send(Message().apply {
                                what = MSG_WHAT_SCAN
                                arg1 = 0
                            })
                        }

                    }else{
                        msg.replyTo.send(Message().apply {
                            what = MSG_WHAT_SCAN
                            arg1 = 0
                        })
                    }
                }
                MSG_WHAT_CONNECT -> {
                    if(BluetoothQueueNew.bluetoothClient.isBluetoothOpened&& BluetoothQueueNew.bluetoothClient.isBleSupported){
                        runCatching {
                            BluetoothQueueNew.bluetoothClient.disconnect(BluetoothQueueNew.MAC)
                        }

                        val address = msg.obj as String
                        Log.i("BluetoothService","$address")
                        connectingRelayMessenger = msg.replyTo
                        connectingRelayMessenger?.send(Message().apply {
                            what = MSG_WHAT_CONNECTING_MSG
                            arg1 = 0
                        })
                        BluetoothQueueNew.connect(address, object: BleConnectResponse{
                            override fun onResponse(code: Int, data: BleGattProfile?) {
                                if(code == REQUEST_SUCCESS){
                                    BluetoothQueueNew.MAC = address
                                    connectingRelayMessenger?.send(Message().apply {
                                        what = MSG_WHAT_CONNECTING_MSG
                                        arg1 = 1
                                    })
                                    if(data == null){
                                        connectingRelayMessenger?.send(Message().apply {
                                            what = MSG_WHAT_CONNECTING_MSG
                                            arg1 = 4
                                        })
                                        runCatching {
                                            BluetoothQueueNew.bluetoothClient.disconnect(address)
                                        }
                                        return
                                    }
                                    val d = data.getService(UUID.fromString(BluetoothQueueNew.SERVICE_UUID))
                                    if(d == null){
                                        connectingRelayMessenger?.send(Message().apply {
                                            what = MSG_WHAT_CONNECTING_MSG
                                            arg1 = 4
                                        })
                                        runCatching {
                                            BluetoothQueueNew.bluetoothClient.disconnect(address)
                                        }
                                        return
                                    }
                                    for(c in d.characters){
                                        if(c.uuid.equals(UUID.fromString(BluetoothQueueNew.CHARACTERISTIC_UUID))){
                                            BluetoothQueueNew.bluetoothClient.read(
                                                BluetoothQueueNew.MAC, UUID.fromString(BluetoothQueueNew.SERVICE_UUID),
                                                UUID.fromString(BluetoothQueueNew.CHARACTERISTIC_UUID)
                                            ){cod,_->
                                                if(cod != REQUEST_SUCCESS){
                                                    connectingRelayMessenger?.send(Message().apply {
                                                        what = MSG_WHAT_CONNECTING_MSG
                                                        arg1 = 4
                                                    })
                                                    runCatching {
                                                        BluetoothQueueNew.bluetoothClient.disconnect(address)
                                                    }
                                                    return@read
                                                }
                                                BluetoothQueueNew.bluetoothClient.writeDescriptor(
                                                    BluetoothQueueNew.MAC, UUID.fromString(BluetoothQueueNew.SERVICE_UUID),
                                                    UUID.fromString(BluetoothQueueNew.CHARACTERISTIC_UUID),
                                                    UUID.fromString(BluetoothQueueNew.DESCRIPTOR_UUID),
                                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                                ) {
                                                    if(it != REQUEST_SUCCESS){
                                                        connectingRelayMessenger?.send(Message().apply {
                                                            what = MSG_WHAT_CONNECTING_MSG
                                                            arg1 = 4
                                                        })
                                                        runCatching {
                                                            BluetoothQueueNew.bluetoothClient.disconnect(address)
                                                        }
                                                        return@writeDescriptor
                                                    }
                                                    BluetoothQueueNew.notify {
                                                        connectingRelayMessenger?.send(Message().apply {
                                                            what = MSG_WHAT_CONNECTING_MSG
                                                            arg1 = 3
                                                        })
                                                        HoloApplication.INSTANCE.connect.postValue(true)
                                                    }

                                                }
                                            }
                                            return
                                        }
                                    }
                                    connectingRelayMessenger?.send(Message().apply {
                                        what = MSG_WHAT_CONNECTING_MSG
                                        arg1 = 4
                                    })
                                    runCatching {
                                        BluetoothQueueNew.bluetoothClient.disconnect(address)
                                    }

                                    return
                                }else{
                                    connectingRelayMessenger?.send(Message().apply {
                                        what = MSG_WHAT_CONNECTING_MSG
                                        arg1 = 2
                                    })
                                    runCatching {
                                        BluetoothQueueNew.bluetoothClient.disconnect(address)
                                    }
                                }
                            }
                        })
                    }else{
                        msg.replyTo.send(Message().apply {
                            what = MSG_WHAT_SCAN
                            arg1 = 0
                        })
                    }
                }
                MSG_NEW_COMMAND -> {

                    if(bluetoothQueueThread == null){
                        bluetoothQueueThread = BluetoothQueueThread()
                        bluetoothQueueThread?.start()
                    }else if(bluetoothQueueThread!!.stopFlags){
                        bluetoothQueueThread = BluetoothQueueThread()
                        bluetoothQueueThread?.start()
                    }

                    if (msg.obj is BluetoothQueueNew.Command) {
                        val command = msg.obj as BluetoothQueueNew.Command
                        BluetoothQueueNew.add(command)
                    } else{
                        runCatching {
                            val command = msg.obj as List<BluetoothQueueNew.Command>
                            BluetoothQueueNew.addAll(command)
                        }
                    }
                }
                MSG_NEW_COMMAND_P -> {

                    if(bluetoothQueueThread == null){
                        bluetoothQueueThread = BluetoothQueueThread()
                        bluetoothQueueThread?.start()
                    }else if(bluetoothQueueThread!!.stopFlags){
                        bluetoothQueueThread = BluetoothQueueThread()
                        bluetoothQueueThread?.start()
                    }

                    if (msg.obj is BluetoothQueueNew.Command) {
                        val command = msg.obj as BluetoothQueueNew.Command
                        BluetoothQueueNew.add(command.apply {
                            p = 0
                        })
                    } else{
                        runCatching {
                            val command = msg.obj as List<BluetoothQueueNew.Command>
                            BluetoothQueueNew.addAllN(command)
                        }
                    }
                }
            }

        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        val messenger = Messenger(handler)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {

        return super.onUnbind(intent)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, "holo_service")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("holo_service", getString(R.string.holo_bluetooth_service),
                    NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val int = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, int, 0)
        builder.setContentTitle("Holo")
                .setContentText(getString(R.string.bluetooth_connecting))
                .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
        startForeground(1000, builder.build())



        return super.onStartCommand(intent, flags, startId)
    }


    val bs = object: BluetoothStateListener(){
        override fun onBluetoothStateChanged(openOrClosed: Boolean) {
            if(!openOrClosed){
                HoloApplication.INSTANCE.handler.post {
                    Toast.makeText(HoloApplication.INSTANCE, getString(R.string.disconnect_please_connect_again),Toast.LENGTH_SHORT).show()
                    HoloApplication.INSTANCE.connect.postValue(false)
                }
            }else{
                HoloApplication.INSTANCE.connect.postValue(true)
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, filter)
        BluetoothQueueNew.bluetoothClient.registerBluetoothStateListener(bs)
    }

    override fun onDestroy() {
        HoloApplication.INSTANCE.connect.postValue(false)
        bluetoothQueueThread = null
        unregisterReceiver(receiver)
        BluetoothQueueNew.bluetoothClient.unregisterBluetoothStateListener(bs)
        super.onDestroy()
    }

    inner class BluetoothCheckThread(): Thread(){
        var stopFlags = false
        var delay = 2000
        override fun run() {
            super.run()
            while(!stopFlags){

                BluetoothQueueNew.addAll(ConnectionModel.check { it ->

                    val last = HoloApplication.INSTANCE.nowDeviceRun.value!!
                    if(last != it){
                        if(it){

                            HoloApplication.INSTANCE.isStopClick.postValue(false)
                            HoloApplication.INSTANCE.nowDeviceRun.postValue(it)
                            if(!HoloApplication.INSTANCE.isRunClick.value!!){

                                HoloApplication.INSTANCE.handler.post {
                                    HoloApplication.INSTANCE.nowShowStatus.value = ShowStatus()
                                    val prescription = Prescription()
                                    var title = HoloApplication.INSTANCE.currentPrescription.value!!.trackType
                                    if(title.isEmpty()){
                                        title = "手动启动"
                                    }
                                    BluetoothQueueNew.addAll(DataAdapter.getReadCommandByPrescription({
                                        HoloApplication.INSTANCE.currentPrescription.postValue(prescription.apply {
                                            trackType = title
                                            HoloApplication.INSTANCE.nowShowStatus.postValue(ShowStatus())
                                        })
                                    }, HoloApplication.INSTANCE.prescriptionSetting.value!!, prescription.apply {
                                        trackType = title
                                    }))

                                    BluetoothQueueNew.addAll(ConnectionModel.status())
                                }
                            }
                        }else{
                            HoloApplication.INSTANCE.isRunClick.postValue(false)
                            HoloApplication.INSTANCE.nowDeviceRun.postValue(it)

                            if(!HoloApplication.INSTANCE.isStopClick.value!!){
                                // 生成报表

                                val title = HoloApplication.INSTANCE.currentPrescription.value!!.trackType
                                // 同步配方
                                val prescription = HoloApplication.INSTANCE.currentPrescription.value!!.copy()
                                BluetoothQueueNew.addAll(ConnectionModel.status())
                                BluetoothQueueNew.addAll(DataAdapter.getReadCommandByPrescription({
                                    HoloApplication.INSTANCE.handler.post {
                                        HoloApplication.INSTANCE.currentPrescription.value = prescription.copy()
                                        prescription.copy().apply {

                                            trackType = title
                                            // 生成报表
                                            val reportForm = ReportForm()
                                            reportForm.prescription = this
                                            reportForm.deviceType = HoloApplication.INSTANCE.deviceId.value!!
                                            reportForm.workTime = HoloApplication.INSTANCE.nowShowStatus.value!!.workTime
                                            reportForm.endTime = System.currentTimeMillis()
                                            reportForm.startTime = reportForm.endTime - HoloApplication.INSTANCE.nowShowStatus.value!!.workTime*1000
                                            HoloApplication.INSTANCE.nowShowStatus.value?.let { ss ->
                                                for(i in ss.list){
                                                    reportForm.newTem(i.upModelTem, i.downModelTem, i.time)
                                                }
                                                for(i in ss.pressList){
                                                    reportForm.newPre(i)
                                                }
                                            }


                                            BluetoothQueueNew.addAll(ConnectionModel.readHistNum {
                                                val atomLong = AtomicLong(it)

                                                val listener:ConnectionModel.OnReadHistListener = object :ConnectionModel.OnReadHistListener{
                                                    override fun onRead(time: Long, up: Float, down: Float, pre: Float) {
                                                        if(time < reportForm.startTime){
                                                            HoloRetrofit.holoService.uploadData(HoloApplication.INSTANCE.token.value!!, DataAdapter.getReportFormBody(reportForm))
                                                                    .enqueue(object: Callback<ResponseBody>{
                                                                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                                                            runCatching {
                                                                                val s = response.body()!!.string()
                                                                                val jsonObject = JSONObject(s)
                                                                                if(jsonObject.getInt("StatusCode") == 200){
                                                                                    HoloApplication.INSTANCE.handler.post {
                                                                                        Toast.makeText(HoloApplication.INSTANCE, getString(R.string.report_upload_suc),Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }else{
                                                                                    HoloApplication.INSTANCE.handler.post {
                                                                                        Toast.makeText(HoloApplication.INSTANCE, getString(R.string.report_upload_fal), Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }
                                                                            }.onFailure { t ->
                                                                                t.printStackTrace()
                                                                                Toast.makeText(HoloApplication.INSTANCE, getString(R.string.report_upload_fal),Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        }

                                                                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                                                            Toast.makeText(HoloApplication.INSTANCE, getString(R.string.report_upload_fal),Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    })
                                                        }else{
                                                            reportForm.newTem(up, down, time)
                                                            reportForm.newPre(pre)

                                                            BluetoothQueueNew.addAll(ConnectionModel.readAHist(
                                                                    atomLong.getAndDecrement(), this
                                                            ))

                                                        }
                                                    }
                                                }
                                                BluetoothQueueNew.addAll(ConnectionModel.readAHist(
                                                        atomLong.get(), listener
                                                ))





                                            })

                                            HoloApplication.INSTANCE.currentPrescription.postValue(this)
                                        }
                                    }
                                }, HoloApplication.INSTANCE.prescriptionSetting.value!!, prescription))

                            }
                        }
                    }
                    if(it){
                        delay = 5000
                        BluetoothQueueNew.addAllN(ConnectionModel.status())
                    }else{
                        delay = 3000
                        BluetoothQueueNew.addAllN(ConnectionModel.status())
                    }
                })


                sleep(delay.toLong())
            }
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
                        if(bluetoothThread == null || bluetoothThread!!.stopFlags){
                            bluetoothThread = BluetoothCheckThread()
                            bluetoothThread?.start()
                        }
                        runCatching {
                            BluetoothQueueNew.run()
                        }
                        handler.post(this)
                    }else{
                        bluetoothThread = null
                        Looper.myLooper()?.quitSafely()
                    }
                }
            })

            Looper.loop()

        }
    }


}






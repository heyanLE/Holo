package com.heyanle.holo.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.logic.model.TestModel
import com.heyanle.modbus.ByteUtil
import com.inuker.bluetooth.library.BluetoothClient
import com.inuker.bluetooth.library.Code.REQUEST_SUCCESS
import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.connect.response.BleConnectResponse
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse
import com.inuker.bluetooth.library.connect.response.BleWriteResponse
import com.inuker.bluetooth.library.search.SearchRequest
import com.inuker.bluetooth.library.search.response.SearchResponse
import java.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit


object BluetoothQueueNew {

    const val DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    const val CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
    const val SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"

    var MAC: String = ""

    var last = 0L

    abstract class Command:Comparable<Command>{

        var addTime = 0L
        var lifecycle: Lifecycle? = null

        abstract fun run()
        var p = 1
        override fun compareTo(other: Command): Int {
            return when {
                other.p > p -> 1
                other.p == p -> {
                    when {
                        other.addTime < addTime -> 1
                        other.addTime == addTime -> 0
                        else -> -1
                    }
                }
                else -> -1
            }
        }
    }

    class WriteCommand(val byteArray: ByteArray): Command(){
        override fun run() {
            if(lifecycle!=null && !lifecycle!!.currentState.isAtLeast(Lifecycle.State.STARTED)){
                currentCommand = null

                return
            }
            Log.i("BluetoothQueue", "WriteRun")
            if(HoloApplication.DEBUG){
                TestModel.get(byteArray)
                currentCommand = null
                return
            }
            runCatching {
                bluetoothClient.write(MAC, UUID.fromString(SERVICE_UUID), UUID.fromString(CHARACTERISTIC_UUID) , byteArray
                ) {
                    Thread{
                        if(it == REQUEST_SUCCESS){
                            var s = 0
                            val byteArray = ByteArray(8)
                            while (s < byteArray.size){
                                val b = commandByte.poll(1, TimeUnit.SECONDS) ?: break
                                byteArray[s] = b
                                s ++
                            }
                            //onResult(byteArray)
                            //Thread.sleep(10)
                            currentCommand = null
                        }else{
                            run()
                        }
                    }.start()
                }
            }
        }
    }

    open class ReadCommand(val target: Int, val byteA: ByteArray): Command(){
        var onResult:(ByteArray) -> Unit = {}
        override fun run() {
            if(lifecycle!=null && !lifecycle!!.currentState.isAtLeast(Lifecycle.State.STARTED)){
                currentCommand = null
                onResult(ByteArray(8))
                return
            }
            Log.i("BluetoothQueue", "ReadRun")
            if(HoloApplication.DEBUG){
                val byteArray = TestModel.get(byteA)
                val real = ByteArray(target)
                for(i in 3 until 3+ real.size){
                    real[i-3] = byteArray[i]
                }
                onResult(real)
                currentCommand = null
                return
            }
            runCatching {
                commandByte.clear()
                bluetoothClient.write(MAC, UUID.fromString(SERVICE_UUID), UUID.fromString(CHARACTERISTIC_UUID) , byteA
                ) {
                    Thread{
                        if(it == REQUEST_SUCCESS){
                            var s = 0
                            val byteArray = ByteArray(target+5)
                            while (s < byteArray.size){
                                val b = commandByte.poll(1, TimeUnit.SECONDS) ?: break
                                byteArray[s] = b
                                s ++
                            }
                            val real = ByteArray(target)
                            for(i in 3 until 3+ real.size){
                                real[i-3] = byteArray[i]
                            }
                            onResult(real)
                            //Thread.sleep(10)
                            currentCommand = null
                        }else{
                            run()
                        }
                    }.start()

                }

            }
        }
    }

    lateinit var bluetoothClient: BluetoothClient
    private val commandList = PriorityQueue<Command>()
    private val importantCommandList: Queue<Command> = LinkedList<Command>()
    private val commandByte = LinkedBlockingQueue<Byte>()
    var currentCommand: Command? = null



    fun init(context: Context){
        bluetoothClient = BluetoothClient(context)
    }

    fun search(searchResponse: SearchResponse){
        val request = SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3) // 先扫BLE设备3次，每次3s
                .build()
        bluetoothClient.search(request, searchResponse)
    }

    fun connect(mac: String, response: BleConnectResponse){
        val options = BleConnectOptions.Builder()
                .setConnectRetry(2) // 连接如果失败重试2次
                .setConnectTimeout(2000) // 连接超时2s
                .setServiceDiscoverRetry(2) // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(2000) // 发现服务超时2s
                .build()
        bluetoothClient.connect(mac, options, response)
    }

    fun notify(onResponse: (Int)->Unit){
        bluetoothClient.notify(MAC, UUID.fromString(SERVICE_UUID), UUID.fromString(CHARACTERISTIC_UUID) , object : BleNotifyResponse{
            override fun onResponse(code: Int) {
                onResponse(code)
            }

            override fun onNotify(service: UUID?, character: UUID?, value: ByteArray?) {


                value?.let {
                    for(bb in it) {
                        commandByte.put(bb)
                    }
//                    if(currentCommand is WriteCommand){
//
//                        return
//                    }
//
//
//                    runCatching {
//                        if(it.size >= 6) {
//                            var f = 1
//                            if (it.size == 20) {
//                                f = 0
//                            }
//                            val b = ByteArray(it.size - 3 - 1 - f)
//                            for (i in 3 until (it.size - 1 - f)) {
//                                b[i - 3] = it[i]
//                            }
//                            Log.i("BluetoothQueue", ByteUtil.toHexString(b))
//                            for(bb in b) {
//                                commandByte.put(bb)
//                            }
//
//                        }
//                    }
                }
            }
        })
    }
    fun unNotify(re: BleUnnotifyResponse){}

    @Synchronized
    fun run(){
        if(currentCommand == null) {

            if(importantCommandList.isNotEmpty()){
                currentCommand = importantCommandList.poll()!!
                currentCommand?.run()
            }else if(commandList.isNotEmpty()){
                currentCommand = commandList.poll()!!
                currentCommand?.run()
            }
        }
    }

    @Synchronized
    fun add(command: Command){
        command.addTime = System.currentTimeMillis()
        BluetoothQueueNew.commandList.add(command)
    }
    @Synchronized
    fun addAll(command : List<Command>){
        for(c in command){
            c.addTime = System.currentTimeMillis()
            commandList.add(c)
        }
    }
    @Synchronized
    fun addAllN(command : List<Command>){
        for(c in command){
            c.addTime = System.currentTimeMillis()
            commandList.add(c.apply {
                p = 0
            })
        }
    }

    @Synchronized
    fun addI(command: Command){
        command.addTime = System.currentTimeMillis()
        importantCommandList.add(command)
    }

    @Synchronized
    fun addAllI(command: List<Command>){
        for(c in command){
            c.addTime = System.currentTimeMillis()
            importantCommandList.add(c)
        }
    }
    @Synchronized
    fun clear(){
        commandList.clear()
        importantCommandList.clear()
        commandByte.clear()
        currentCommand = null
    }

}
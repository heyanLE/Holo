package com.heyanle.holo.service

import android.bluetooth.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.res.TypedArrayUtils
import com.heyanle.holo.HoloApplication
import com.heyanle.modbus.ByteUtil
import kotlinx.coroutines.flow.callbackFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue

//object BluetoothQueue {
//
//    abstract class Command{
//        abstract fun run(characteristic:BluetoothGattCharacteristic,
//                         bluetoothGatt: BluetoothGatt)
//        var onTimeout:()->Unit = {
//            HoloApplication.INSTANCE.handler.post{
//                Toast.makeText(HoloApplication.INSTANCE, "Time out",Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    class ReadCommand(private val targetSize: Int): Command(){
//
//        var onResult:(ByteArray) -> Unit = {}
//
//        override fun run(characteristic:BluetoothGattCharacteristic,
//                         bluetoothGatt: BluetoothGatt) {
//            val byteArray = ByteArray(targetSize)
//            var s = 0
//            while(s < byteArray.size){
//                val b = commandByte.take()
//                byteArray[s] = b
//                s ++
//            }
//            onResult(byteArray)
//        }
//    }
//
//    class WriteCommand: Command(){
//
//        var byteArray = byteArrayOf()
//
//
//        override fun run(characteristic:BluetoothGattCharacteristic,
//                         bluetoothGatt: BluetoothGatt) {
//            buff = null
//            HoloApplication.INSTANCE.handler.post{
//                Toast.makeText(HoloApplication.INSTANCE, "Write - Run",Toast.LENGTH_SHORT).show()
//            }
//            characteristic.value = byteArray
//            bluetoothGatt.writeCharacteristic(characteristic)
//            Thread.sleep(100)
//        }
//    }
//
//
//    var lastTime = 0L
//    var timeout = 1000L
//
//
//    private val commandList = LinkedBlockingQueue<Command>()
//    private val commandByte = LinkedBlockingQueue<Byte>()
//
//    private var isAdd = false
//    fun clear(handler: Handler){
//        buff = null
//        commandList.clear()
//
//    }
//    fun size():Int{
//        return commandList.size
//    }
//    fun add(command:Command, handler: Handler){
//        commandList.put(command)
//
//    }
//
//
//    fun addAll(command:List<Command>, handler: Handler){
//        for(c in command){
//            commandList.put(c)
//        }
//
//    }
//
//    fun run(characteristic:BluetoothGattCharacteristic,
//            bluetoothGatt: BluetoothGatt){
//        val f = commandList.take()
//        lastTime = System.currentTimeMillis()
//        f.run(characteristic, bluetoothGatt)
//    }
//
//    fun onNewWrite(){
//
//
//    }
//
//    var buff:ByteArray? = null
//    fun onNewData(byteArray: ByteArray){
//        runCatching {
//            if(byteArray.size >= 6) {
//                var f = 1
//                if (byteArray.size == 20) {
//                    f = 0
//                }
//                val b = ByteArray(byteArray.size - 3 - 1 - f)
//                for (i in 3 until (byteArray.size - 1 - f)) {
//                    b[i - 3] = byteArray[i]
//                }
//                for(bb in b) {
//                    commandByte.put(bb)
//                }
//            }
//        }
//
////        HoloApplication.INSTANCE.handler.post {
////            Toast.makeText(HoloApplication.INSTANCE, ByteUtil.toHexString(b), Toast.LENGTH_SHORT).show()
////        }
//
//
//
//
//
//    }
//
//
//}


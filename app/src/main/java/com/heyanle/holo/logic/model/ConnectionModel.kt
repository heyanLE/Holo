package com.heyanle.holo.logic.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.entity.CurrentStatus
import com.heyanle.holo.entity.Device
import com.heyanle.holo.entity.Notes
import com.heyanle.holo.entity.ShowStatus
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.view.NewLineChartView
import com.heyanle.modbus.ByteUtil
import com.swallowsonny.convertextlibrary.readInt16BE
import com.swallowsonny.convertextlibrary.readUInt16BE
import com.swallowsonny.convertextlibrary.readUInt32BE
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch


/**
 * Created by HeYanLe on 2021/2/6 0006 21:57.
 * https://github.com/heyanLE
 */

object ConnectionModel {

    fun check(listener:(Boolean)->Unit):List<BluetoothQueueNew.Command>{

        val list = arrayListOf<BluetoothQueueNew.Command>()

        // 读指令
        val bb = HoloApplication.INSTANCE.modbusRtuMaster
                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 4)


        // 构造读命令
        val read = BluetoothQueueNew.ReadCommand(2, bb)
        read.onResult = {
            val int = it.readUInt16BE()
            listener(int ==1)
        }
        read.p = -1

        list.add(read)
        return list

    }
    var pressure = 0F
    var upModelTem = 0F
    var downModelTem = 0F
    var eventType = 0

    var year = 0
    var month = 0
    var day = 0
    var hour = 0
    var minute = 0
    var second = 0

    fun readHistNum(listener: (Int) -> Unit):List<BluetoothQueueNew.Command>{
        val ymRead = BluetoothQueueNew.ReadCommand(
                4, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
                BluetoothService.SLAVE_ADDRESS, 18,2)
        )
        ymRead.onResult = {
            listener(it.readUInt32BE().toInt())
            HoloApplication.INSTANCE.handler.post {
                if(it.readUInt32BE().toInt() >= 10000){
                    Toast.makeText(HoloApplication.INSTANCE, "设备记录已满，请及时清理！",Toast.LENGTH_SHORT).show()
                }
            }
        }
        return listOf(ymRead)
    }


    interface OnReadHistListener{
        fun onRead(time:Long, up:Float, down: Float, pre: Float, eventType: Int)
    }

    fun readAHist(int: Int, lifecycle: Lifecycle?, listener: OnReadHistListener)
            :List<BluetoothQueueNew.Command>{

        if(int <= 0){
            listener.onRead(-1,0F,0F,0F, 0)
            return emptyList()
        }

        val z = (int ushr 16) and 0xFFFF
        val o = int and 0xFFFF

        val writeCommand = BluetoothQueueNew.WriteCommand(
            HoloApplication.INSTANCE.modbusRtuMaster.writeHoldingRegisters(
                BluetoothService.SLAVE_ADDRESS, 4000,2, intArrayOf(z.toInt(), o.toInt()))
        )
        writeCommand.p = 2

        val fRead = BluetoothQueueNew.ReadCommand(
            8, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
                BluetoothService.SLAVE_ADDRESS, 4002, 4)
        )
        fRead.onResult = {
            eventType = it.readUInt16BE(0)
            runCatching {
                year = it[2].toInt() and 0xff
                month = it[3].toInt() and 0xff
            }
            runCatching {
                day = it[4].toInt() and 0xff
                hour = it[5].toInt() and 0xff
            }
            runCatching {
                minute = it[6].toInt() and 0xff
                second = it[7].toInt() and 0xff
            }
        }


        val preRead = BluetoothQueueNew.ReadCommand(
            6, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
                BluetoothService.SLAVE_ADDRESS, 4009, 3)
        )
        preRead.onResult = {
            pressure = it.readUInt16BE(0)/100F
            upModelTem = it.readInt16BE(2)/100F
            downModelTem = it.readInt16BE(4)/100F
        }
        val d = check {
            var time = System.currentTimeMillis()
            val pre = pressure
            val up = upModelTem
            val down = downModelTem
            runCatching {
                fun getS(i: Int):String = if (i < 10) "0$i" else "$i"
                val s = "20${getS(year)}-${getS(month)}-${getS(day)} ${getS(hour)}:${getS(minute)}:${getS(second)}"
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                time = simpleDateFormat.parse(s)!!.time
            }.onFailure {
                listener.onRead(-1, up, down,pre, eventType)
            }.onSuccess {
                listener.onRead(time, up, down, pre, eventType)
            }
        }
        val ans =  arrayListOf(writeCommand, fRead, preRead).apply {
            addAll(d)
        }
        ans.forEach {
            it.lifecycle = lifecycle
        }
        return ans
    }

    fun readAHistBlock(int: Int, lifecycle: Lifecycle?): Notes{
        val notes = Notes()
        val latch = CountDownLatch(1)
        BluetoothQueueNew.addAllI(readAHist(int,lifecycle, object: OnReadHistListener{
            override fun onRead(time: Long, up: Float, down: Float, pre: Float, eventType: Int) {
                notes.time = time
                notes.upTem = up
                notes.downTem = down
                notes.pressure = pre
                notes.eventType = eventType
                latch.countDown()
            }
        }))
        runCatching {
            latch.await()
        }.onFailure {
            it.printStackTrace()
        }
        return notes
    }

    var up = 0F
    var down = 0F
    var pre = 0F
    var targetUp = 0F
    var targetDown = 0F
    var targetPress = 0F
    var time = 0L
    var workTime = 0L
    var soakingTime =0
    var coolingTem = 0F

    fun status():List<BluetoothQueueNew.Command>{

        val cR = BluetoothQueueNew.ReadCommand(6
            , HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
                BluetoothService.SLAVE_ADDRESS, 1, 3
            ))
        cR.onResult = {
            pre = it.readUInt16BE()/100F
            up = it.readUInt16BE(2)/100F
            down = it.readUInt16BE(4)/100F
        }

        val targetUpRead = BluetoothQueueNew.ReadCommand(4,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
                BluetoothService.SLAVE_ADDRESS, 3010, 2))
        targetUpRead.onResult = {
            targetUp = it.readUInt16BE().toFloat()
            targetDown = it.readUInt16BE(2).toFloat()
        }

        val targetPreRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                        BluetoothService.SLAVE_ADDRESS, 3007))
        targetPreRead.onResult = {
            targetPress = it.readUInt16BE()/100F
        }

        val workTimeRead = BluetoothQueueNew.ReadCommand(4,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
                        BluetoothService.SLAVE_ADDRESS, 7, 2))
        workTimeRead.onResult = {
            Log.i("xxxxxx", "${it.readUInt32BE()}")
            workTime = it.readUInt32BE()

        }

        val soakingTimeRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                        BluetoothService.SLAVE_ADDRESS, 3012))
        soakingTimeRead.onResult = {
            soakingTime = it.readUInt16BE()
        }

        val coolingTimeRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                        BluetoothService.SLAVE_ADDRESS, 3013))
        coolingTimeRead.onResult = {
            coolingTem  = it.readUInt16BE().toFloat()
            HoloApplication.INSTANCE.nowShowStatus.value?.let {  sss ->
                if(up !=0F && down != 0F)
                sss.list.add(NewLineChartView.TemData().also { t ->
                    t.time = System.currentTimeMillis()
                    t.upModelTem = up
                    t.downModelTem = down
                })
                sss.pressList.add(pre)
                sss.targetDown = targetDown
                sss.targetUp = targetUp
                sss.targetPre = targetPress
                sss.coolingTem = coolingTem
                sss.soakingTime = soakingTime
                sss.workTime = workTime
                HoloApplication.INSTANCE.nowShowStatus.postValue(sss)
            }

            HoloApplication.INSTANCE.realShowStatus.value?.let {  sss ->
                if(up !=0F && down != 0F)
                    sss.list.add(NewLineChartView.TemData().also { t ->
                        t.time = System.currentTimeMillis()
                        t.upModelTem = up
                        t.downModelTem = down
                    })
                sss.pressList.add(pre)
                sss.targetDown = targetDown
                sss.targetUp = targetUp
                sss.targetPre = targetPress
                sss.coolingTem = coolingTem
                sss.soakingTime = soakingTime
                sss.workTime = workTime
                HoloApplication.INSTANCE.realShowStatus.postValue(sss)
            }
        }
        return arrayListOf(cR, targetUpRead, targetPreRead, workTimeRead, soakingTimeRead,
            coolingTimeRead
        ).onEach {
            it.p = 0
        }.also {
            it.last().p = -1
        }


    }


    var endTime = HoloApplication.INSTANCE.showReportForm.endTime

    fun makeReport(listener: ()->Unit){
        // endtime
    }







    fun writeP():List<BluetoothQueueNew.Command>{
        val list = arrayListOf<BluetoothQueueNew.Command>()

        val writeList = DataAdapter.getWriteCommandByPrescription(
                HoloApplication.INSTANCE.prescriptionSetting.value!!,
                HoloApplication.INSTANCE.currentPrescription.value!!
        )
        list.addAll(writeList)
        return list
    }
    fun run():List<BluetoothQueueNew.Command>{

        val list = arrayListOf<BluetoothQueueNew.Command>()

        val writeList = DataAdapter.getWriteCommandByPrescription(
                HoloApplication.INSTANCE.prescriptionSetting.value!!,
                HoloApplication.INSTANCE.currentPrescription.value!!
        )
        //list.addAll(writeList)
        //BluetoothQueueNew.commandList.addAll(writeList)

        val bb = HoloApplication.INSTANCE.modbusRtuMaster
                .writeSingleRegister(BluetoothService.SLAVE_ADDRESS, 5001, 9865)
        val write = BluetoothQueueNew.WriteCommand(bb)
        //BluetoothQueueNew.commandList.add(write)

        list.add(write)
        return list

    }

    fun stop():List<BluetoothQueueNew.Command> {
        val list = arrayListOf<BluetoothQueueNew.Command>()
        HoloApplication.INSTANCE.runReportForm.value?.endTime = System.currentTimeMillis()
        val bb = HoloApplication.INSTANCE.modbusRtuMaster
                .writeSingleRegister(BluetoothService.SLAVE_ADDRESS, 5002, 39010)
        val write = BluetoothQueueNew.WriteCommand(bb)
        list.add(write)
        return list
    }

}


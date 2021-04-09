package com.heyanle.holo.logic.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.entity.CurrentStatus
import com.heyanle.holo.entity.Device
import com.heyanle.holo.entity.ShowStatus
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.view.NewLineChartView
import com.heyanle.modbus.ByteUtil
import com.swallowsonny.convertextlibrary.readUInt16BE
import com.swallowsonny.convertextlibrary.readUInt32BE
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


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
    var yearMonth = 0
    var dayHour = 0
    var minuteSecond = 0
    var pressure = 0F
    var upModelTem = 0F
    var downModelTem = 0F

    fun readHistNum(listener: (Long) -> Unit):List<BluetoothQueueNew.Command>{
        val ymRead = BluetoothQueueNew.ReadCommand(
                4, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegisters(
                BluetoothService.SLAVE_ADDRESS, 18,2)
        )
        ymRead.onResult = {
            listener(it.readUInt32BE())
        }
        return listOf(ymRead)
    }


    interface OnReadHistListener{
        fun onRead(time:Long, up:Float, down: Float, pre: Float)
    }
    fun readAHist(int: Long, listener: OnReadHistListener)
    :List<BluetoothQueueNew.Command>{

        if(int <= 0){
            listener.onRead(-1,0F,0F,0F)
        }

        val z = (int ushr 16) and 0xFFFF
        val o = int and 0xFFFF

        val writeCommand = BluetoothQueueNew.WriteCommand(
                HoloApplication.INSTANCE.modbusRtuMaster.writeHoldingRegisters(
                        BluetoothService.SLAVE_ADDRESS, 4000,2, intArrayOf(z.toInt(), o.toInt()))
        )

        val ymRead = BluetoothQueueNew.ReadCommand(
                2, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 4003)
        )
        ymRead.onResult = {
            yearMonth = it.readUInt16BE()
        }

        val dhRead = BluetoothQueueNew.ReadCommand(
                2, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 4004)
        )
        dhRead.onResult = {
            dayHour = it.readUInt16BE()
        }

        val msRead = BluetoothQueueNew.ReadCommand(
                2, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 4005)
        )
        msRead.onResult = {
            minuteSecond = it.readUInt16BE()
        }

        val preRead = BluetoothQueueNew.ReadCommand(
                2, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 4009)
        )
        preRead.onResult = {
            pressure = it.readUInt16BE()/100F
        }

        val upRead = BluetoothQueueNew.ReadCommand(
                2, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 4003)
        )
        upRead.onResult = {
            upModelTem = it.readUInt16BE()/100F
        }

        val downRead = BluetoothQueueNew.ReadCommand(
                2, HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 4003)
        )
        downRead.onResult = {
            downModelTem = it.readUInt16BE()/100F
        }

        val d = check {
            var time = System.currentTimeMillis()
            var pre = pressure
            var up = upModelTem
            var down = downModelTem
            runCatching {
                val year = (yearMonth.toString()).subSequence(0, 4)
                val month = (yearMonth.toString()).subSequence(4, 6)
                val day = dayHour.toString().subSequence(0,2)
                val hour = dayHour.toString().subSequence(2,4)
                val min = minuteSecond.toString().subSequence(0,2)
                val sec = minuteSecond.toString().subSequence(2,4)

                val s = "$year-$month-$day $hour:$min:$sec"
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                time = simpleDateFormat.parse(s)!!.time
            }.onFailure {
                listener.onRead(-1, pre, up, down)
            }.onSuccess {
                listener.onRead(time, pre, up, down)
            }
        }
        return arrayListOf(writeCommand, ymRead, dhRead, msRead, preRead, upRead, downRead).apply {
            addAll(d)
        }

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
        val currentUpRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 2))
        currentUpRead.onResult = {

            up = it.readUInt16BE()/100F
        }
        val currentDownRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                        BluetoothService.SLAVE_ADDRESS, 3))
        currentDownRead.onResult = {
            down = it.readUInt16BE()/100F
        }

        val currentPreRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                        BluetoothService.SLAVE_ADDRESS, 1))
        currentPreRead.onResult = {
            pre = it.readUInt16BE()/100F
        }

        val targetUpRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                BluetoothService.SLAVE_ADDRESS, 3010))
        targetUpRead.onResult = {
            targetUp = it.readUInt16BE().toFloat()
        }
        val targetDownRead = BluetoothQueueNew.ReadCommand(2,
                HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(
                        BluetoothService.SLAVE_ADDRESS, 3011))
        targetDownRead.onResult = {
            targetDown = it.readUInt16BE().toFloat()
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
        return arrayListOf(currentDownRead, currentUpRead, currentPreRead,
                targetDownRead, targetUpRead, targetPreRead, workTimeRead, soakingTimeRead, coolingTimeRead
        )


    }


    var endTime = HoloApplication.INSTANCE.showReportForm.endTime

    fun makeReport(listener: ()->Unit){
        // endtime
    }





    fun getStatus(listener:(up:Float, down:Float, pre:Float, time:Long)->Unit):List<BluetoothQueueNew.Command>{

        val list = arrayListOf<BluetoothQueueNew.Command>()

        // 读指令
        val pre = HoloApplication.INSTANCE.modbusRtuMaster
                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 1)
        val up = HoloApplication.INSTANCE.modbusRtuMaster
                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 2)
        val down = HoloApplication.INSTANCE.modbusRtuMaster
                .readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 3)

        var preValue = -1F
        var upValue = -1F
        var downValue = -1F

        val preRead = BluetoothQueueNew.ReadCommand(2, pre)
        val upRead = BluetoothQueueNew.ReadCommand(2, up)
        val downRead = BluetoothQueueNew.ReadCommand(2, down)

        preRead.onResult = {
            val int = it.readUInt16BE()
            val f = int/100F
            preValue = f
            if(preValue != -1F && upValue!=-1F && downValue !=-1F){
                listener(upValue, downValue, preValue, System.currentTimeMillis())
            }
        }
        upRead.onResult = {
            val int = it.readUInt16BE()
            val f = int/100F
            upValue = f
            if(preValue != -1F && upValue!=-1F && downValue !=-1F){
                listener(upValue, downValue, preValue, System.currentTimeMillis())
            }
        }
        downRead.onResult = {
            val int = it.readUInt16BE()
            val f = int/100F
            downValue = f
            if(preValue != -1F && upValue!=-1F && downValue !=-1F){
                listener(upValue, downValue, preValue, System.currentTimeMillis())
            }
        }

        list.add(preRead)
        list.add(upRead)
        list.add(downRead)

        return list
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


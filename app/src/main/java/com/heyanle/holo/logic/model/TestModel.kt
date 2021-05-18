package com.heyanle.holo.logic.model

import android.util.Log
import com.swallowsonny.convertextlibrary.*
import java.util.*
import kotlin.concurrent.timer

/**
 * Created by HeYanLe on 2021/5/11 15:52.
 * https://github.com/heyanLE
 */
object TestModel {

    var startTime: Long=  0L
    var endTime: Long = 0L

    var k = 0.0
    var b = 0.0

    init {
        endTime = System.currentTimeMillis()
        startTime = endTime - 40*60*1000

        k = 40*60*1000/29.0
        b = -30*(40*60*1000)/29.0+ endTime
    }

    val hashMap = hashMapOf<Int, Int>()


    fun get(byteArray: ByteArray): ByteArray {
        Log.i("TestModel", "get ${byteArray[1].toInt()}")


        runCatching {
            Thread.sleep(50)
            when(byteArray[1].toInt()){
                3 -> { // 读保存寄存器
                    val startAddress = byteArray.readUInt16BE(2)
                    val num = byteArray.readUInt16BE(4)



                    val r = arrayListOf<Int>()
                    for(i in 1..num){
                        r.add(read(startAddress + i -1))
                    }

                    val result = arrayListOf<Byte>(0,0,0)
                    if(startAddress == 18 && num == 2){
                        val r = ByteArray(4)
                        r.writeInt16BE(30,2)
                        result.addAll(r.toTypedArray())
                    }else {
                        r.forEach {
                            val r = ByteArray(2)
                            r.writeInt16BE(it)
                            result.addAll(r.toTypedArray())
                        }
                    }

                    result.add(0)
                    result.add(0)
                    return result.toTypedArray().toByteArray()


                }
                6 -> { // 写单个保持寄存器
                    val address = byteArray.readUInt16BE(2)
                    val value = byteArray.readUInt16BE(4)
                    write(address, value)
                    return ByteArray(8)
                }
                16 -> { // 写多个保持寄存器
                    var startAddress = byteArray.readUInt16BE(2)
                    val num = byteArray.readUInt16BE(4)

                    var i = 7
                    var n = num
                    while(n > 0){
                        write(startAddress, byteArray.readUInt16BE(i))
                        startAddress += 1
                        n--
                        i += 2
                    }

                    if(byteArray.readUInt16BE(2) == 4000){
                        val i = byteArray.readInt32BE(7)
                        var nT = k*i+ b
                        if(i == 30){
                            setTime(endTime, i)
                        }else if(i == 1){
                            setTime(startTime, i)
                        }else{
                            setTime(nT.toLong(), i)
                        }


                    }
                    return ByteArray(8)
                }
                else -> {}
            }
        }.onFailure {
            it.printStackTrace()
        }

        return byteArrayOf()
    }

    fun write(address: Int, value:Int){
        Log.i("TestModel","Write ${address} ${value}")
        hashMap[address] = value

    }
    fun read(address: Int): Int{
        Log.i("TestModel","Read ${address} ${hashMap[address]?:30}")
        return hashMap[address]?:30
    }

    fun setTime(time: Long, index: Int){
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time

        val year = calendar.get(Calendar.YEAR) % 100
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val sec = calendar.get(Calendar.SECOND)

        val byteArray = ByteArray(2)
        byteArray.writeInt8(year)
        byteArray.writeInt8(month, 1)
        hashMap[4003] = byteArray.readInt16BE()

        byteArray.writeInt8(day)
        byteArray.writeInt8(hour, 1)
        hashMap[4004] = byteArray.readInt16BE()

        byteArray.writeInt8(minute)
        byteArray.writeInt8(sec, 1)
        hashMap[4005] = byteArray.readInt16BE()

        hashMap[4010] = 3000 + index*100
        hashMap[4011] = 2000 + index*100
        hashMap[4009] = 3000 + index*100


        if(time == startTime){
            hashMap[4002]=0x00E2
        }else if(time == endTime){
            hashMap[4002]=0x00E3
        }else{
            hashMap[4002]=0x00E1
        }

        if(index == 15){
            hashMap[4002]=0x00E3
        }else if(index == 16){
            hashMap[4002]=0x00E2
        }

    }

}
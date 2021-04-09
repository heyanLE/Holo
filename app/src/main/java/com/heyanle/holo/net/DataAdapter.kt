package com.heyanle.holo.net

import android.animation.FloatEvaluator
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.entity.*
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.view.NewLineChartView
import com.heyanle.modbus.ByteUtil
import com.swallowsonny.convertextlibrary.readUInt16BE
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.SimpleFormatter
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object DataAdapter {

    fun getDeviceDescribe(jsonObject: JSONObject): DeviceDescribe{
        val d = DeviceDescribe()
        d.brand = jsonObject.getString("pinpai")
        d.alias = jsonObject.getString("bieming")
        d.type = jsonObject.getString("xinghao")
        d.productUse = jsonObject.getString("gyongtu")
        d.useI = jsonObject.getString("yongtu")
        d.useII = jsonObject.getString("guige")
        d.img = ""
        runCatching { 
            d.img = jsonObject.getString("WebSite")
        }

        return d
    }

    fun getPrescriptionList(jsonArray: JSONArray):ArrayList<Prescription>{
        val p = arrayListOf<Prescription>()


        for(i in 0 until jsonArray.length()){
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getString("shibiehao")
            if(id != HoloApplication.INSTANCE.deviceN.value!!){
                continue
            }

            val pre = Prescription()
            pre.trackType = jsonObject.getString("pidai")
            val entry = jsonObject.getJSONArray("Entry")
            pre.unique = jsonObject.getString("FInterID")

            val map = hashMapOf<String, SettingEntity<*>>()
            for(j in HoloApplication.INSTANCE.prescriptionSetting.value!!){
                map[j.title] = j
            }
            for(j in 0 until entry.length()){
                val o = entry.getJSONObject(j)
                val name = o.getString("FName")
                if(!map.containsKey(name)){
                    continue
                }
                when(map[name]!!.address){
                    3005 -> { // 预热气压
                        pre.pressure = o.getString("FValue").toFloat()
                    }
                    3006 -> { // 预热开关
                        pre.isPreheatingPreloading = o.getString("FValue") == "1"
                    }
                    3007 -> { // 气压设定
                        pre.pressure = o.getString("FValue").toFloat()
                    }
                    3008 -> { // 预热温度设定值
                        pre.preheatingTemperature = o.getString("FValue").toFloat()
                    }
                    3009 -> { // 预热保温时间
                        pre.preheatingSoakingTime = o.getInt("FValue")
                    }
                    3010 -> { // 上模
                        pre.upModelTemperature = o.getInt("FValue").toFloat()
                    }
                    3011 -> { // 下模
                        pre.downModelTemperature = o.getInt("FValue").toFloat()
                    }
                    3012 -> { // 接头保温时间
                        pre.soakingTime = o.getInt("FValue")
                    }
                    3013 -> { // 冷却温度设定
                        pre.coolingTemperature = o.getInt("FValue").toFloat()
                    }
                }

            }

            p.add(pre)


        }

        return p
    }

    fun getStarNetBody(prescription: Prescription):HashMap<String, StarNetBody>{
        val hashMap = hashMapOf<String, StarNetBody>()
        val starNetBody = StarNetBody()

        starNetBody.pidai = prescription.trackType
        starNetBody.shibiehao = HoloApplication.INSTANCE.deviceN.value!!
        val s = HoloApplication.INSTANCE.prescriptionSetting.value!!
        for(p in s){
            when(p.address){
                3005 -> { // 预热气压
                    val f = p as FloatSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.preloading}"
                    })
                }
                3006 -> { // 预热开关
                    val f = p as BooleanSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${if(prescription.isPreheatingPreloading)1 else 0}"
                    })
                }
                3007 -> { // 气压设定
                    val f = p as FloatSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.pressure}"
                    })
                }
                3008 -> { // 预热温度设定值
                    val f = p as FloatSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.preheatingTemperature}"
                    })
                }
                3009 -> { // 预热保温时间
                    val f = p as IntSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.preheatingSoakingTime}"
                    })
                }
                3010 -> { // 上模
                    val f = p as FloatSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.upModelTemperature}"
                    })
                }
                3011 -> { // 下模
                    val f = p as FloatSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.downModelTemperature}"
                    })
                }
                3012 -> { // 接头保温时间
                    val f = p as IntSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.soakingTime}"
                    })
                }
                3013 -> { // 冷却温度设定
                    val f = p as FloatSetting
                    starNetBody.entity.add(StarNetBodyEntity().apply {
                        fName = f.title
                        fValue = "${prescription.coolingTemperature}"
                    })
                }
            }
        }

        hashMap["Data"] = starNetBody
        return hashMap
    }

    fun getSettingArray(jsonArray: JSONArray): ArrayList<SettingEntity<*>>{
        val d = arrayListOf<SettingEntity<*>>()

        for(i in 0 until jsonArray.length()){
            val o = jsonArray.getJSONObject(i)
            val add = o.getString("QAdress")
            when(add){
                "3005" -> { // 预热气压
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val floatEntity = FloatSetting(min.toFloat(),
                            o.getString("FName")
                            ,3005, R.id.layout_preloading)

                    floatEntity.max = max.toFloat()
                    floatEntity.min = min.toFloat()
                    d.add(floatEntity)
                }
                "3006"->{ // 预热开关
                    val booleanEntity = BooleanSetting(true, o.getString("FName")
                            , 3006,R.id.layout_preheatingPreloading)
                    d.add(booleanEntity)
                }
                "3007" -> { // 气压设定值
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val floatEntity = FloatSetting(min.toFloat(),
                            o.getString("FName")
                            ,3007, R.id.layout_pressure)

                    floatEntity.max = max.toFloat()
                    floatEntity.min = min.toFloat()
                    d.add(floatEntity)
                }
                "3008" -> {// 预热温度值
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val floatEntity = FloatSetting(min.toFloat(),
                            o.getString("FName")
                            ,3008, R.id.layout_preheatingTemperature)

                    floatEntity.max = max.toFloat()
                    floatEntity.min = min.toFloat()
                    d.add(floatEntity)
                }
                "3009" -> {// 预热保温时间
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val intEntity = IntSetting(min.toFloat().toInt(),
                            o.getString("FName")
                            ,3009, R.id.layout_preheatingSoakingTime)

                    intEntity.max = max.toFloat().toInt()
                    intEntity.min = min.toFloat().toInt()
                    d.add(intEntity)
                }
                "3010" -> { // 上摸温度
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val floatEntity = FloatSetting(min.toFloat(),
                            o.getString("FName")
                            ,3010, R.id.layout_upModelTemperature)

                    floatEntity.max = max.toFloat()
                    floatEntity.min = min.toFloat()
                    d.add(floatEntity)
                }
                "3011"->{ // 下模温度
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val floatEntity = FloatSetting(min.toFloat(),
                            o.getString("FName")
                            ,3011, R.id.layout_downModelTemperature)

                    floatEntity.max = max.toFloat()
                    floatEntity.min = min.toFloat()
                    d.add(floatEntity)
                }
                "3012"->{ //接头保温时间
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val intEntity = IntSetting(min.toFloat().toFloat().toInt(),
                            o.getString("FName")
                            ,3012, R.id.layout_SoakingTime)

                    intEntity.max = max.toFloat().toInt()
                    intEntity.min = min.toFloat().toInt()
                    d.add(intEntity)
                }
                "3013"->{ // 冷却温度设定值
                    val max = o.getString("Max")
                    val min = o.getString("Min")
                    val floatEntity = FloatSetting(min.toFloat(),
                            o.getString("FName")
                            ,3013,R.id.layout_coolingTemperature)
                    floatEntity.max = max.toFloat()
                    floatEntity.min = min.toFloat()
                    d.add(floatEntity)
                }
            }
        }
        return d
    }

    fun getWriteCommandByPrescription(
            list: List<SettingEntity<*>>,
            prescription: Prescription): ArrayList<BluetoothQueueNew.WriteCommand>{
        val writeList = arrayListOf<BluetoothQueueNew.WriteCommand>()
        for(sc in list){
            when(sc.address){
                3005 -> { // 预热气压
                    if(!prescription.isPreheatingPreloading){
                        break
                    }

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3005, ((prescription.preloading as Float)*100).toFloat().toInt())
                    //write.byteArray = b
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3006 -> { // 预热开关

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3006, if(prescription.isPreheatingPreloading){1} else {0})
                    //write.byteArray = b
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3007 -> { // 气压设定

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3007, ((prescription.pressure as Float)*100).toFloat().toInt())
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3008 -> { // 预热温度设定值
                    if(!prescription.isPreheatingPreloading){
                        break
                    }

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3008, ((prescription.preheatingTemperature as Float)).toFloat().toInt())
                    //.byteArray = b
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3009 -> { // 预热保温时间
                    if(!prescription.isPreheatingPreloading){
                        break
                    }

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3009, prescription.preheatingSoakingTime as Int)
                    //write.byteArray = b
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3010 -> { // 上模

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3010, ((prescription.upModelTemperature)).toFloat().toInt())
                    //write.byteArray = b
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3011 -> { // 下模

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3011, ((prescription.downModelTemperature as Float)).toFloat().toInt())
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3012 -> { // 接头保温时间

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3012, prescription.soakingTime)
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
                3013 -> { // 冷却温度设定

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .writeSingleRegister(BluetoothService.SLAVE_ADDRESS,
                                    3013, ((prescription.coolingTemperature as Float)).toFloat().toInt())
                    val write = BluetoothQueueNew.WriteCommand(b)
                    writeList.add(write)
                }
            }
        }


        return writeList
    }

    fun getReadCommandByPrescription(
            listener: ()->Unit,
            address: List<SettingEntity<*>>,
            prescription: Prescription): ArrayList<BluetoothQueueNew.Command>{
        val list = arrayListOf<BluetoothQueueNew.Command>()
        for(sc in address){
            when(sc.address){
                3005 -> { // 预热气压
                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3005)
                    val read = BluetoothQueueNew.ReadCommand(2,b)
                    read.onResult = {
                        prescription.preloading = it.readUInt16BE()/100F
                    }
                    list.add(read)
                }
                3006 -> { // 预热开关

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3006)
                    val read = BluetoothQueueNew.ReadCommand(2,b)
                    read.onResult = {
                        prescription.isPreheatingPreloading = it.readUInt16BE() == 1
                    }
                    list.add(read)
                }
                3007 -> { // 气压设定

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3007)
                    val read = BluetoothQueueNew.ReadCommand(2,b)
                    read.onResult = {
                        prescription.pressure = (it.readUInt16BE().toFloat())/100F
                    }

                    list.add(read)
                }
                3008 -> { // 预热温度设定值

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3008)
                    val read = BluetoothQueueNew.ReadCommand(2, b)

                    read.onResult = {
                        prescription.preheatingTemperature = it.readUInt16BE().toFloat()
                    }

                    list.add(read)
                }
                3009 -> { // 预热保温时间

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3009)
                    val read = BluetoothQueueNew.ReadCommand(2, b)

                    read.onResult = {
                        prescription.preheatingSoakingTime = it.readUInt16BE().toInt()
                    }

                    list.add(read)
                }
                3010 -> { // 上模

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3010)
                    val read = BluetoothQueueNew.ReadCommand(2, b)

                    read.onResult = {
                        prescription.upModelTemperature = it.readUInt16BE().toFloat()

                    }

                    list.add(read)
                }
                3011 -> { // 下模

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3011)
                    val read = BluetoothQueueNew.ReadCommand(2, b)

                    read.onResult = {
                        prescription.downModelTemperature = it.readUInt16BE().toFloat()
//                        HoloApplication.INSTANCE.handler.post{
//                            Toast.makeText(HoloApplication.INSTANCE, ByteUtil.toHexString(it),Toast.LENGTH_SHORT).show()
//                        }
                    }

                    list.add(read)
                }
                3012 -> { // 接头保温时间

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3012)
                    val read = BluetoothQueueNew.ReadCommand(2,b)

                    read.onResult = {
                        prescription.soakingTime = it.readUInt16BE()
                    }

                    list.add(read)
                }
                3013 -> { // 冷却温度设定

                    val b = HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegister(BluetoothService.SLAVE_ADDRESS,
                                    3013)
                    val read = BluetoothQueueNew.ReadCommand(2, b)

                    read.onResult = {
                        prescription.coolingTemperature = it.readUInt16BE().toFloat()
                    }

                    list.add(read)
                }
            }
        }

        list.addAll(ConnectionModel.check {
            listener()
        })

        return list
    }

    fun getReportFormBody(reportForm: ReportForm): HashMap<String,ReportFormBody>{
        val map = hashMapOf<String,ReportFormBody>()
        val reportFormBody = ReportFormBody()
        val simpleFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.getDefault())
        reportFormBody.startTime = simpleFormatter.format(Date().apply {
            time = reportForm.startTime
        })
        reportFormBody.endTime = simpleFormatter.format(Date().apply {
            time = reportForm.endTime
        })
        reportFormBody.deviceID = HoloApplication.INSTANCE.deviceId.value!!
        reportFormBody.pidai = reportForm.prescription.trackType
        reportFormBody.totalTime = "${reportForm.endTime - reportForm.startTime}"
        reportFormBody.isPreheatingPreloading = "${if(reportForm.prescription.isPreheatingPreloading)1 else 0}"
        reportFormBody.preloading = "${reportForm.prescription.preloading}"
        reportFormBody.preheatingTem = "${reportForm.prescription.preheatingTemperature}"
        reportFormBody.preheatingSoakingTime = "${reportForm.prescription.preheatingSoakingTime}"
        reportFormBody.soakingTime = "${reportForm.prescription.soakingTime}"
        reportFormBody.coolingTem = "${reportForm.prescription.coolingTemperature}"
        reportFormBody.downModelTargetTem = "${reportForm.prescription.downModelTemperature}"
        reportFormBody.upModelTargetTem = "${reportForm.prescription.upModelTemperature}"
        reportFormBody.targetPressure = "${reportForm.prescription.pressure}"

        reportFormBody.downModelMax = "${reportForm.getMaxDownTem()}"
        reportFormBody.downModelMin = "${reportForm.getMinDownTem()}"
        reportFormBody.upModelMax = "${reportForm.getMaxUpTem()}"
        reportFormBody.upModelMin = "${reportForm.getMinUpTem()}"

        reportFormBody.maxPressure = "${reportForm.maxPre}"
        reportFormBody.minPressure = "${reportForm.minPre}"



        for(d in reportForm.list){
            reportFormBody.upEntities.add(ReportFormBodyEntity().apply {
                fTime = "${d.time}"
                fValue = "${d.upModelTem}"
            })
            reportFormBody.downEntities.add(ReportFormBodyEntity().apply {
                fTime = "${d.time}"
                fValue = "${d.downModelTem}"
            })
        }

        map["Data"] = reportFormBody
        return map
    }

    fun getReportFormList(r: List<ReportFormBody>):ArrayList<ReportForm>{
        val list = arrayListOf<ReportForm>()
        for(rr in r){
            runCatching {
                list.add(getReportForm(rr))
            }
        }
        return list
    }

    fun getReportForm(reportFormBody: ReportFormBody): ReportForm{
        val r = ReportForm()
        val simpleFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.getDefault())
        r.startTime = simpleFormatter.parse(reportFormBody.startTime)!!.time
        r.endTime = simpleFormatter.parse(reportFormBody.endTime)!!.time
        r.maxPre = reportFormBody.maxPressure.toFloat()
        r.minPre = reportFormBody.minPressure.toFloat()
        r.deviceType = reportFormBody.deviceID
        r.workTime = reportFormBody.totalTime.toLong()

        val prescription = Prescription()
        prescription.trackType = reportFormBody.pidai
        prescription.pressure = reportFormBody.targetPressure.toFloat()
        prescription.upModelTemperature = reportFormBody.upModelTargetTem.toFloat()
        prescription.downModelTemperature = reportFormBody.downModelTargetTem.toFloat()
        prescription.coolingTemperature = reportFormBody.coolingTem.toFloat()
        prescription.soakingTime = reportFormBody.soakingTime.toFloat().toInt()
        prescription.preheatingSoakingTime = reportFormBody.preheatingSoakingTime.toFloat().toInt()
        prescription.preheatingTemperature = reportFormBody.preheatingTem.toFloat()
        prescription.preloading = reportFormBody.preloading.toFloat()
        prescription.isPreheatingPreloading = reportFormBody.isPreheatingPreloading.toFloat().toInt() == 1
        //prescription.trackType = "报表载入"

        r.prescription = prescription


        val hashMap = hashMapOf<Long, NewLineChartView.TemData>()
        for(d in reportFormBody.upEntities){
            hashMap[d.fTime.toLong()] = NewLineChartView.TemData()
                    .apply {
                        time = d.fTime.toLong()
                        upModelTem = d.fValue.toFloat()
                    }
        }
        for(d in reportFormBody.downEntities){
            if(hashMap.containsKey(d.fTime.toLong())){
                hashMap[d.fTime.toLong()]?.downModelTem = d.fValue.toFloat()
            }
        }

        r.list.clear()
        r.list.addAll(hashMap.values)

        return r
    }


}

class StarNetBody{
    var shibiehao = ""
    var pidai = ""

    @SerializedName("Entry")
    var entity = arrayListOf<StarNetBodyEntity>()
}

class StarNetBodyEntity{
    @SerializedName("FName")
    var fName = ""

    @SerializedName("FValue")
    var fValue = ""
}

class ReportFormBody{
    @SerializedName("StartTime")
    var startTime = ""

    @SerializedName("EndTime")
    var endTime = ""

    @SerializedName("FNumber")
    var deviceID = ""

    @SerializedName("TotalTime")
    var totalTime = ""

    @SerializedName("pidai")
    var pidai = ""

    @SerializedName("yu_kaiguan")
    var isPreheatingPreloading = "" // 预热开关

    @SerializedName("yu_ya")
    var preloading = "" // 预压

    @SerializedName("yu_wendu")
    var preheatingTem = ""// 预热温度

    @SerializedName("yu_baowen")
    var preheatingSoakingTime = "" // 预热保温时间

    @SerializedName("jietoubaowen")
    var soakingTime = ""// 接头保温时间

    @SerializedName("lengque")
    var coolingTem = ""// 冷却温度

    @SerializedName("x_wendu")
    var downModelTargetTem = ""//下模设定温度

    @SerializedName("x_max")
    var downModelMax = "" // 下模最大温度

    @SerializedName("x_min")
    var downModelMin = "" // 下模最小温度

    @SerializedName("s_wendu")
    var upModelTargetTem = "" // 上摸设定温度

    @SerializedName("s_max")
    var upModelMax = "" // 上模最大温度

    @SerializedName("s_min")
    var upModelMin = "" // 上模最小温度

    @SerializedName("q_bar")
    var targetPressure = "" // 设定气压
    @SerializedName("q_max")
    var maxPressure = "" // 最大气压
    @SerializedName("q_min")
    var minPressure = "" // 最小气压

    @SerializedName("sentry")
    val upEntities = arrayListOf<ReportFormBodyEntity>()

    @SerializedName("xentry")
    val downEntities = arrayListOf<ReportFormBodyEntity>()



}

class ReportFormBodyEntity{

    @SerializedName("FTime")
    var fTime = ""
    @SerializedName("FValue")
    var fValue = ""
}
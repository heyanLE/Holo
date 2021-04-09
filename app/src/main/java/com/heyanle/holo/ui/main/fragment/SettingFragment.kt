package com.heyanle.holo.ui.main.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.FragmentSettingBinding
import com.heyanle.holo.entity.*
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.logic.viewmodel.MainViewModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.dialog.EditDialog
import com.heyanle.holo.utils.observeWithNotify
import com.swallowsonny.convertextlibrary.readUInt16BE
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by HeYanLe on 2021/2/7 0007 14:52.
 * https://github.com/heyanLE
 */


class SettingFragment : PageFragment(R.layout.fragment_setting){

    override val title: String
        get() =
            HoloApplication.INSTANCE.getString(R.string.setting)

    private lateinit var binding: FragmentSettingBinding

    private var prescription: Prescription = Prescription()

    private val activityVM by activityViewModels<MainViewModel>()

    private var ifPreloading = false
    private var ifPreheatingTemperature = false
    private var ifPreheatingSoakingTime = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentSettingBinding.bind(v!!)
        initView()
        return v
    }

    @SuppressLint("SetTextI18n")
    private fun initView(){

        HoloApplication.INSTANCE.currentPrescription.observeWithNotify(viewLifecycleOwner, {
            //save()
            prescription = it
            binding.prescription = it
            binding.switchPreheatingPreloading.isChecked = it.isPreheatingPreloading
        })
        HoloApplication.INSTANCE.isClick.observeWithNotify(viewLifecycleOwner){
            binding.btRun.isEnabled = !it
            binding.btStop.isEnabled = !it
        }
        binding.btStar.setOnClickListener {
            if(binding.etTrackType.text.isEmpty()){
                Toast.makeText(requireContext(), R.string.please_enter_track_type,Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            save()
            HoloApplication.INSTANCE.prescriptionList.value?.let {

                HoloRetrofit.holoService.star(HoloApplication.INSTANCE.token.value!!, DataAdapter.getStarNetBody(prescription))
                        .enqueue(object: Callback<ResponseBody>{
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                runCatching {
                                    val s = response.body()!!.string()
                                    val jsonObject = JSONObject(s)
                                    val code = jsonObject.getInt("StatusCode")
                                    if(code == 200){
                                        Toast.makeText(requireContext(), R.string.star_success,Toast.LENGTH_SHORT).show()
                                    }else{
                                        Toast.makeText(requireContext(), jsonObject.getString("Info"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Toast.makeText(requireContext(), getString(R.string.star_fal),Toast.LENGTH_SHORT).show()
                            }
                        })
            }
        }



        binding.tvDeviceId.text = HoloApplication.INSTANCE.deviceId.value
        HoloApplication.INSTANCE.deviceId.observe(viewLifecycleOwner){
            binding.tvDeviceId.text = it
        }

        binding.btStop.setOnClickListener {
            HoloApplication.INSTANCE.isStopClick.value = true
            HoloApplication.INSTANCE.isClick.postValue(true)
            HoloApplication.INSTANCE.handler.postDelayed({
                HoloApplication.INSTANCE.isClick.postValue(false)
            }, 2000)


            Toast.makeText(requireContext(), R.string.stop_success,Toast.LENGTH_SHORT).show()



            BluetoothQueueNew.addAll(ConnectionModel.status())
            activityVM.messenger?.send(Message().apply {
                what = BluetoothService.MSG_NEW_COMMAND
                obj = ConnectionModel.stop()
            })
            BluetoothQueueNew.addAllN(DataAdapter.getReadCommandByPrescription({



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



        binding.btRun.setOnClickListener {

            if(binding.etTrackType.text.isEmpty()){
                Toast.makeText(requireContext(), R.string.please_enter_track_type,Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(HoloApplication.INSTANCE.nowDeviceRun.value!!){
                Toast.makeText(requireContext(), R.string.please_stop_first,Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            BluetoothQueueNew.addAllN(DataAdapter.getReadCommandByPrescription(
                    {
                        HoloApplication.INSTANCE.currentPrescription.postValue(prescription)

            },HoloApplication.INSTANCE.prescriptionSetting.value!!, prescription))


            HoloApplication.INSTANCE.isRunClick.value = true
            HoloApplication.INSTANCE.nowShowStatus.value = ShowStatus()


            activityVM.messenger?.send(Message().apply {
                what = BluetoothService.MSG_NEW_COMMAND
                obj = ConnectionModel.run()
            })
            activityVM.messenger?.send(Message().apply {
                what = BluetoothService.MSG_NEW_COMMAND
                obj = ConnectionModel.status()
            })


            activityVM.messenger?.send(Message().apply {
                what = BluetoothService.MSG_NEW_COMMAND
                obj = ConnectionModel.check {
                    if(it){
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(),getString(R.string.start_sus),Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        HoloApplication.INSTANCE.isRunClick.postValue(false)
                    }
                }
            })
            Toast.makeText(requireContext(), R.string.run_success,Toast.LENGTH_SHORT).show()

            HoloApplication.INSTANCE.isClick.postValue(true)
            HoloApplication.INSTANCE.handler.postDelayed({
                HoloApplication.INSTANCE.isClick.postValue(false)
            }, 2000)

        }



        if(!HoloApplication.INSTANCE.currentPrescription.value!!.isPreheatingPreloading){
            binding.layoutPreloading.visibility = View.GONE
            binding.layoutPreheatingTemperature.visibility = View.GONE
            binding.layoutPreheatingSoakingTime.visibility = View.GONE
        }
        HoloApplication.INSTANCE.prescriptionSetting.value?.let {
            for(p in it){
                binding.root.findViewById<LinearLayout>(p.layoutId)
                        .visibility = View.VISIBLE
            }

            for(p in it){
                when(p.address){

                    3005 -> { // 预热气压

                        ifPreloading = true
                        val s = (p as FloatSetting)
                        binding.inputBoxPreloading.max = s.max
                        binding.inputBoxPreloading.min = s.min
                        binding.inputBoxPreloading.onHandChangeListener = {
                            save()
                            if(prescription.isPreheatingPreloading) {
                                val l = arrayListOf<SettingEntity<*>>(p)
                                activityVM.messenger?.send(Message().apply {
                                    what = BluetoothService.MSG_NEW_COMMAND
                                    obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                                })
                            }else{
                                binding.layoutPreloading.visibility = View.GONE
                            }
                        }
                    }
                    3006 -> { // 预热开关

                        binding.switchPreheatingPreloading.setOnCheckedChangeListener { _, b ->
                            prescription.isPreheatingPreloading = b
                            //binding.prescription = prescription
                            save()

                            if (b){
                                if (ifPreheatingSoakingTime){
                                    binding.layoutPreheatingSoakingTime.visibility = View.VISIBLE
                                    binding.inputBoxPreheatingSoakingTime.setNum(prescription.preheatingSoakingTime)
                                }else{
                                    binding.layoutPreheatingSoakingTime.visibility = View.GONE
                                }

                                if (ifPreheatingTemperature){
                                    binding.layoutPreheatingTemperature.visibility = View.VISIBLE
                                    binding.inputBoxPreheatingTemperature.setNum(prescription.preheatingTemperature)
                                }else{
                                    binding.layoutPreheatingTemperature.visibility = View.GONE
                                }

                                if (ifPreloading){
                                    binding.layoutPreloading.visibility = View.VISIBLE
                                    binding.inputBoxPreloading.setNum(prescription.preloading)
                                }else{
                                    binding.layoutPreloading.visibility = View.GONE
                                }
                                Log.i("SettingFragment","send switch")
                                val l = arrayListOf<SettingEntity<*>>(p,
                                        SettingEntity(0, "", 3005, 0),
                                        SettingEntity(0, "", 3008, 0),
                                        SettingEntity(0, "", 3009, 0),
                                )
                                val m = prescription
                                activityVM.messenger?.send(Message().apply {
                                    what = BluetoothService.MSG_NEW_COMMAND
                                    obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                                })
                            }else{
                                binding.layoutPreloading.visibility = View.GONE
                                binding.layoutPreheatingTemperature.visibility = View.GONE
                                binding.layoutPreheatingSoakingTime.visibility = View.GONE
                                Log.i("SettingFragment","send switch")
                                val l = arrayListOf<SettingEntity<*>>(p
                                )
                                val m = prescription
                                activityVM.messenger?.send(Message().apply {
                                    what = BluetoothService.MSG_NEW_COMMAND
                                    obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                                })
                            }
                        }
                        binding.switchPreheatingPreloading.isChecked = prescription.isPreheatingPreloading
                        if (prescription.isPreheatingPreloading){
                            if (ifPreheatingSoakingTime){
                                binding.layoutPreheatingSoakingTime.visibility = View.VISIBLE
                                binding.inputBoxPreheatingSoakingTime.setNum(prescription.preheatingSoakingTime)
                            }else{
                                binding.layoutPreheatingSoakingTime.visibility = View.GONE
                            }

                            if (ifPreheatingTemperature){
                                binding.layoutPreheatingTemperature.visibility = View.VISIBLE
                                binding.inputBoxPreheatingTemperature.setNum(prescription.preheatingTemperature)
                            }else{
                                binding.layoutPreheatingTemperature.visibility = View.GONE
                            }

                            if (ifPreloading){
                                binding.layoutPreloading.visibility = View.VISIBLE
                                binding.inputBoxPreloading.setNum(prescription.preloading)
                            }else{
                                binding.layoutPreloading.visibility = View.GONE
                            }

                        }else{
                            binding.layoutPreloading.visibility = View.GONE
                            binding.layoutPreheatingTemperature.visibility = View.GONE
                            binding.layoutPreheatingSoakingTime.visibility = View.GONE
                        }
                    }
                    3007 -> { // 气压设定
                        val s = (p as FloatSetting)
                        binding.inputBoxPressure.max = s.max
                        binding.inputBoxPressure.min = s.min
                        binding.inputBoxPressure.onHandChangeListener = {
                            save()
                            val l = arrayListOf<SettingEntity<*>>(p)
                            Log.i("SettingFragment","send")
                            activityVM.messenger?.send(Message().apply {
                                what = BluetoothService.MSG_NEW_COMMAND
                                obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                            })
                        }
                    }
                    3008 -> { // 预热温度设定值
                        val s = (p as FloatSetting)
                        ifPreheatingTemperature = true
                        binding.inputBoxPreheatingTemperature.max = s.max.toInt()
                        binding.inputBoxPreheatingTemperature.min = s.min.toInt()
                        binding.inputBoxPreheatingTemperature.onHandChangeListener = {
                            save()
                            if(prescription.isPreheatingPreloading) {

                                val l = arrayListOf<SettingEntity<*>>(p)
                                activityVM.messenger?.send(Message().apply {
                                    what = BluetoothService.MSG_NEW_COMMAND
                                    obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                                })
                            }else{
                                binding.layoutPreheatingTemperature.visibility = View.GONE
                            }
                        }
                    }
                    3009 -> { // 预热保温时间
                        ifPreheatingSoakingTime = true
                        val s = (p as IntSetting)
                        binding.inputBoxPreheatingSoakingTime.max = s.max
                        binding.inputBoxPreheatingSoakingTime.min = s.min
                        binding.inputBoxPreheatingSoakingTime.onHandChangeListener = {
                            save()
                            if(prescription.isPreheatingPreloading){

                                val l = arrayListOf<SettingEntity<*>>(p)
                                activityVM.messenger?.send(Message().apply {
                                    what = BluetoothService.MSG_NEW_COMMAND
                                    obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                                })
                            }else{
                                binding.layoutPreheatingSoakingTime.visibility = View.GONE
                            }

                        }
                    }
                    3010 -> { // 上模
                        val s = (p as FloatSetting)
                        binding.inputBoxUpModelTemperature.max = s.max.toInt()
                        binding.inputBoxUpModelTemperature.min = s.min.toInt()
                        binding.inputBoxUpModelTemperature.onHandChangeListener = {
                            save()
                            val l = arrayListOf<SettingEntity<*>>(p)
                            activityVM.messenger?.send(Message().apply {
                                what = BluetoothService.MSG_NEW_COMMAND
                                obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                            })
                        }
                    }
                    3011 -> { // 下模
                        val s = (p as FloatSetting)
                        binding.inputBoxDownModelTemperature.max = s.max.toInt()
                        binding.inputBoxDownModelTemperature.min = s.min.toInt()
                        binding.inputBoxDownModelTemperature.onHandChangeListener = {
                            save()
                            val l = arrayListOf<SettingEntity<*>>(p)
                            activityVM.messenger?.send(Message().apply {
                                what = BluetoothService.MSG_NEW_COMMAND
                                obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                            })
                        }
                    }
                    3012 -> { // 接头保温
                        val s = (p as IntSetting)
                        binding.inputBoxSoakingTime.max = s.max
                        binding.inputBoxSoakingTime.min = s.min
                        binding.inputBoxSoakingTime.onHandChangeListener = {
                            save()
                            val l = arrayListOf<SettingEntity<*>>(p)
                            activityVM.messenger?.send(Message().apply {
                                what = BluetoothService.MSG_NEW_COMMAND
                                obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                            })
                        }
                    }
                    3013 -> { // 冷却温度设定
                        val s = (p as FloatSetting)
                        binding.inputBoxCoolingTemperature.max = s.max.toInt()
                        binding.inputBoxCoolingTemperature.min = s.min.toInt()
                        binding.inputBoxCoolingTemperature.onHandChangeListener = {
                            save()
                            val l = arrayListOf<SettingEntity<*>>(p)
                            activityVM.messenger?.send(Message().apply {
                                what = BluetoothService.MSG_NEW_COMMAND
                                obj = DataAdapter.getWriteCommandByPrescription(l, prescription)
                            })
                        }
                    }
                }
            }
        }

        activityVM.nowSelect.observeWithNotify(viewLifecycleOwner){
            if(it == 2){

                requireActivity().runOnUiThread {
                    activityVM.messenger?.send(Message().apply {
                        what = BluetoothService.MSG_NEW_COMMAND
                        obj = DataAdapter.getReadCommandByPrescription({
                            runCatching {
                                requireActivity().runOnUiThread {
                                    HoloApplication.INSTANCE.currentPrescription.value = prescription
                                }
                            }
                                    .onFailure {
                                        HoloApplication.INSTANCE.handler.post {
                                            HoloApplication.INSTANCE.currentPrescription.value = prescription
                                        }

                                    }

                            HoloApplication.INSTANCE.handler.postDelayed({
                                HoloApplication.INSTANCE.currentPrescription.value = prescription
                            }, 1000)
                        }, HoloApplication.INSTANCE.prescriptionSetting.value!!, prescription)
                    })
                    isRefresh = true
                    HoloApplication.INSTANCE.handler.postDelayed({
                        isRefresh = false
                    }, 2000)
                }
            }
        }


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_setting, menu)
    }

    var isRefresh = false
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.item_refresh).setOnMenuItemClickListener {
            if(!isRefresh) {
                activityVM.messenger?.send(Message().apply {
                    what = BluetoothService.MSG_NEW_COMMAND
                    obj = DataAdapter.getReadCommandByPrescription({
                        requireActivity().runOnUiThread {
                            HoloApplication.INSTANCE.currentPrescription.value = prescription
                        }
                        HoloApplication.INSTANCE.handler.postDelayed({
                            HoloApplication.INSTANCE.currentPrescription.value = prescription
                        }, 1000)
                    }, HoloApplication.INSTANCE.prescriptionSetting.value!!, prescription)
                })
                isRefresh = true
                HoloApplication.INSTANCE.handler.postDelayed({
                                                             isRefresh = false
                }, 2000)
            }
            true
        }
    }


    fun save(){
        prescription.trackType = binding.etTrackType.text.toString()
        prescription.pressure = binding.inputBoxPressure.getNum().toFloat()
        prescription.upModelTemperature = binding.inputBoxUpModelTemperature.getNum().toFloat()
        prescription.downModelTemperature = binding.inputBoxDownModelTemperature.getNum().toFloat()
        prescription.coolingTemperature = binding.inputBoxCoolingTemperature.getNum().toFloat()
        prescription.isPreheatingPreloading = binding.switchPreheatingPreloading.isChecked
        prescription.preloading = binding.inputBoxPreloading.getNum().toFloat()
        prescription.preheatingTemperature = binding.inputBoxPreheatingTemperature.getNum().toFloat()
        prescription.preheatingSoakingTime = binding.inputBoxPreheatingSoakingTime.getNum()
    }


}
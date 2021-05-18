package com.heyanle.holo.ui.main.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityLoadReportBinding
import com.heyanle.holo.entity.Notes
import com.heyanle.holo.entity.Prescription
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.net.ReportFormBody
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.dialog.EditDialog
import com.heyanle.holo.ui.main.adapter.ReportFormAdapter
import com.heyanle.holo.ui.main.adapter.ReportFromLoadAdapter
import com.heyanle.holo.utils.ViewUtils
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by HeYanLe on 2021/5/12 19:07.
 * https://github.com/heyanLE
 */
class LoadReportFormActivity : BaseActivity() {

    companion object{
        var REQUEST_NUM = 0
        var IS_LOAD = false
    }

    val binding: ActivityLoadReportBinding by lazy{
        ActivityLoadReportBinding.inflate(LayoutInflater.from(this))
    }

    private val list = arrayListOf<ReportForm>()

    private val timeMap =  hashMapOf<Long, Boolean>()

    private val adapter: ReportFromLoadAdapter by lazy {
        ReportFromLoadAdapter(list, this)
    }

    private var messenger: Messenger? = null
    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messenger = Messenger(service)
            val map = hashMapOf<String, HashMap<String, String>>()
            val m = hashMapOf<String, String>()
            m["FNumber"] = HoloApplication.INSTANCE.deviceId.value!!
            map["Data"] = m
            HoloRetrofit.holoService.getReportForm(HoloApplication.INSTANCE.token.value!!, map)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        runCatching {

                            val t = (object : TypeToken<List<ReportFormBody>>() {}).type
                            val s = response.body()!!.string()
                            val jsonObject = JSONObject(s)
                            val jsonArray = jsonObject.getJSONArray("Data")
                            val list = Gson().fromJson<List<ReportFormBody>>(jsonArray.toString(), t)
                            val l = DataAdapter.getReportFormList(list)
                            HoloApplication.INSTANCE.reportFromList.value?.let {
                                it.clear()
                                it.addAll(l)
                                runOnUiThread {
                                    HoloApplication.INSTANCE.reportFromList.value = it
                                    load()
                                }

                            }
                        }.onFailure {
                            load()
                            it.printStackTrace()
                            //Toast.makeText(this@ReportFormActivity, "获取报表列表失败", Toast.LENGTH_SHORT).show()
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        load()
                        //isLoad = true
                        //Toast.makeText(this@ReportFormActivity, "获取报表列表失败", Toast.LENGTH_SHORT).show()
                    }
                })

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messenger?.send(Message().apply {
                what = BluetoothService.MSG_WHAT_BLUETOOTH_LISTENER_UNBIND
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun initView(){
        setSupportActionBar(binding.toolbar)
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
        binding.progress.visibility = View.VISIBLE
        showPro(0, 0)
        HoloApplication.INSTANCE.reportFromList.value?.forEach {
            timeMap[it.endTime] = true
        }
        ViewUtils.setToolbarCenter(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_chevron_left_24)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        Toast.makeText(this, R.string.load_report_without_upload_from_device, Toast.LENGTH_SHORT).show()
        
        binding.btUpload.setOnClickListener {
            val real = arrayListOf<ReportForm>()
            adapter.select.iterator().let {
                while(it.hasNext()){
                    val ent = it.next()
                    if(ent.value){
                        real.add(list[ent.key])
                    }
                }
            }
            if(real.isEmpty()){
                Toast.makeText(this, R.string.please_select_report, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Thread{
                var last = 0L
                runCatching {
                    real.forEach {
                        HoloRetrofit.holoService.uploadData(HoloApplication.INSTANCE.token.value!!,
                            DataAdapter.getReportFormBody(it)).execute().apply {
                            body()?.string()?.let { _ ->
                                last = last.coerceAtLeast(it.endTime)
                            }
                        }
                    }
                }.onSuccess {
                    HoloApplication.INSTANCE.handler.post {
                        Toast.makeText(HoloApplication.INSTANCE, HoloApplication.INSTANCE.getString(R.string.report_upload_suc),Toast.LENGTH_SHORT).show()
                        if(!HoloApplication.DEBUG)
                            SPModel.lastReportTime = SPModel.lastReportTime.coerceAtLeast(last)
                        setResult(RESULT_OK)
                        finish()
                    }

                }.onFailure {
                    HoloApplication.INSTANCE.handler.post {
                        Toast.makeText(HoloApplication.INSTANCE, HoloApplication.INSTANCE.getString(R.string.report_upload_fal),Toast.LENGTH_SHORT).show()
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }

            }.start()
        }
    }



    fun showPro(note: Int, report: Int){
        runOnUiThread {
            binding.note.text = getString(R.string.load_num_of_note, note.toString())
            binding.report.text = getString(R.string.load_num_of_report, report.toString(), REQUEST_NUM.toString())
        }
    }

    private fun load(){

        val pres: Prescription = Prescription()
        IS_LOAD = true
        BluetoothQueueNew.addAllI(DataAdapter.getReadCommandByPrescription({
            pres.trackType = HoloApplication.INSTANCE.getString(R.string.manual_starting)
            //HoloApplication.INSTANCE.currentPrescription.postValue(pres.copy())

            BluetoothQueueNew.addAllI(ConnectionModel.readHistNum { num ->
                Thread{

                    fun makeReportFrom(list: List<Notes>):ReportForm{
                        val ans = ReportForm()
                        ans.prescription = pres.copy()
                        list.forEach {
                            ans.newTem(it.upTem, it.downTem, it.time)
                            ans.newPre(it.pressure)
                            if(it.eventType == 0x00E3){
                                ans.endTime = it.time
                            }else if(it.eventType == 0x00E2){
                                ans.startTime = it.time
                            }
                        }
                        ans.deviceType = HoloApplication.INSTANCE.deviceId.value!!
                        ans.workTime = ans.endTime - ans.startTime
                        return ans
                    }

                    var numOfNote = 0
                    val buff = arrayListOf<Notes>()
                    val reList = arrayListOf<ReportForm>()
                    var i = num
                    wh@ while(i > 0){
                        val n = ConnectionModel.readAHistBlock(i, lifecycle)
                        if(!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){
                            return@Thread
                        }
                        numOfNote ++
                        showPro(numOfNote, reList.size)
                        when(n.eventType){
                            0x00E3 -> {
                                buff.clear()
                                buff.add(n)
                            }
                            0x00E2 -> {
                                buff.add(n)
                                if(buff.first().eventType == 0x00E3){
                                    val r = makeReportFrom(buff)
                                    if(timeMap[r.endTime] != true){
                                        reList.add(r)
                                        showPro(numOfNote, reList.size)
                                        if(reList.size == REQUEST_NUM){
                                            break@wh
                                        }
                                    }
                                }
                            }
                            else -> {
                                buff.add(n)
                            }
                        }


                        i --
                    }

                    runOnUiThread {
                        IS_LOAD = false
                        this@LoadReportFormActivity.list.clear()
                        this@LoadReportFormActivity.list.addAll(reList)
                        this@LoadReportFormActivity.adapter.select.clear()
                        this@LoadReportFormActivity.adapter.notifyDataSetChanged()
                        binding.progress.visibility = View.GONE
                        binding.note.visibility = View.GONE
                        binding.report.visibility = View.GONE
                    }
                }.start()

            })

        }, HoloApplication.INSTANCE.prescriptionSetting.value!!, pres))
    }


    override fun onStop() {
        IS_LOAD = false
        super.onStop()
    }
    override fun onDestroy() {
        unbindService(
            serviceConnection
        )
        IS_LOAD = false
        super.onDestroy()
    }

}
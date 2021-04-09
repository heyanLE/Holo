package com.heyanle.holo.ui.main.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityReportFormBinding
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.entity.StatusInfo
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.net.ReportFormBody
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.main.MainActivity
import com.heyanle.holo.ui.main.adapter.ReportFormAdapter
import com.heyanle.holo.utils.ViewUtils
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by HeYanLe on 2021/2/14 0014 16:42.
 * https://github.com/heyanLE
 */

class ReportFormActivity : BaseActivity(){

    private val binding: ActivityReportFormBinding by lazy{
        ActivityReportFormBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
    }

    private val list = arrayListOf<ReportForm>()
    private val adapter: ReportFormAdapter by lazy {
        ReportFormAdapter(list, this)
    }

    private fun initView(){
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        adapter.nowIndex = 0

        list.clear()
        list.addAll(HoloApplication.INSTANCE.reportFromList.value!!)
        adapter.notifyDataSetChanged()

        val map = hashMapOf<String, HashMap<String, String>>()
        val m = hashMapOf<String, String>()
        m["FNumber"] = HoloApplication.INSTANCE.deviceId.value!!
        map["Data"] = m
        HoloRetrofit.holoService.getReportForm(HoloApplication.INSTANCE.token.value!!, map)
                .enqueue(object : Callback<ResponseBody>{
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
                                HoloApplication.INSTANCE.reportFromList.postValue(it)
                            }
                        }.onFailure {
                            it.printStackTrace()
                            Toast.makeText(this@ReportFormActivity, "获取报表列表失败", Toast.LENGTH_SHORT).show()
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(this@ReportFormActivity, "获取报表列表失败", Toast.LENGTH_SHORT).show()
                    }
                })

        adapter.onLoadListener = {
            HoloApplication.INSTANCE.showReportForm = list[it]
            val intent = Intent(this, ReportFormDisplayActivity::class.java)
            startActivity(intent)
        }



        HoloApplication.INSTANCE.reportFromList.observe(this, {
            adapter.nowIndex = 0
            list.clear()
            list.addAll(HoloApplication.INSTANCE.reportFromList.value!!)
            adapter.notifyDataSetChanged()
        })

        ViewUtils.setToolbarCenter(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_chevron_left_24)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

}
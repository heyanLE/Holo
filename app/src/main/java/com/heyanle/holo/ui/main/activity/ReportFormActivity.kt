package com.heyanle.holo.ui.main.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityReportFormBinding
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.entity.StatusInfo
import com.heyanle.holo.logic.paging.ReportPaging
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.net.ReportFormBody
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.dialog.EditDialog
import com.heyanle.holo.ui.main.MainActivity
import com.heyanle.holo.ui.main.adapter.ItemLoadStateAdapter
import com.heyanle.holo.ui.main.adapter.ReportFormAdapter
import com.heyanle.holo.utils.ViewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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


    private var isLoad = false

    private val pager = Pager<Int, ReportForm>(
        PagingConfig(pageSize = 20),
        1
    ){
        ReportPaging()
    }

    private val adapter: ReportFormAdapter by lazy {
        ReportFormAdapter(this)
    }
    private val concatAdapter: ConcatAdapter by lazy {
        adapter.withLoadStateHeaderAndFooter(
            header = ItemLoadStateAdapter(adapter::retry),
            footer = ItemLoadStateAdapter(adapter::retry)
        )
    }

    private fun initView(){
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = concatAdapter

        adapter.nowIndex = 0


        adapter.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO){
            pager.flow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }


        adapter.onLoadListener = {
            HoloApplication.INSTANCE.showReportForm = it
            val intent = Intent(this, ReportFormDisplayActivity::class.java)
            startActivity(intent)
        }





        setSupportActionBar(binding.toolbar)
        ViewUtils.setToolbarCenter(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_chevron_left_24)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_report, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            it.findItem(R.id.item_load).setOnMenuItemClickListener {
                EditDialog(this).apply {
                    show()
                    binding.etMsg.setText("1")
                    binding.tvTitle.setText(R.string.please_enter_num_of_report_loaded)
                    binding.tvConfirm.setOnClickListener {
                        runCatching {
                            val i = binding.etMsg.text.toString().toInt()
                            LoadReportFormActivity.REQUEST_NUM = i
                            startActivityForResult(
                                Intent(this@ReportFormActivity,
                                    LoadReportFormActivity::class.java), 1000)
                            dismiss()
                        }.onFailure {
                            it.printStackTrace()
                            dismiss()
                            Toast.makeText(context, R.string.right, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                true
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1000){
            if (resultCode == RESULT_OK){
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
                                    isLoad = true
                                }
                            }.onFailure {
                                isLoad = true
                                it.printStackTrace()
                                Toast.makeText(this@ReportFormActivity, "获取报表列表失败", Toast.LENGTH_SHORT).show()
                            }

                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            isLoad = true
                            Toast.makeText(this@ReportFormActivity, "获取报表列表失败", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
    }

}
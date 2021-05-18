package com.heyanle.holo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityFirstBinding
import com.heyanle.holo.language.LanguageManager
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.ui.main.MainActivity
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by HeYanLe on 2021/2/6 0006 15:27.
 * https://github.com/heyanLE
 */

class FirstActivity : BaseActivity(){

    companion object{
        const val DELAY_TIME = 1000L

        var adUrl = "https://www.baidu.com"
        var adWebsite = ""
    }

    private val binding: ActivityFirstBinding by lazy {
        ActivityFirstBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding
        // inflate 完成
        setContentView(binding.root)

        if(HoloApplication.INSTANCE.connect.value!!){
            val map = hashMapOf<String, HashMap<String, String>>()
            val m = hashMapOf<String, String>()
            m["shibiehao"] = SPModel.deviceId
            m["FType"] = "${LanguageManager.nowIndex}"
            map["Data"] = m
            HoloRetrofit.holoService.machine(HoloApplication.INSTANCE.token.value!!, map).enqueue(object: retrofit2.Callback<ResponseBody>{
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    runCatching {
                        val s = response.body()!!.string()
                        val jsonObject = JSONObject(s)
                        val code = jsonObject.getInt("StatusCode")
                        if(code != 200){
                            Toast.makeText(this@FirstActivity, getString(R.string.get_device_msg_fal),
                                Toast.LENGTH_SHORT).show()
                            return
                        }
                        if(jsonObject.getString("Data") == "无数据"){

                        }else{
                            val js = jsonObject.getJSONObject("Data")
                            HoloApplication.INSTANCE.deviceId.postValue(js.getString("FNumber"))
                            HoloApplication.INSTANCE.deviceDescribe.postValue(DataAdapter.getDeviceDescribe(js))
                            HoloApplication.INSTANCE.prescriptionSetting.postValue(DataAdapter.getSettingArray(js.getJSONArray("MachingUnit")))

                            val intent = Intent(this@FirstActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }



                    }
                        .onFailure {
                            Toast.makeText(this@FirstActivity, getString(R.string.get_device_msg_fal),
                                Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val m = hashMapOf<String,HashMap<String, String>>()
                    val mm = hashMapOf<String, String>()
                    mm["FType"] = "${SPModel.china}"
                    m["Data"] = mm
                    HoloRetrofit.holoService.ad(SPModel.token, m).enqueue(object : Callback<ResponseBody>{
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            runCatching {
                                val s = response.body()!!.string()
                                val jsonObject = JSONObject(s)
                                val o = jsonObject.getJSONObject("Data")
                                adUrl = o.getString("Url")
                                adWebsite = o.getString("WebSite")
                            }
                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this@FirstActivity, ADActivity::class.java)
                                startActivity(intent)
                                finish()
                                overridePendingTransition(0, 0)
                            }, DELAY_TIME)
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this@FirstActivity, ADActivity::class.java)
                                startActivity(intent)
                                finish()
                                overridePendingTransition(0, 0)
                            }, DELAY_TIME)
                        }
                    })


                }
            })

        }else {


            val m = hashMapOf<String,HashMap<String, String>>()
            val mm = hashMapOf<String, String>()
            mm["FType"] = "${SPModel.china}"
            m["Data"] = mm
            HoloRetrofit.holoService.ad(SPModel.token, m).enqueue(object : Callback<ResponseBody>{
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    runCatching {
                        val s = response.body()!!.string()
                        val jsonObject = JSONObject(s)
                        val o = jsonObject.getJSONObject("Data")
                        adUrl = o.getString("Url")
                        adWebsite = o.getString("WebSite")
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@FirstActivity, ADActivity::class.java)
                        startActivity(intent)
                        finish()
                        overridePendingTransition(0, 0)
                    }, DELAY_TIME)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@FirstActivity, ADActivity::class.java)
                        startActivity(intent)
                        finish()
                        overridePendingTransition(0, 0)
                    }, DELAY_TIME)
                }
            })


        }
    }

}
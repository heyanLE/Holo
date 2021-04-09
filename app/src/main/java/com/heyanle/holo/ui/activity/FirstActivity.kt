package com.heyanle.holo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.databinding.ActivityFirstBinding
import com.heyanle.holo.logic.model.SPModel
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
        setContentView(binding.root)


        if(HoloApplication.INSTANCE.connect.value!!){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
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
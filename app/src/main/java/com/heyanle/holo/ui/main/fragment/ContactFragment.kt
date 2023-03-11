package com.heyanle.holo.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.FragmentContactBinding
import com.heyanle.holo.entity.MsgInfo
import com.heyanle.holo.language.LanguageManager
import com.heyanle.holo.logic.viewmodel.MainViewModel
import com.heyanle.holo.net.HoloRetrofit
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by HeYanLe on 2021/2/7 0007 14:51.
 * https://github.com/heyanLE
 */

class ContactFragment : PageFragment(R.layout.fragment_contact){

    override val title: String
    get() =
        HoloApplication.INSTANCE.getString(R.string.contact)



    private lateinit var binding: FragmentContactBinding
    private val activityVM by activityViewModels<MainViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentContactBinding.bind(v!!)
        initView()
        return v
    }


    private fun initView(){
        binding.msg = MsgInfo()
        refresh()
        activityVM.nowSelect.observe(viewLifecycleOwner){
            if(it == 3){
                refresh()
            }
        }
    }

    private fun refresh(){
        val map = hashMapOf<String, HashMap<String, String>>()
        val m = hashMapOf<String, String>()
        m["FType"] = "${LanguageManager.nowIndex}"
        map["Data"] = m
        HoloRetrofit.holoService.msg(HoloApplication.INSTANCE.token.value!!, map).enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                runCatching {
                    val s = response.body()!!.string()
                    val jsonObject = JSONObject(s)

                    val ms = Gson().fromJson(jsonObject.getJSONObject("Data").toString(), MsgInfo::class.java)
                    binding.msg = ms
                    Glide.with(requireContext()).load(ms.wechat).into(binding.we)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

            }
        })
    }

}
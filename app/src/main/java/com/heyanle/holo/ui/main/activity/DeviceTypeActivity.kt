package com.heyanle.holo.ui.main.activity

import android.content.Intent
import android.media.audiofx.BassBoost
import android.os.Bundle
import android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityDeviceTypeBinding
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.activity.ConnectActivity
import com.heyanle.holo.ui.activity.LoginActivity
import com.heyanle.holo.ui.dialog.BaseDialog
import com.heyanle.holo.ui.main.adapter.DeviceTypeItemAdapter
import com.heyanle.holo.utils.ViewUtils
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by HeYanLe on 2021/2/8 0008 21:38.
 * https://github.com/heyanLE
 */

class DeviceTypeActivity : BaseActivity(){

    private val binding: ActivityDeviceTypeBinding by lazy{
        ActivityDeviceTypeBinding.inflate(LayoutInflater.from(this))
    }

    private val list = arrayListOf<String>()
    private val finderList = arrayListOf<String>()
    private val adapter : DeviceTypeItemAdapter by lazy {
        DeviceTypeItemAdapter(list, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_chevron_left_24)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        ViewUtils.setToolbarCenter(binding.toolbar)

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        HoloRetrofit.holoService.getUserMachine(HoloApplication.INSTANCE.token.value!!).enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                runCatching {
                    val s = response.body()!!.string()
                    val jsonObject = JSONObject(s)
                    val status = jsonObject.getInt("StatusCode")

                    if(status != 200){
                        Toast.makeText(HoloApplication.INSTANCE, jsonObject.getString("Info"), Toast.LENGTH_SHORT).show()
                        return@runCatching
                    }


                    val array = jsonObject.getJSONArray("Data")
                    val stringList = arrayListOf<String>()
                    val finterL = arrayListOf<String>()
                    for(i in 0 until array.length()){
                        val o = array.getJSONObject(i)
                        stringList.add(o.getString("FNumber"))
                        finterL.add(o.getString("FInterID"))
                    }

                    runOnUiThread {
                        list.clear()
                        list.addAll(stringList)
                        finderList.clear()
                        finderList.addAll(finterL)
                        adapter.notifyDataSetChanged()
                        adapter.nowIndex = list.indexOf(HoloApplication.INSTANCE.currentType.value!!)
                        adapter.notifyDataSetChanged()
                    }
                }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(HoloApplication.INSTANCE, "获取机型列表失败",Toast.LENGTH_SHORT).show()
            }
        })


        adapter.notifyDataSetChanged()

        binding.change.setOnClickListener {
            val baseDialog = BaseDialog(this)
            baseDialog.show()
            baseDialog.binding.title.setText(R.string.point_up)
            baseDialog.binding.msg.setText(R.string.please_connect_again)
            baseDialog.binding.tvConfirm.setText(R.string.to_set)
            baseDialog.binding.tvConfirm.setOnClickListener {
                val intent = Intent(this, ConnectActivity::class.java)
                startActivity(intent)
                finish()
                baseDialog.dismiss()
            }
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
            }
        }
        binding.bind.setOnClickListener {
            val fID = finderList[adapter.nowIndex]
            val baseDialog = BaseDialog(this)
            baseDialog.show()
            baseDialog.binding.title.setText(R.string.point_up)
            baseDialog.binding.msg.setText(R.string.realy_to_bind_deviceId)
            baseDialog.binding.tvConfirm.setText(R.string.confirm)
            baseDialog.binding.tvConfirm.setOnClickListener {

                val map = hashMapOf<String, HashMap<String, String>>()
                val m = hashMapOf<String, String>()
                m["shibiehao"] = HoloApplication.INSTANCE.deviceN.value!!
                m["FInterID"] = fID
                map["Data"] = m
                HoloRetrofit.holoService.bind(HoloApplication.INSTANCE.token.value!!, map)
                        .enqueue(object: retrofit2.Callback<ResponseBody>{
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                val s = response.body()!!.string()
                                val jsonObject = JSONObject(s)
                                val code = jsonObject.getInt("StatusCode")
                                if(code != 200){
                                    Toast.makeText(HoloApplication.INSTANCE, HoloApplication.INSTANCE.getString(R.string.bind_failed),Toast.LENGTH_SHORT).show()
                                    return
                                }else{
                                    Toast.makeText(HoloApplication.INSTANCE, HoloApplication.INSTANCE.getString(R.string.bind_complete),Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Toast.makeText(HoloApplication.INSTANCE, HoloApplication.INSTANCE.getString(R.string.bind_failed),Toast.LENGTH_SHORT).show()
                            }
                        })

                baseDialog.dismiss()
            }
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
            }
        }

        adapter.onLoadListener = {
            val baseDialog = BaseDialog(this)
            baseDialog.show()
            baseDialog.binding.title.setText(R.string.point_up)
            baseDialog.binding.msg.setText(R.string.please_connect_again)
            baseDialog.binding.tvConfirm.setText(R.string.to_set)
            baseDialog.binding.tvConfirm.setOnClickListener {
                val intent = Intent(this, ConnectActivity::class.java)
                startActivity(intent)
                finish()
                baseDialog.dismiss()
            }
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
            }
        }

    }

}
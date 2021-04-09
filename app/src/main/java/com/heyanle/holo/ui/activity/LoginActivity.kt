package com.heyanle.holo.ui.activity

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityLoginBinding
import com.heyanle.holo.databinding.DialogOneBinding
import com.heyanle.holo.logic.model.SP
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.ui.dialog.OneDialog
import com.heyanle.holo.ui.main.MainActivity
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.log

/**
 * Created by HeYanLe on 2021/2/6 0006 16:02.
 * https://github.com/heyanLE
 */

class LoginActivity : BaseActivity(){

    private val binding: ActivityLoginBinding by  lazy {
        ActivityLoginBinding.inflate(LayoutInflater.from(this))
    }

    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    var locatedString by SP.string("com.heyanle.holo.LOCATED_STRING", "中国")

    var provider = LocationManager.GPS_PROVIDER

    val geocoder: Geocoder by lazy{
        Geocoder(applicationContext)
    }

    var locationListener: LocationListener = object :LocationListener{
        override fun onLocationChanged(location: Location) {
            runCatching {
                val s = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                Log.i("LoginActivity", "$s")
                locatedString = "${s[0].countryName}${s[0].adminArea}${s[0].locality}"
                binding.btLogin.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btLogin.setOnClickListener {
            login(binding.etUsername.text.toString(), binding.etPassword.text.toString())
        }

        binding.btLogin.isEnabled = false

        binding.etUsername.setText(SPModel.username)
        binding.etPassword.setText(SPModel.password)



    }

    override fun onStart() {
        super.onStart()
        ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
        val username = SPModel.username
        val password = SPModel.password
        if (username.isNotEmpty() && password.isNotEmpty()){
            login(username, password)
        }


    }

    private fun checkPermission(){
        val dialog = OneDialog(this)
        dialog.show()
        dialog.binding.tvConfirm.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
            dialog.dismiss()
        }
        dialog.binding.tvTitle.setText(R.string.point_up)
        dialog.binding.tvMsg.setText(R.string.request_located_permission)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1000) {
            if (permissions.size == 1 && grantResults.size == 1) {
                if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION) {
                    val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        if(locatedString.isNotEmpty()){
                            binding.btLogin.isEnabled = true
                        }
                        provider = if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                            LocationManager.GPS_PROVIDER
                        }else{
                            LocationManager.NETWORK_PROVIDER
                        }
                        location(provider)
                    } else {
                        checkPermission()
                    }
                }
            }
        }
    }

    private fun login(username: String, password: String){

        val map = hashMapOf<String, String>()
        val timeZone: TimeZone = TimeZone.getDefault()
        val id = timeZone.id
        val tz = timeZone.getDisplayName(false, TimeZone.SHORT)

        map["LoginName"] = username
        map["PassWord"] = password
        map["Adress"] = locatedString
        map["shiqu"] = "$id $tz"

        val m = hashMapOf<String, HashMap<String, String>>()
        m["Data"] = map
        HoloRetrofit.holoService.login(m).enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                runCatching {
                    val s = response.body()!!.string()
                    Log.i("LoginActivity", s)
                    val jsonObject = JSONObject(s)
                    if(jsonObject.getInt("StatusCode") != 200){
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, jsonObject.getString("Data"), Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }else{
                        val d = jsonObject.getJSONObject("Data")
                        val token = d.getString("Token")
                        val name = d.getString("FName")

                        HoloApplication.INSTANCE.token.postValue(token)
                        HoloApplication.INSTANCE.userTitle.postValue(name)

                        runOnUiThread {
                            SPModel.token = token
                            SPModel.username = username
                            SPModel.password = password
                            val intent: Intent = Intent(this@LoginActivity
                                    , ConnectActivity::class.java)
                            startActivity(intent)
                            finish()
                        }

                    }
                }.onFailure {
                    it.printStackTrace()
                    Toast.makeText(this@LoginActivity, it.message, Toast.LENGTH_SHORT)
                            .show()
                }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                runOnUiThread {

                    t.printStackTrace()
                    Toast.makeText(this@LoginActivity, "网络异常", Toast.LENGTH_SHORT)
                            .show()
                }

            }
        })



    }

    private fun location(provider: String){



        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission()
            return
        }
        runCatching {
            val l = locationManager.getLastKnownLocation(provider)
            if(l != null){

                    val s = geocoder.getFromLocation(l.latitude, l.longitude, 1)
                    locatedString = "${s[0].countryName}${s[0].adminArea}${s[0].locality}"
                    binding.btLogin.isEnabled = true
                    return@location
            }
        }
        locationManager.requestLocationUpdates(provider, 10000L, 10000F, locationListener)


    }

    override fun onDestroy() {
        runCatching {
            locationManager.removeUpdates(locationListener)
        }
        super.onDestroy()

    }

}
package com.heyanle.holo.net

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface HoloService {

    @POST("/api/Delete/Star")
    fun delete(@Header("Token") token: String,@Body hashMap: HashMap<String, HashMap<String,String>>):Call<ResponseBody>

    @POST("/api/Get/IndexBody")
    fun msg():Call<ResponseBody>

    @POST("/api/Get/guanggao")
    fun ad(@Header("Token") token: String,@Body hashMap: HashMap<String, HashMap<String,String>>): Call<ResponseBody>

    @POST("/api/Post/Login")
    fun login(@Body hashMap: HashMap<String, HashMap<String,String>>): Call<ResponseBody>

    @POST("/api/Post/SignOut")
    fun logout(@Body hashMap: HashMap<String, HashMap<String,String>>): Call<ResponseBody>

    @POST("/api/Get/UserMaching")
    fun getUserMachine(@Header("Token") token: String):Call<ResponseBody>

    @POST("/api/Get/MachingInfo")
    fun machine(@Header("Token") token: String, @Body hashMap: HashMap<String, HashMap<String,String>>): Call<ResponseBody>

    @POST("/api/Post/Star")
    fun star(@Header("Token") token: String,@Body hashMap: HashMap<String, StarNetBody>): Call<ResponseBody>

    @POST("/api/Get/Star")
    fun getStar(@Header("Token") token: String,@Body hashMap: HashMap<String, HashMap<String,String>>): Call<ResponseBody>

    @POST("/api/Post/UploadData")
    fun uploadData(@Header("Token") token: String,@Body hashMap: HashMap<String, ReportFormBody>): Call<ResponseBody>

    @POST("/api/Get/UploadData")
    fun getReportForm(@Header("Token") token: String, @Body hashMap: HashMap<String, HashMap<String,String>>): Call<ResponseBody>
}
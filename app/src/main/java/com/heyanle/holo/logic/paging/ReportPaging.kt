package com.heyanle.holo.logic.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.net.ReportFormBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.NullPointerException

/**
 * Created by HeYanLe on 2021/5/13 14:14.
 * https://github.com/heyanLE
 */
class ReportPaging : PagingSource<Int, ReportForm>(){

    override fun getRefreshKey(state: PagingState<Int, ReportForm>): Int? {
        return 1
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReportForm> {

        return withContext(Dispatchers.IO){
            val map = hashMapOf<String, HashMap<String, String>>()
            val m = hashMapOf<String, String>()
            m["FNumber"] = HoloApplication.INSTANCE.deviceId.value!!
            m["PageSize"] = "20"
            m["PageIndex"] = "${params.key}"
            map["Data"] = m
            val b =
                HoloRetrofit.holoService.getReportForm(HoloApplication.INSTANCE.token.value!!, map)
                    .execute().body() ?: return@withContext LoadResult.Error<Int, ReportForm>(NullPointerException())
            b?.string().let {
                val t = (object : TypeToken<List<ReportFormBody>>() {}).type
                val jsonObject = JSONObject(it)
                val jsonArray = jsonObject.getJSONArray("Data")
                val list = Gson().fromJson<List<ReportFormBody>>(jsonArray.toString(), t)
                val l = DataAdapter.getReportFormList(list)
                return@withContext LoadResult.Page<Int, ReportForm>(
                    data = l,
                    nextKey = if(l.isEmpty()) null else {
                        params.key?.plus(1)
                    },
                    prevKey = null
                )

            }
            return@withContext LoadResult.Error<Int, ReportForm>(NullPointerException())

        }
    }
}
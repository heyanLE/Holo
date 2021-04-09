package com.heyanle.holo.ui.main.fragment

import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.FragmentStatusBinding
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.logic.viewmodel.MainViewModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.view.LineChartView
import com.heyanle.holo.ui.view.NewLineChartView
import com.heyanle.holo.utils.ViewUtils
import com.heyanle.holo.utils.observeWithNotify

/**
 * Created by HeYanLe on 2021/2/7 0007 14:47.
 * https://github.com/heyanLE
 */

class StatusFragment : PageFragment(R.layout.fragment_status){

    override val title: String
        get() =
            HoloApplication.INSTANCE.getString(R.string.status)



    private lateinit var binding: FragmentStatusBinding
    private val activityVM by activityViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentStatusBinding.bind(v!!)
        initView()
        return v
    }

    private fun initView(){

        //ViewUtils.initChart(binding.lineCharNew)
        //ViewUtils.showLineChart(binding.lineCharNew, HoloApplication.INSTANCE.tem.value!!)

        HoloApplication.INSTANCE.currentPrescription.observeWithNotify(viewLifecycleOwner){
            binding.title.text = it.trackType
        }
        HoloApplication.INSTANCE.realShowStatus.observeWithNotify(viewLifecycleOwner){
            Log.i("StatusFragment", "time -> ${it.list.size}")
            binding.lineChar.list.clear()
            binding.lineChar.list.addAll(it.list)
            binding.lineChar.refresh()
            binding.lineChar.postInvalidate()
            binding.lineCharNew.invalidate()
            binding.status = it
            if(it.list.isEmpty()){
                binding.totalTime.text = "${getString(R.string.total_time)}：00:00:00"
                binding.tvWorktime.text = "00:00:00"
            }else{
                val d = it.workTime
                binding.totalTime.text = "${getString(R.string.total_time)}：${g(d/3600)}:${g((d%3600)/60)}:${g(d%60)}"
                binding.tvWorktime.text = "${g(d/3600)}:${g((d%3600)/60)}:${g(d%60)}"
            }
        }






    }

    fun g(int: Long):String{
        return if(int<10) "0${int}" else "$int"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_setting, menu)
    }

    var isRefresh = false
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.item_refresh).setOnMenuItemClickListener {
            if(!isRefresh) {
                activityVM.messenger?.send(Message().apply {
                    what = BluetoothService.MSG_NEW_COMMAND
                    obj = ConnectionModel.status()
                })
                isRefresh = true
                HoloApplication.INSTANCE.handler.postDelayed({
                    isRefresh = false
                }, 2000)
            }
            true
        }
    }

}
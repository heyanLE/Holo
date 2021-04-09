package com.heyanle.holo.ui.main.activity

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityStatusBinding
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.dialog.BaseDialog
import com.heyanle.holo.ui.main.MainActivity
import com.heyanle.holo.ui.view.LineChartView
import com.heyanle.holo.utils.ViewUtils

/**
 * Created by HeYanLe on 2021/2/14 0014 19:14.
 * https://github.com/heyanLE
 */

class ReportFormDisplayActivity : BaseActivity(){

    private val binding: ActivityStatusBinding by lazy{
        ActivityStatusBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        ViewUtils.setToolbarCenter(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_chevron_left_24)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.lineChar.list.clear()
        binding.lineChar.list.addAll(HoloApplication.INSTANCE.showReportForm.list)
        binding.lineChar.refresh()
        binding.lineChar.postInvalidate()

        binding.lineChar.isDrag = true

        binding.report = HoloApplication.INSTANCE.showReportForm
        val f = HoloApplication.INSTANCE.showReportForm

        //binding.totalTime.text = HoloApplication.INSTANCE.showReportForm.getAllTime()
        binding.tvDeviceId.text = HoloApplication.INSTANCE.deviceId.value!!

        val d = HoloApplication.INSTANCE.showReportForm.workTime
        binding.totalTime.text = "${getString(R.string.total_time)}：${g(d/3600)}:${g((d%3600)/60)}:${g(d%60)}"
        //val d = HoloApplication.INSTANCE.showReportForm.workTime
        binding.allTime.text = "${g(d/3600)}:${g((d%3600)/60)}:${g(d%60)}"
    }

    fun g(int: Long):String{
        return if(int<10) "0${int}" else "$int"
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_status_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.item_load)?.setOnMenuItemClickListener {
            val dialog = BaseDialog(this)
            dialog.show()
            dialog.binding.title.setText(R.string.point_up)
            dialog.binding.msg.setText(R.string.sure_to_load_report_form)
            dialog.binding.tvCancel.setOnClickListener {
                dialog.dismiss()
            }
            dialog.binding.tvConfirm.setOnClickListener {
                // 载入设置
                dialog.dismiss()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra(MainActivity.KEY, 1)
                startActivity(intent)
                finish()
            }
            true
        }
        return true
    }

}
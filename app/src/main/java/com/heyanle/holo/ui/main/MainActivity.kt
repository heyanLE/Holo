package com.heyanle.holo.ui.main

import android.bluetooth.BluetoothGatt
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityMainBinding
import com.heyanle.holo.entity.Notes
import com.heyanle.holo.entity.Prescription
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.entity.ShowStatus
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.logic.viewmodel.MainViewModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.activity.ConnectActivity
import com.heyanle.holo.ui.main.adapter.PagerAdapter
import com.heyanle.holo.ui.main.fragment.*
import com.heyanle.holo.utils.ViewUtils
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by HeYanLe on 2021/2/7 0007 13:37.
 * https://github.com/heyanLE
 */

class MainActivity : BaseActivity(){

    companion object{
        const val KEY = "com.heyanle.holo.mainactivity.key"
    }

    private lateinit var pagerAdapter: PagerAdapter
    private val fragmentList = arrayListOf<PageFragment>(
            StatusFragment(), StarFragment(), SettingFragment(), ContactFragment(), MyFragment()
    )
    private val tabItemId = arrayOf(
            R.id.item_status, R.id.item_star,  R.id.item_setting, R.id.item_contact, R.id.item_my)

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(LayoutInflater.from(this))
    }

    private val viewModel : MainViewModel by viewModels<MainViewModel>()

    private val handler = object: Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                BluetoothService.MSG_WHAT_BLUETOOTH_LISTENER_BIND -> {
                    if(msg.arg1 == BluetoothGatt.STATE_DISCONNECTED){
                        Toast.makeText(this@MainActivity, "连接断开，请重新连接",Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MainActivity, ConnectActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }
    private var messenger: Messenger? = null
    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messenger = Messenger(service)
            viewModel.messenger = messenger
            messenger?.send(Message().apply {
                what = BluetoothService.MSG_WHAT_BLUETOOTH_LISTENER_BIND
                replyTo = Messenger(handler)
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messenger?.send(Message().apply {
                what = BluetoothService.MSG_WHAT_BLUETOOTH_LISTENER_UNBIND
            })
        }
    }



    var isLoad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        ViewUtils.setToolbarCenter(binding.toolbar)

        HoloApplication.INSTANCE.connect.observe(this){
            if(!it){
                val intent = Intent(this, ConnectActivity::class.java)
                startActivity(intent)
                finish()
            }

        }

        HoloApplication.INSTANCE.prescriptionSetting.observe(this){ list ->
//            if(list.isEmpty()){
//                return@observe
//            }
//            if(isLoad){
//                return@observe
//            }
//            isLoad = true
//
//            // 同步报表
//            Toast.makeText(HoloApplication.INSTANCE, R.string.make_report_before,Toast.LENGTH_SHORT).show()
//
//            val pres: Prescription = Prescription()
//            BluetoothQueueNew.addAllI(DataAdapter.getReadCommandByPrescription({
//                pres.trackType = HoloApplication.INSTANCE.getString(R.string.manual_starting)
//                HoloApplication.INSTANCE.currentPrescription.postValue(pres.copy())
//
//                BluetoothQueueNew.addAll(ConnectionModel.readHistNum { num ->
//                    val buff = arrayListOf<Notes>()
//
//                    val num = AtomicInteger(num)
//                    BluetoothQueueNew.addAllI(ConnectionModel.readAHist(num.get(), object: ConnectionModel.OnReadHistListener{
//                        override fun onRead(time: Long, up: Float, down: Float, pre: Float, eventType: Int) {
//
//                            val node = Notes(time, up, down, pre, eventType)
//                            buff.add(node)
//
//                            if(time > SPModel.lastReportTime && num.get() > 1){
//                                BluetoothQueueNew.addAllI(ConnectionModel.readAHist(num.decrementAndGet(), this))
//                            }else{
//                                BluetoothQueueNew.addAllI(ConnectionModel.check {
//
//                                    // 升
//                                    buff.sortBy { note ->
//                                        note.time
//                                    }
//
//                                    val reportList = arrayListOf<ReportForm>()
//                                    var nowReportForm: ReportForm? = null
//                                    buff.forEach {
//                                        when (it.eventType) {
//                                            0x00E3 -> { // 停止
//                                                nowReportForm?.let { re ->
//                                                    re.newPre(it.pressure)
//                                                    re.newTem(it.upTem, it.downTem, it.time)
//                                                    re.endTime = it.time
//                                                    re.workTime = it.time - re.startTime
//                                                    reportList.add(re)
//                                                    nowReportForm = null
//                                                }
//                                            }
//                                            0x00E2 -> {// 开始
//                                                nowReportForm = ReportForm()
//                                                nowReportForm?.let { re ->
//                                                    re.prescription = pres.copy()
//                                                    re.deviceType = HoloApplication.INSTANCE.deviceId.value!!
//                                                    re.newPre(it.pressure)
//                                                    re.newTem(it.upTem, it.downTem, it.time)
//                                                    re.startTime = it.time
//                                                }
//                                            }
//                                            else -> {
//                                                nowReportForm?.let { re ->
//                                                    re.newPre(it.pressure)
//                                                    re.newTem(it.upTem, it.downTem, it.time)
//                                                }
//                                            }
//                                        }
//                                    }
//
//                                    HoloApplication.INSTANCE.handler.post {
//                                        Toast.makeText(HoloApplication.INSTANCE,
//                                                HoloApplication.INSTANCE.getString(R.string.make_report_before_complete,
//                                                        reportList.size.toString()),Toast.LENGTH_SHORT).show()
//
//                                        Thread{
//                                            var last = 0L
//                                            runCatching {
//                                                reportList.forEach {
//                                                    HoloRetrofit.holoService.uploadData(HoloApplication.INSTANCE.token.value!!,
//                                                            DataAdapter.getReportFormBody(it)).execute().apply {
//                                                        body()?.string()?.let { _ ->
//                                                            last = last.coerceAtLeast(it.endTime)
//                                                        }
//                                                    }
//                                                }
//                                            }.onSuccess {
//                                                HoloApplication.INSTANCE.handler.post {
//                                                    Toast.makeText(HoloApplication.INSTANCE, HoloApplication.INSTANCE.getString(R.string.report_upload_suc),Toast.LENGTH_SHORT).show()
//                                                    if(!HoloApplication.DEBUG)
//                                                    SPModel.lastReportTime = SPModel.lastReportTime.coerceAtLeast(last)
//                                                }
//                                            }.onFailure {
//                                                HoloApplication.INSTANCE.handler.post {
//                                                    Toast.makeText(HoloApplication.INSTANCE, HoloApplication.INSTANCE.getString(R.string.report_upload_fal),Toast.LENGTH_SHORT).show()
//                                                }
//                                            }
//
//                                        }.start()
//
//                                    }
//
//
//
//
//                                })
//                            }
//
//                        }
//                    }))
//
//
//
//                })
//
//            }, list, pres))




        }


        //binding.navigation.itemIconTintList = null

        pagerAdapter = PagerAdapter(this, fragmentList)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //binding.navigation.selectedItemId = tabItemId[position]
                viewModel.toolbarTitle.postValue(fragmentList[position].title)
                viewModel.nowSelect.postValue(position)
            }
        })

//        binding.navigation.setOnNavigationItemSelectedListener {
//            binding.viewPager.setCurrentItem(tabItemId.indexOf(it.itemId), true)
//            return@setOnNavigationItemSelectedListener true
//        }

        viewModel.toolbarTitle.observe(this, {
            binding.toolbar.title = it
        })

        runCatching {
            if (intent.getIntExtra(KEY, 0) == 1){
                binding.viewPager.setCurrentItem(2, true)
                viewModel.nowSelect.postValue(2)

                val p = HoloApplication.INSTANCE.showReportForm.prescription.copy()
                HoloApplication.INSTANCE.currentPrescription.postValue(p)

                //binding.navigation.selectedItemId = tabItemId[3]
            }
        }


        val b = HoloApplication.INSTANCE.prescriptionSetting.value!!


        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        testData()

        viewModel.nowSelect.observe(this){
            binding.ivStatus.setImageResource(R.drawable.status)
            binding.tvStatus.setTextColor((0xFF666666).toInt())

            binding.ivStar.setImageResource(R.drawable.star)
            binding.tvStar.setTextColor((0xFF666666).toInt())

            binding.ivSetting.setImageResource(R.drawable.setting)
            binding.tvSetting.setTextColor((0xFF666666).toInt())

            binding.ivContact.setImageResource(R.drawable.contact)
            binding.tvContact.setTextColor((0xFF666666).toInt())

            binding.ivMy.setImageResource(R.drawable.my)
            binding.tvMy.setTextColor((0xFF666666).toInt())

            when(it){
                0 -> {
                    binding.ivStatus.setImageResource(R.drawable.status_down)
                    binding.tvStatus.setTextColor((0xFFd4212b).toInt())
                }
                1->{
                    binding.ivStar.setImageResource(R.drawable.star_down)
                    binding.tvStar.setTextColor((0xFFd4212b).toInt())
                }
                2->{
                    binding.ivSetting.setImageResource(R.drawable.setting_down)
                    binding.tvSetting.setTextColor((0xFFd4212b).toInt())
                }
                3->{
                    binding.ivContact.setImageResource(R.drawable.contact_down)
                    binding.tvContact.setTextColor((0xFFd4212b).toInt())
                }
                4->{
                    binding.ivMy.setImageResource(R.drawable.my_down)
                    binding.tvMy.setTextColor((0xFFd4212b).toInt())
                }
            }

        }

        binding.layoutStatus.setOnClickListener {
            binding.viewPager.setCurrentItem(0, true)
            viewModel.nowSelect.postValue(0)
        }
        binding.layoutStar.setOnClickListener {
            binding.viewPager.setCurrentItem(1, true)
            viewModel.nowSelect.postValue(1)
        }
        binding.layoutContact.setOnClickListener {
            binding.viewPager.setCurrentItem(3, true)
            viewModel.nowSelect.postValue(3)
        }
        binding.layoutMy.setOnClickListener {
            binding.viewPager.setCurrentItem(4, true)
            viewModel.nowSelect.postValue(4)
        }
        binding.frameSetting.setOnClickListener {
            binding.viewPager.setCurrentItem(2, true)
            viewModel.nowSelect.postValue(2)
        }
        binding.navBottomSetting.setOnClickListener {
            binding.viewPager.setCurrentItem(2, true)
            viewModel.nowSelect.postValue(2)
        }
        binding.layoutSetting.setOnClickListener {
            binding.viewPager.setCurrentItem(2, true)
            viewModel.nowSelect.postValue(2)
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        runCatching {
            if (intent!!.getIntExtra(KEY, 0) == 1){
                binding.viewPager.setCurrentItem(2, true)
                viewModel.nowSelect.postValue(2)

                val p = HoloApplication.INSTANCE.showReportForm.prescription.copy()
                HoloApplication.INSTANCE.currentPrescription.postValue(p)

                //binding.navigation.selectedItemId = tabItemId[3]
            }
        }

    }
    override fun onStart() {
        super.onStart()
        binding.navBottomRelative.post {
            val h = (binding.navBottomSetting.height/97F*31F).toInt()
            val lp = binding.navBottomRelative.layoutParams as RelativeLayout.LayoutParams
            lp.topMargin = h
            binding.navBottomRelative.layoutParams = lp

        }
//        if(!HoloApplication.INSTANCE.connect.value!!){
//            val intent = Intent(this, ConnectActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }



    fun changeToSetting(){
        binding.viewPager.post {
            binding.viewPager.setCurrentItem(2, true)
            viewModel.nowSelect.postValue(2)
            //binding.navigation.selectedItemId = tabItemId[3]
        }
    }

    private fun testData(){




    }




}
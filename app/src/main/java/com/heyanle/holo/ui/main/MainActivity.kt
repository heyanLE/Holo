package com.heyanle.holo.ui.main

import android.bluetooth.BluetoothGatt
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.fragment.app.transaction
import androidx.viewpager2.widget.ViewPager2
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityMainBinding
import com.heyanle.holo.entity.Prescription
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.entity.StatusInfo
import com.heyanle.holo.logic.viewmodel.MainViewModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.activity.ConnectActivity
import com.heyanle.holo.ui.main.activity.ReportFormDisplayActivity
import com.heyanle.holo.ui.main.adapter.PagerAdapter
import com.heyanle.holo.ui.main.fragment.*
import com.heyanle.holo.ui.view.LineChartView
import com.heyanle.holo.ui.view.NewLineChartView
import com.heyanle.holo.utils.ViewUtils
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        ViewUtils.setToolbarCenter(binding.toolbar)

//        HoloApplication.INSTANCE.showReportForm = ReportForm().apply {
//            endTime = System.currentTimeMillis()
//            workTime = 43*60
//            startTime = endTime - workTime*1000
//            list.add(NewLineChartView.TemData().apply {
//                time = endTime
//                upModelTem = 60F
//                downModelTem = 40F
//            })
//            list.add(NewLineChartView.TemData().apply {
//                time = startTime
//                upModelTem = 40F
//                downModelTem = 60F
//            })
//        }
//        val intenti = Intent(this, ReportFormDisplayActivity::class.java)
//        startActivity(intenti)
//        return


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
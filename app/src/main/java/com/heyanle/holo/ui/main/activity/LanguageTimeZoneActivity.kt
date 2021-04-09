package com.heyanle.holo.ui.main.activity

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityLanguageTimeZongBinding
import com.heyanle.holo.language.LanguageManager
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.utils.ViewUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


/**
 * Created by HeYanLe on 2021/2/8 0008 17:48.
 * https://github.com/heyanLE
 */


class LanguageTimeZoneActivity : BaseActivity(){

    private val lan:ActivityResultLauncher<Intent> by lazy {
        registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            val extra = data?.getIntExtra(LanguageActivity.KEY, 0)
            extra?.let {
                binding.tvCurrentLanguage.text = LanguageManager.languageList[it]
            }
        }
    }

    private val binding: ActivityLanguageTimeZongBinding by lazy{
        ActivityLanguageTimeZongBinding.inflate(LayoutInflater.from(this))
    }

    private val handler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }
    private val runnable: Runnable by lazy {
        object: Runnable {
            override fun run() {

                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val s = format.format(System.currentTimeMillis())

                binding.tvSystemTime.text = s
                handler.postDelayed(this, 1000)


            }
        }
    }

    private var messenger: Messenger? = null
    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messenger = Messenger(service)
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.tvCurrentLanguage.text = LanguageManager.languageList[LanguageManager.nowIndex]

        val timeZone: TimeZone = TimeZone.getDefault()
        val id = timeZone.id
        val tz = timeZone.getDisplayName(false, TimeZone.SHORT)
        binding.tvCurrentTimeZone.text = "$id $tz"

        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_chevron_left_24)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        ViewUtils.setToolbarCenter(binding.toolbar)

        handler.postDelayed(runnable, 1000)

        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        lan
        binding.layoutCurrentLanguage.setOnClickListener {
            // 当前语言
            lan.launch(Intent(this, LanguageActivity::class.java))
        }
        binding.btSave.setOnClickListener {
            // 保存

            LanguageManager.nowIndex = LanguageManager.languageList.indexOf(binding.tvCurrentLanguage.text.toString())

            messenger?.send(Message().apply {
                what = BluetoothService.MSG_NEW_COMMAND
                obj = BluetoothQueueNew.WriteCommand(HoloApplication.INSTANCE.modbusRtuMaster.writeSingleRegister(
                        BluetoothService.SLAVE_ADDRESS, 3004, LanguageManager.nowIndex))
            })

            Toast.makeText(this, R.string.reboot_to_apply, Toast.LENGTH_SHORT).show()

        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        unbindService(serviceConnection)
        super.onDestroy()
    }



}
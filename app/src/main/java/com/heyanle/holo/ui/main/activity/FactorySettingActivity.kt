package com.heyanle.holo.ui.main.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityFactorySettingBinding
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.ui.dialog.EditDialog
import com.heyanle.holo.utils.ViewUtils
import com.swallowsonny.convertextlibrary.readUInt16BE
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by HeYanLe on 2021/2/15 0015 16:03.
 * https://github.com/heyanLE
 */

class FactorySettingActivity : BaseActivity(){

    private val binding: ActivityFactorySettingBinding by lazy {
        ActivityFactorySettingBinding.inflate(LayoutInflater.from(this))
    }

    private var messenger: Messenger? = null
    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messenger = Messenger(service)
            messenger?.send(Message().apply {
                what = BluetoothService.MSG_WHAT_BLUETOOTH_LISTENER_BIND
                replyTo = Messenger(Handler(Looper.getMainLooper()))
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messenger?.send(Message().apply {
                what = BluetoothService.MSG_WHAT_BLUETOOTH_LISTENER_UNBIND
            })
        }
    }

    private val handler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }
    private val runnable: Runnable by lazy {
        object: Runnable {
            override fun run() {

                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val s = format.format(System.currentTimeMillis())

                binding.time.text = s
                binding.phoneTime.text = s
                handler.postDelayed(this, 1000)


            }
        }
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

        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        HoloApplication.INSTANCE.factorySetting.value!!.deviceId = HoloApplication.INSTANCE.deviceId.value!!
        binding.fa = HoloApplication.INSTANCE.factorySetting.value
        HoloApplication.INSTANCE.factorySetting.observe(this ,{
            binding.fa = it
        })
//        binding.device.setOnClickListener {
//            val editDialog = EditDialog(this)
//            editDialog.show()
//            editDialog.binding.tvTitle.text = "修改机器编号 （0-${(1.shl(16))-1}）"
//            editDialog.binding.etMsg.setText(HoloApplication.INSTANCE.deviceId.value!!)
//            editDialog.binding.tvConfirm.setOnClickListener {
//                editDialog.dismiss()
//                if(editDialog.binding.etMsg.text.isEmpty()){
//
//                    Toast.makeText(this,"请输入合法的数字", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//                runCatching {
//                    val i = editDialog.binding.etMsg.text.toString().toInt()
//                    if(i < 0 || i > (1.shl(16))-1){
//                        Toast.makeText(this,"请输入合法的数字", Toast.LENGTH_SHORT).show()
//                        return@runCatching
//                    }
//                    val write = BluetoothQueue.WriteCommand()
//                    write.byteArray = HoloApplication.INSTANCE.modbusRtuMaster.writeSingleRegister(BluetoothService.SLAVE_ADDRESS, 3000, i)
//                    val writeRead = BluetoothQueue.WriteCommand()
//                    writeRead.byteArray = HoloApplication.INSTANCE.modbusRtuMaster.readHoldingRegister(BluetoothService.SLAVE_ADDRESS, 3000)
//                    val read = BluetoothQueue.ReadCommand()
//                    read.onResult = {
//                        val ii = it.readUInt16BE()
//                        HoloApplication.INSTANCE.deviceId.postValue("$i")
//                        runOnUiThread{
//                            Toast.makeText(this, "修改机器编号成功 ${i}", Toast.LENGTH_SHORT).show()
//                        }
//                        HoloApplication.INSTANCE.factorySetting.value?.let { f ->
//                            f.deviceId = "$i"
//                            HoloApplication.INSTANCE.factorySetting.postValue(f)
//                        }
//                    }
//                    messenger?.send(Message().apply {
//                        what = BluetoothService.MSG_NEW_COMMAND
//                        obj = arrayListOf(write, writeRead, read)
//                    })
//                    Toast.makeText(this, "发送修改指令", Toast.LENGTH_SHORT).show()
//                }.onFailure {
//                    Toast.makeText(this,"请输入合法的数字", Toast.LENGTH_SHORT).show()
//                }
//
//            }
//        }

        handler.post (runnable)
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }
}
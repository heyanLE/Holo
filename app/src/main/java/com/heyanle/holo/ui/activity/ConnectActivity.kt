package com.heyanle.holo.ui.activity

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityConnectBinding
import com.heyanle.holo.databinding.ItemBluetoothDevicesBinding
import com.heyanle.holo.entity.BusEvent
import com.heyanle.holo.entity.DeviceDescribe
import com.heyanle.holo.language.LanguageManager
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.dialog.EditDialog
import com.heyanle.holo.ui.main.MainActivity
import com.heyanle.holo.utils.ViewUtils
import com.heyanle.modbus.ByteUtil
import com.swallowsonny.convertextlibrary.readUInt16BE
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList


/**
 * Created by HeYanLe on 2021/2/6 0006 22:16.
 * https://github.com/heyanLE
 */

class ConnectActivity :BaseActivity(){

    private val binding: ActivityConnectBinding by lazy {
        ActivityConnectBinding.inflate(LayoutInflater.from(this))
    }


    private val addressMap = hashMapOf<String, String>()

    private val map = hashMapOf<String, BluetoothDevice>()
    private var bluetoothDevicesBufferEnable = false
    private val bluetoothDevices = arrayListOf<BluetoothDevice>()
    private val bluetoothDevicesBuffer = arrayListOf<BluetoothDevice>()
    private val bluetoothAdapter:BluetoothDevicesAdapter by lazy {
        BluetoothDevicesAdapter(bluetoothDevices, this, addressMap).apply {
            onItemClick = {
                runCatching {
                    dialog.dismiss()
                    messenger?.send(Message().apply {
                        what = BluetoothService.MSG_WHAT_CONNECT
                        obj = bluetoothDevices[it].address
                        replyTo = Messenger(handler)
                    })
                }
            }
            onItemLongClick = { p ->

                Toast.makeText(this@ConnectActivity, getString(R.string.delete_alias_with_empty),Toast.LENGTH_SHORT).show()
                val editDialog = EditDialog(this@ConnectActivity)
                editDialog.show()
                editDialog.binding.tvTitle.text = getString(R.string.set_device_alias)
                editDialog.binding.etMsg.setText(addressMap[bluetoothDevices[p].address]?:"")
                editDialog.binding.tvConfirm.setOnClickListener {
                    if(editDialog.binding.etMsg.text.isEmpty()){
                        addressMap.remove(bluetoothDevices[p].address)
                        val s = Gson().toJson(addressMap)
                        SPModel.addressMap = s
                        bluetoothAdapter.notifyDataSetChanged()
                        Toast.makeText(this@ConnectActivity, getString(R.string.delete_alias),Toast.LENGTH_SHORT).show()
                        editDialog.dismiss()
                    }else{
                        addressMap[bluetoothDevices[p].address] = editDialog.binding.etMsg.text.toString()
                        val s = Gson().toJson(addressMap)
                        SPModel.addressMap = s
                        Toast.makeText(this@ConnectActivity, getString(R.string.set_alias),Toast.LENGTH_SHORT).show()
                        bluetoothAdapter.notifyDataSetChanged()
                        editDialog.dismiss()
                    }
                }
                true
            }
        }
    }

    private val dialog : Dialog by lazy {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.please_choose)
            setView(
                RecyclerView(this@ConnectActivity).apply {
                    setPadding(25, 25, 25, 25)
                    layoutManager = LinearLayoutManager(this@ConnectActivity)
                    adapter = bluetoothAdapter

            })
        }.create()
    }


    private val handler = object: Handler(Looper.myLooper()!!){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                BluetoothService.MSG_WHAT_SCAN -> {
                    bluetoothDevicesBufferEnable = false
                    if(msg.arg1 == 1) {
                        map.clear()
                        dialog.show()
                        Toast.makeText(this@ConnectActivity, getString(R.string.long_touch_can_set_alias), Toast.LENGTH_SHORT).show()
                    }else{
                        dialog.dismiss()
                        Toast.makeText(this@ConnectActivity, getString(R.string.please_open_bluetooth), Toast.LENGTH_SHORT).show()
                    }
                }
                BluetoothService.MSG_WHAT_SCAN_NEW_DEVICE ->{
                    val de = msg.obj as BluetoothDevice
                    Log.i("ConnectActivity","address -> ${de.address}")


                    map[de.address] = de
                    if(bluetoothDevicesBufferEnable){

                    }else{
                        bluetoothDevices.clear()
                        bluetoothDevices.addAll(map.values)
                        bluetoothDevicesBuffer.clear()
                        bluetoothDevicesBufferEnable = true
                        postDelayed({
                                    bluetoothDevicesBufferEnable = false
                        }, 2000)
                        bluetoothAdapter.notifyDataSetChanged()
                    }




                }
                BluetoothService.MSG_WHAT_START_QUEUE -> {
                    //Toast.makeText(this@ConnectActivity, "发送获取序列号指令",Toast.LENGTH_SHORT).show()
                    val read = BluetoothQueueNew.ReadCommand(16,HoloApplication.INSTANCE.modbusRtuMaster
                            .readHoldingRegisters(BluetoothService.SLAVE_ADDRESS, 2013, 8))
                    read.onResult = {
                        val d = ByteUtil.toHexString(it)
                        runOnUiThread {

                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip: ClipData = ClipData.newPlainText("Holo 序列号", d)
                            clipboard.setPrimaryClip(clip)

                            //Toast.makeText(this@ConnectActivity, "${getString(R.string.copy_id)}：${d}",Toast.LENGTH_SHORT).show()
                        }
                        HoloApplication.INSTANCE.deviceN.postValue(d)


                        val map = hashMapOf<String, HashMap<String, String>>()
                        val m = hashMapOf<String, String>()
                        m["shibiehao"] = d
                        m["FType"] = "${LanguageManager.nowIndex}"
                        map["Data"] = m
                        HoloRetrofit.holoService.machine(HoloApplication.INSTANCE.token.value!!, map).enqueue(object: retrofit2.Callback<ResponseBody>{
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                runCatching {
                                    val s = response.body()!!.string()
                                    val jsonObject = JSONObject(s)
                                    val code = jsonObject.getInt("StatusCode")
                                    if(code != 200){
                                        Toast.makeText(this@ConnectActivity, getString(R.string.get_device_msg_fal),Toast.LENGTH_SHORT).show()
                                        return
                                    }
                                    if(jsonObject.getString("Data") == "无数据"){

                                    }else{
                                        val js = jsonObject.getJSONObject("Data")
                                        HoloApplication.INSTANCE.deviceId.postValue(js.getString("FNumber"))
                                        HoloApplication.INSTANCE.deviceDescribe.postValue(DataAdapter.getDeviceDescribe(js))
                                        HoloApplication.INSTANCE.prescriptionSetting.postValue(DataAdapter.getSettingArray(js.getJSONArray("MachingUnit")))

                                    }



                                }
                                .onFailure {
                                    Toast.makeText(this@ConnectActivity, getString(R.string.get_device_msg_fal),Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Toast.makeText(this@ConnectActivity, getString(R.string.get_device_msg_fal),Toast.LENGTH_SHORT).show()
                            }
                        })
                    }



                    messenger?.send(Message().apply {
                        what = BluetoothService.MSG_NEW_COMMAND
                        obj = arrayListOf(read as BluetoothQueueNew.Command).apply{
                            addAll(ConnectionModel.check {
                                HoloApplication.INSTANCE.nowDeviceRun.postValue(it)
                            })

                        }
                    })

                    binding.btConnect.setText(R.string.connect)
                }
                BluetoothService.MSG_WHAT_CONNECTING_MSG -> {
                    when (msg.arg1) {
                        0 -> {
                            binding.btConnect.setText(R.string.connecting)
                        }
                        1 -> {
                            binding.btConnect.setText(R.string.check_bluetooth)
                        }
                        2 -> {
                            Toast.makeText(this@ConnectActivity, getString(R.string.connect_closed), Toast.LENGTH_SHORT).show()
                            binding.btConnect.setText(R.string.enter_to_connect)
                        }
                        3 -> {
                            Toast.makeText(this@ConnectActivity, getString(R.string.conect_sus), Toast.LENGTH_SHORT).show()
                            val re = Messenger(this)
                            messenger?.send(Message().apply {
                                what = BluetoothService.MSG_WHAT_START_QUEUE
                                replyTo = re
                            })
                        }
                        4 -> {
                            Toast.makeText(this@ConnectActivity, getString(R.string.please_connect_right_device),Toast.LENGTH_SHORT).show()
                            binding.btConnect.setText(R.string.enter_to_connect)
                        }
                    }
                }
            }
        }
    }

    private var messenger:Messenger? = null
    private val serviceConnection = object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messenger = Messenger(service)
            Toast.makeText(this@ConnectActivity, R.string.bluetooth_service_run, Toast.LENGTH_SHORT)
                .show()
            binding.btConnect.setText(R.string.enter_to_connect)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if(LanguageManager.nowIndex == 1){
            HoloApplication.INSTANCE.deviceDescribe.value?.apply {
                HoloApplication.INSTANCE.deviceDescribe.value = this
            }
        }

        setSupportActionBar(binding.toolbar)
        ViewUtils.setToolbarCenter(binding.toolbar)
        //EventBus.getDefault().register(this)

        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        val s = SPModel.addressMap

        val ty = object: TypeToken<Map<String, String>>(){}.type
        val m = Gson().fromJson<Map<String, String>>(s, ty)
        addressMap.clear()
        addressMap.putAll(m)
    }

    override fun onStart() {
        super.onStart()

        if(HoloApplication.DEBUG){
            binding.btTest.visibility = View.VISIBLE
        }else{
            binding.btTest.visibility = View.GONE
        }

        binding.device = HoloApplication.INSTANCE.deviceDescribe.value
        HoloApplication.INSTANCE.deviceDescribe.observe(this){
            binding.device = it
            if(it.img.isEmpty()){
                binding.img.setImageResource(R.drawable.holo_small)
            }else{
                Glide.with(this).load(it.img).into(binding.img)
            }
        }

        binding.btTest.setOnClickListener {
            HoloApplication.INSTANCE.deviceN.postValue("123")

            val map = hashMapOf<String, HashMap<String, String>>()
            val m = hashMapOf<String, String>()
            m["shibiehao"] = "123"
            m["FType"] = "${LanguageManager.nowIndex}"
            map["Data"] = m
            HoloRetrofit.holoService.machine(HoloApplication.INSTANCE.token.value!!, map).enqueue(object: retrofit2.Callback<ResponseBody>{
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    runCatching {
                        val s = response.body()!!.string()
                        val jsonObject = JSONObject(s)
                        val code = jsonObject.getInt("StatusCode")
                        if(code != 200){
                            Toast.makeText(this@ConnectActivity, getString(R.string.get_device_msg_fal),Toast.LENGTH_SHORT).show()
                            return
                        }
                        val js = jsonObject.getJSONObject("Data")
                        HoloApplication.INSTANCE.deviceId.postValue(js.getString("FNumber"))
                        HoloApplication.INSTANCE.deviceDescribe.postValue(DataAdapter.getDeviceDescribe(js))
                        HoloApplication.INSTANCE.prescriptionSetting.postValue(DataAdapter.getSettingArray(js.getJSONArray("MachingUnit")))
                        runOnUiThread{
                            HoloApplication.INSTANCE.connect.value = true
                            val intent = Intent(this@ConnectActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }

                    }
                            .onFailure {
                                Toast.makeText(this@ConnectActivity, getString(R.string.get_device_msg_fal),Toast.LENGTH_SHORT).show()
                            }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@ConnectActivity, getString(R.string.get_device_msg_fal),Toast.LENGTH_SHORT).show()
                }
            })

        }
        binding.btConnect.setOnClickListener {
//            messenger?.send(Message().apply {
//                what = BluetoothService.MSG_TEST
//            })
//            return@setOnClickListener
            when(binding.btConnect.text){
                getString(R.string.enter_to_connect) -> {
                    messenger?.send(Message().apply {
                        what = BluetoothService.MSG_WHAT_SCAN
                        replyTo = Messenger(handler)
                    })
                }
                getString(R.string.connect) -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        HoloApplication.INSTANCE.deviceDescribe.postValue(DeviceDescribe())
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}

class BluetoothDevicesAdapter(val list: ArrayList<BluetoothDevice>,
                              val context: Context,
                              private val map: HashMap<String, String>
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    var onItemClick: (Int) ->Unit = {}
    var onItemLongClick: (Int) -> Boolean = {false}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemBluetoothDevicesBinding.inflate(LayoutInflater.from(context), parent, false)
        return object:RecyclerView.ViewHolder(binding.root){}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = ItemBluetoothDevicesBinding.bind(holder.itemView)
        binding.title.text = if(map.containsKey(list[position].address)){
            map[list[position].address]
        }else list[position].name?:list[position].address
        binding.root.setOnClickListener {
            onItemClick(position)
        }
        binding.root.setOnLongClickListener {
            onItemLongClick(position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}


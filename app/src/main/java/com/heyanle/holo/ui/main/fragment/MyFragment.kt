package com.heyanle.holo.ui.main.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.FragmentMyBinding
import com.heyanle.holo.language.LanguageManager
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.logic.model.SP
import com.heyanle.holo.logic.model.SPModel
import com.heyanle.holo.logic.viewmodel.MainViewModel
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothQueueNew
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.activity.LoginActivity
import com.heyanle.holo.ui.dialog.BaseDialog
import com.heyanle.holo.ui.main.MainActivity
import com.heyanle.holo.ui.main.activity.DeviceTypeActivity
import com.heyanle.holo.ui.main.activity.FactorySettingActivity
import com.heyanle.holo.ui.main.activity.LanguageTimeZoneActivity
import com.heyanle.holo.ui.main.activity.ReportFormActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by HeYanLe on 2021/2/7 0007 14:52.
 * https://github.com/heyanLE
 */

class MyFragment : PageFragment(R.layout.fragment_my){

    override val title: String
        get() =
            HoloApplication.INSTANCE.getString(R.string.my)

    lateinit var binding: FragmentMyBinding

    private val activityVM by activityViewModels<MainViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentMyBinding.bind(v!!)
        onShow()
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun onShow(){


        binding.tvUsername.text = HoloApplication.INSTANCE.userTitle.value
        binding.tvLanguage.text = LanguageManager.languageList[LanguageManager.nowIndex]

        binding.tvType.text = HoloApplication.INSTANCE.deviceId.value!!
        HoloApplication.INSTANCE.deviceId.observe(viewLifecycleOwner, {
            binding.tvType.text = it
        })


        binding.layoutChangeType.setOnClickListener {
            // 切换机型
            val intent = Intent(requireContext(), DeviceTypeActivity::class.java)
            startActivity(intent)

        }

        binding.layoutReport.setOnClickListener {
            // 运行报表
            val intent = Intent(requireContext(), ReportFormActivity::class.java)
            startActivity(intent)
        }
        binding.layoutLanguage.setOnClickListener {
            // 语言
            val intent = Intent(requireContext(), LanguageTimeZoneActivity::class.java)
            startActivity(intent)
        }

        binding.layoutFactorySetting.setOnClickListener {
            // 出厂设置
            val intent = Intent(requireContext(), FactorySettingActivity::class.java)
            startActivity(intent)
        }

        binding.layoutRebootSystem.setOnClickListener {
            // 重启系统
            val baseDialog = BaseDialog(requireContext())
            baseDialog.show()
            baseDialog.binding.title.setText(R.string.point_up)
            baseDialog.binding.msg.setText(R.string.sure_to_reboot)
            baseDialog.binding.tvConfirm.setOnClickListener {
                //重启

                baseDialog.dismiss()
            }
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
            }
        }

        binding.layoutClearNote.setOnClickListener {
            val baseDialog = BaseDialog(requireContext())
            baseDialog.show()
            baseDialog.binding.title.setText(R.string.point_up)
            baseDialog.binding.msg.setText(R.string.realy_to_clear)
            baseDialog.binding.tvConfirm.setOnClickListener {
                BluetoothQueueNew.WriteCommand(
                    HoloApplication.INSTANCE.modbusRtuMaster.writeSingleRegister(
                        BluetoothService.SLAVE_ADDRESS, 5030, 0x5858)
                ).let {
                    BluetoothQueueNew.add(it)
                    HoloApplication.INSTANCE.handler.post {
                        Toast.makeText(HoloApplication.INSTANCE, R.string.clear_completely, Toast.LENGTH_SHORT).show()
                    }
                }
                getNoteNum()
                baseDialog.dismiss()
            }
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
            }
        }
        binding.tvNumNote.text = getString(R.string.note_num, "N")
        getNoteNum()
        binding.btLogout.setOnClickListener {
            // 取消登录

            val baseDialog = BaseDialog(requireContext())
            baseDialog.show()
            baseDialog.binding.title.setText(R.string.point_up)
            baseDialog.binding.msg.setText(R.string.sure_to_logout)
            baseDialog.binding.tvConfirm.setOnClickListener {
                baseDialog.dismiss()
                SPModel.password = ""
                SPModel.username = ""
                HoloRetrofit.holoService.logout(HoloApplication.INSTANCE.token.value!!)
                        .enqueue(object: Callback<ResponseBody>{
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                            }
                        })
                HoloApplication.INSTANCE.token.value = ""
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()

            }
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
            }
        }
    }

    fun getNoteNum(){
        BluetoothQueueNew.addAll(
            ConnectionModel.readHistNum {
                runCatching {
                    requireActivity().runOnUiThread {
                        binding.tvNumNote.text = getString(R.string.note_num, it.toString())
                    }
                }
            }
        )

    }

}
package com.heyanle.holo.ui.main.fragment

import android.os.Bundle
import android.os.Message
import android.view.*
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.databinding.FragmentStarBinding
import com.heyanle.holo.entity.Prescription
import com.heyanle.holo.logic.model.ConnectionModel
import com.heyanle.holo.logic.viewmodel.MainViewModel
import com.heyanle.holo.net.DataAdapter
import com.heyanle.holo.net.HoloRetrofit
import com.heyanle.holo.service.BluetoothService
import com.heyanle.holo.ui.dialog.BaseDialog
import com.heyanle.holo.ui.main.MainActivity
import com.heyanle.holo.ui.main.adapter.StarItemAdapter
import com.heyanle.holo.utils.observeWithNotify
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * Created by HeYanLe on 2021/2/7 0007 14:50.
 * https://github.com/heyanLE
 */

class StarFragment : PageFragment(R.layout.fragment_star){

    override val title: String
        get() =
            HoloApplication.INSTANCE.getString(R.string.star)

    private lateinit var binding: FragmentStarBinding
    private val activityVM by activityViewModels<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_star, menu)
    }
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.item_delete).setOnMenuItemClickListener {
            if(starItemAdapter.nowIndex <0 || starItemAdapter.nowIndex >= prescriptionList.size){
                return@setOnMenuItemClickListener true
            }
            val baseDialog = BaseDialog(requireContext())
            baseDialog.show()
            baseDialog.binding.msg.text = "确认要删除吗？"
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
                starItemAdapter.notifyDataSetChanged()
            }
            baseDialog.binding.tvConfirm.setOnClickListener {
                baseDialog.dismiss()
                val ma = hashMapOf<String, HashMap<String, String>>()
                val m = hashMapOf<String, String>()
                m["FInterID"] = prescriptionList[starItemAdapter.nowIndex].unique
                ma["Data"] = m
                HoloRetrofit.holoService.delete(HoloApplication.INSTANCE.token.value!!, ma)
                        .enqueue(object : Callback<ResponseBody>{
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                requireActivity().runOnUiThread {
                                    refresh()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                requireActivity().runOnUiThread {
                                    Toast.makeText(requireContext(),"删除失败",Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
            }
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentStarBinding.bind(v!!)
        initView()
        return v
    }

    private val prescriptionList = arrayListOf<Prescription>()
    private val starItemAdapter: StarItemAdapter by lazy {
        StarItemAdapter(prescriptionList, requireContext())
    }

    private val itemTouchHelper:ItemTouchHelper by  lazy{
        ItemTouchHelper(helperCallback)
    }
    private val helperCallback = object: ItemTouchHelper.Callback(){
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val dragFlags = ItemTouchHelper.UP
            val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
            return makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val po = viewHolder.adapterPosition
            val baseDialog = BaseDialog(requireContext())
            baseDialog.show()
            baseDialog.binding.msg.text = "确认要删除吗？"
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
                starItemAdapter.notifyDataSetChanged()
            }
            baseDialog.binding.tvConfirm.setOnClickListener {
                baseDialog.dismiss()
                val ma = hashMapOf<String, HashMap<String, String>>()
                val m = hashMapOf<String, String>()
                m["FInterID"] = prescriptionList[po].unique
                ma["Data"] = m
                HoloRetrofit.holoService.delete(HoloApplication.INSTANCE.token.value!!, ma)
                        .enqueue(object : Callback<ResponseBody>{
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                requireActivity().runOnUiThread {
                                    refresh()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                requireActivity().runOnUiThread {
                                    Toast.makeText(requireContext(),"删除失败",Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
            }
        }

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }
    }

    private fun initView(){
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = starItemAdapter
        binding.recycler.isNestedScrollingEnabled = false

        //itemTouchHelper.attachToRecyclerView(binding.recycler)
        refresh()

        activityVM.nowSelect.observe(viewLifecycleOwner){
            if(it == 1){
                refresh()
            }
        }


        HoloApplication.INSTANCE.prescriptionList.observeWithNotify(viewLifecycleOwner) { it ->
            prescriptionList.clear()
            prescriptionList.addAll(it)
            starItemAdapter.nowIndex = 0
            starItemAdapter.notifyDataSetChanged()
        }

        starItemAdapter.onLoadListener = { index ->
            val baseDialog = BaseDialog(requireContext())
            baseDialog.show()
            baseDialog.binding.title.setText(R.string.point_up)
            baseDialog.binding.msg.setText(R.string.sure_to_load_this_prescription)
            baseDialog.binding.tvConfirm.setOnClickListener {
                if(HoloApplication.INSTANCE.nowDeviceRun.value!!){
                    Toast.makeText(requireContext(), R.string.please_stop_first, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                HoloApplication.INSTANCE.currentPrescription.postValue(
                        HoloApplication.INSTANCE.prescriptionList.value?.get(index)!!.copy()
                )
                activityVM.messenger?.send(Message().apply {
                    what = BluetoothService.MSG_NEW_COMMAND
                    obj = ConnectionModel.writeP()
                })
                runCatching {
                    (requireActivity()as MainActivity).changeToSetting()
                }
                baseDialog.dismiss()
            }
            baseDialog.binding.tvCancel.setOnClickListener {
                baseDialog.dismiss()
            }
        }


    }

    fun refresh(){
        val map = hashMapOf<String, HashMap<String, String>>()
        val m = hashMapOf<String, String>()
        m["shibiehao"] = HoloApplication.INSTANCE.deviceN.value!!
        map["Data"] = m
        HoloRetrofit.holoService.getStar(HoloApplication.INSTANCE.token.value!!, map).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                runCatching {
                    val s = response.body()!!.string()
                    val jsonObject = JSONObject(s)
                    val jsonArray = jsonObject.getJSONArray("Data")
                    val list = DataAdapter.getPrescriptionList(jsonArray)
                    HoloApplication.INSTANCE.prescriptionList.value?.let {
                        it.clear()
                        it.addAll(list)
                        HoloApplication.INSTANCE.prescriptionList.postValue(it)
                    }
                }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "网络异常", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        })
    }

}
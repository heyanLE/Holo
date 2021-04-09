package com.heyanle.holo.ui.main.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.heyanle.holo.databinding.ItemReportFormBinding
import com.heyanle.holo.entity.Prescription
import com.heyanle.holo.entity.ReportForm
import com.heyanle.holo.entity.StatusInfo

/**
 * Created by HeYanLe on 2021/2/14 0014 15:02.
 * https://github.com/heyanLE
 */

class ReportFormAdapter(private val list: List<ReportForm>, val context: Context)
    : RecyclerView.Adapter<ReportITemViewHolder>(){

    var nowIndex = 0
    var onLoadListener: (Int) -> Unit = {}


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportITemViewHolder {
        return ReportITemViewHolder(ItemReportFormBinding.inflate(LayoutInflater.from(context), parent,
                false))
    }

    override fun onBindViewHolder(holder: ReportITemViewHolder, position: Int) {
        holder.covert(list[position], position == nowIndex)
        holder.itemView.setOnClickListener {
            nowIndex = position
            notifyDataSetChanged()
        }
        holder.binding.tvLoad.setOnClickListener {
            onLoadListener(position)
        }

        if (position == list.size-1){
            holder.binding.view.visibility = View.GONE
        }else{
            holder.binding.view.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

class ReportITemViewHolder(val binding: ItemReportFormBinding)
    : RecyclerView.ViewHolder(binding.root){
    @SuppressLint("ClickableViewAccessibility")
    fun covert(statusInfo: ReportForm, isSelect: Boolean){
        binding.checkbox.isFocusable = false
        binding.checkbox.setOnTouchListener { _, _ ->
            false
        }
        binding.checkbox.isChecked = isSelect
        binding.tvLoad.visibility = if(isSelect) View.VISIBLE else View.GONE
        binding.report = statusInfo
    }
}
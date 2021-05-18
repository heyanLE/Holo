package com.heyanle.holo.ui.main.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.heyanle.holo.databinding.ItemReportFormBinding
import com.heyanle.holo.entity.ReportForm

/**
 * Created by HeYanLe on 2021/5/12 19:14.
 * https://github.com/heyanLE
 */
class ReportFromLoadAdapter (private val list: List<ReportForm>, val context: Context)
    : RecyclerView.Adapter<ReportFromLoadAdapter.ReportITemViewHolder>(){

    val select = hashMapOf<Int, Boolean>()



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportITemViewHolder {
        return ReportFromLoadAdapter.ReportITemViewHolder(ItemReportFormBinding.inflate(
            LayoutInflater.from(context), parent,
            false))
    }

    override fun onBindViewHolder(holder: ReportITemViewHolder, position: Int) {
        holder.covert(list[position], select[position]?:false)
        holder.itemView.setOnClickListener {
            select[position] = select[position] != true
            notifyItemChanged(position)
            //notifyDataSetChanged()
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

    class ReportITemViewHolder(val binding: ItemReportFormBinding)
        : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("ClickableViewAccessibility")
        fun covert(statusInfo: ReportForm, isSelect: Boolean){
            binding.checkbox.isFocusable = false

            binding.checkbox.setOnTouchListener { _, _ ->
                false
            }
            binding.checkbox.isChecked = isSelect
            binding.tvLoad.visibility = View.GONE
            binding.report = statusInfo
        }
    }
}


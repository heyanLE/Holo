package com.heyanle.holo.ui.main.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.heyanle.holo.databinding.ItemStarBinding
import com.heyanle.holo.entity.Prescription

/**
 * Created by HeYanLe on 2021/2/8 0008 14:57.
 * https://github.com/heyanLE
 */

class StarItemAdapter(private val list: List<Prescription>, val context: Context)
    : RecyclerView.Adapter<StarItemViewHolder>(){

    var nowIndex = 0
        set(value) {
            if(field != value){
                val old = field
                field = value
                notifyItemChanged(old)
                notifyItemChanged(value)
            }
        }
    var onLoadListener: (Int) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StarItemViewHolder {
        return StarItemViewHolder(ItemStarBinding.inflate(LayoutInflater.from(context),
                parent, false))
    }

    override fun onBindViewHolder(holder: StarItemViewHolder, position: Int) {
        holder.covert(list[position], position == nowIndex)
        holder.itemView.setOnClickListener {
            nowIndex = position
            //notifyDataSetChanged()
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

class StarItemViewHolder(val binding: ItemStarBinding)
    : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("ClickableViewAccessibility")
    fun covert(prescription: Prescription, isSelect: Boolean){
        binding.checkbox.isFocusable = false
        binding.checkbox.setOnTouchListener { _, _ ->
            false
        }
        binding.checkbox.isChecked = isSelect
        binding.tvLoad.visibility = if(isSelect) View.VISIBLE else View.GONE
        binding.tvTitle.text = prescription.trackType
    }

}
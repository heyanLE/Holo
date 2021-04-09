package com.heyanle.holo.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ViewInputBoxBinding
import com.heyanle.holo.ui.dialog.EditDialog
import kotlin.math.max

/**
 * Created by HeYanLe on 2021/2/7 0007 22:08.
 * https://github.com/heyanLE
 */

class InputBox :RelativeLayout{

    val binding : ViewInputBoxBinding by lazy {
        ViewInputBoxBinding.inflate(LayoutInflater.from(context), this, true)
    }

    constructor(context: Context?) : super(context){
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        init()
    }

    var max = -1
    var min = -1


    private var mNum = 0
    set(value) {


        val maxN = if(max == -1) value else max
        val minN = if(min == -1) value else min
        if(value > maxN){
            Toast.makeText(context, R.string.max, Toast.LENGTH_SHORT).show()
            Log.i("InputBox","Max $max Set $value")
            binding.tv.text = "d"
            return
        }

        if(value < minN){
            Toast.makeText(context, R.string.min, Toast.LENGTH_SHORT).show()
            return
        }
        field = value
        onChangeListener()
        requestFocus()
        binding.tv.text = "$value"

    }

    var onChangeListener: ()->Unit = {}
    var onHandChangeListener: ()->Unit = {}



    fun setNum(num:Int){
        mNum = num
    }

    fun setNum(num:Float){
        mNum = num.toInt()
    }

    fun getNum():Int = mNum

    private fun init(){

        binding.add.setOnClickListener {
            mNum ++
            onHandChangeListener()
        }
        binding.remove.setOnClickListener {
            mNum --
            onHandChangeListener()
        }

        binding.tv.setOnClickListener {
            val dialog = EditDialog(context)
            dialog.show()
            dialog.binding.etMsg.setText("$mNum")
            dialog.binding.tvConfirm.setOnClickListener {
                dialog.dismiss()
                val s = dialog.binding.etMsg.text.toString()
                runCatching {
                    val s = s.toInt()
                    setNum(s)
                    onHandChangeListener()
                }.onFailure {
                    Toast.makeText(context, R.string.right, Toast.LENGTH_SHORT)
                            .show()
                }

            }
        }
    }




}

class InputBoxFloat :RelativeLayout{

    val binding : ViewInputBoxBinding by lazy {
        ViewInputBoxBinding.inflate(LayoutInflater.from(context), this, true)
    }

    constructor(context: Context?) : super(context){
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        init()
    }
    var max = -1F
    var min = -1F

    var onChangeListener:() ->Unit = {}
    var onHandChangeListener: ()->Unit = {}
    private var mNum = 0.0F
        set(value) {
            val maxN = if(max == -1F) value else max
            val minN = if(min == -1F) value else min
            if(value > maxN){
                Toast.makeText(context, R.string.max, Toast.LENGTH_SHORT).show()
                Log.i("InputBox","Max $max Set $value")
                binding.tv.text = "d"
                return
            }

            if(value < minN){
                Toast.makeText(context, R.string.min, Toast.LENGTH_SHORT).show()
                return
            }

            field = ((value*100F).toInt())/100F
            onChangeListener()
            binding.tv.text = "$field"
            post {

            }
        }


    fun setNum(num: Float){
        mNum = num
    }

    fun getNum():Float = mNum

    private fun init(){

        binding.add.setOnClickListener {
            mNum= (mNum+0.01F)
            onHandChangeListener()
        }
        binding.remove.setOnClickListener {
            mNum = (mNum-0.01F)
            onHandChangeListener()
        }
        binding.tv.setOnClickListener {
            val dialog = EditDialog(context)
            dialog.show()
            dialog.binding.etMsg.setText("$mNum")
            dialog.onClickListener = {
                dialog.dismiss()
                val s = dialog.binding.etMsg.text.toString()
                runCatching {
                    val s = s.toFloat()
                    setNum(s)
                    onHandChangeListener()
                }.onFailure {
                    Toast.makeText(context, R.string.right, Toast.LENGTH_SHORT)
                            .show()
                }

            }
        }
    }



}
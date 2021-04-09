package com.heyanle.holo.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import com.heyanle.holo.databinding.DialogBaseBinding
import com.heyanle.holo.databinding.DialogEditBinding
import com.heyanle.holo.databinding.DialogOneBinding

/**
 * Created by HeYanLe on 2021/2/6 0006 21:09.
 * https://github.com/heyanLE
 */

class BaseDialog :Dialog{
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)
    constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener)

    val binding: DialogBaseBinding by lazy{
        DialogBaseBinding.inflate(LayoutInflater.from(context))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        setCancelable(false)

        context
    }

    override fun show() {
        super.show()
        window?.setBackgroundDrawableResource(android.R.color.transparent)


    }
}

class OneDialog :Dialog{
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)
    constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener)

    val binding: DialogOneBinding by lazy{
        DialogOneBinding.inflate(LayoutInflater.from(context))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        setCancelable(false)
    }

    override fun show() {
        super.show()
        window?.setBackgroundDrawableResource(android.R.color.transparent)


    }
}

class EditDialog :Dialog{
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)
    constructor(
            context: Context,
            cancelable: Boolean,
            cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener)

    val binding: DialogEditBinding by lazy{
        DialogEditBinding.inflate(LayoutInflater.from(context))
    }

    var onClickListener:()->Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        //setCanceledOnTouchOutside(false)
        //setCancelable(false)
        binding.tvConfirm.setOnClickListener {
            onClickListener()
        }
    }

    override fun show() {
        super.show()
        window?.setBackgroundDrawableResource(android.R.color.transparent)


    }
}


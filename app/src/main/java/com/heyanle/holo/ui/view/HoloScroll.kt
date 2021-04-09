package com.heyanle.holo.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView


class HoloScroll : NestedScrollView {

    var lastScrollY = 0
    override fun requestLayout() {
        lastScrollY = scrollY
        super.requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        scrollY = lastScrollY
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}
package com.heyanle.holo.ui.main.fragment

import androidx.fragment.app.Fragment

/**
 * Created by HeYanLe on 2021/2/7 0007 14:52.
 * https://github.com/heyanLE
 */
abstract class PageFragment(layoutId: Int): Fragment(layoutId) {

    abstract val title: String

}
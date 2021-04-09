package com.heyanle.holo.ui.main.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Created by HeYanLe on 2021/2/7 0007 14:57.
 * https://github.com/heyanLE
 */

class PagerAdapter(
        fragmentActivity: FragmentActivity,
        private val fragments:List<Fragment>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
package com.madness.collision.wearable.av

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

internal class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment){

    private val av: ApiFragment by lazy { ApiFragment() }
    private val settings: AvSettingsFragment by lazy { AvSettingsFragment() }

    override fun getItemCount(): Int {
        // hide settings page to work around the bug that
        // pager is triggered to swipe instead of scrolling up the list from rotary input
        return 1
    }

    override fun createFragment(position: Int): Fragment = getFragment(position)

    fun getFragment(position: Int): Fragment = when (position) {
        1 -> settings
        else -> av
    }
}

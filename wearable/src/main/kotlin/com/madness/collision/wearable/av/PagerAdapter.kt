package com.madness.collision.wearable.av

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

internal class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment){

    private val av: ApiFragment by lazy { ApiFragment() }
    private val settings: AvSettingsFragment by lazy { AvSettingsFragment() }

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            1 -> settings
            else -> av
        }
    }
}

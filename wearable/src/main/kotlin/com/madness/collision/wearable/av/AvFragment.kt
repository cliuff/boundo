package com.madness.collision.wearable.av

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.madness.collision.wearable.databinding.FragmentAvBinding

internal class AvFragment: Fragment(){
    private lateinit var pagerAdapter: PagerAdapter
    private lateinit var mViews: FragmentAvBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pagerAdapter = PagerAdapter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mViews = FragmentAvBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mViews.avPager.adapter = pagerAdapter
        mViews.avPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val fragment = pagerAdapter.getFragment(position)
                if (fragment is ApiFragment) fragment.onPageVisible()
            }
        })
    }
}

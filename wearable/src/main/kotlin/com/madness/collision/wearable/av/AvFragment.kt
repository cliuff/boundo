package com.madness.collision.wearable.av

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.madness.collision.wearable.databinding.FragmentAvBinding

internal class AvFragment: Fragment(){

    private lateinit var mViews: FragmentAvBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mViews = FragmentAvBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViews.avPager.adapter = PagerAdapter(this)
    }
}

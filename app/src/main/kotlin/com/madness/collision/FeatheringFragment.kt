package com.madness.collision

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc

class FeatheringFragment: Fragment(), Democratic {
    companion object {
        @JvmStatic
        fun newInstance() = FeatheringFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(R.layout.feature_feathering, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.run {
            val mainViewModel: MainViewModel by activityViewModels()
            democratize(mainViewModel)
        }
    }
}
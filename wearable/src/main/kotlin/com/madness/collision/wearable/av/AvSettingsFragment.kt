package com.madness.collision.wearable.av

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.madness.collision.wearable.databinding.FragmentAvSettingsBinding

internal class AvSettingsFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentAvSettingsBinding.inflate(inflater, container, false).root
    }
}

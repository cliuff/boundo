package com.madness.collision.unit.api_viewing

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.alterPadding
import com.madness.collision.util.ensureAdded
import kotlinx.android.synthetic.main.fragment_settings_av.*
import com.madness.collision.unit.api_viewing.R as MyR

internal class AvSettingsFragment : Fragment(), Democratic {

    private lateinit var mContext: Context
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var mPref: PrefAv

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.apiViewer)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        mPref = PrefAv.newInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(MyR.layout.fragment_settings_av, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ensureAdded(MyR.id.settingsAvPref, mPref)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            settingsAvPref.alterPadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            settingsAvPref.alterPadding(bottom = it)
        }
    }
}
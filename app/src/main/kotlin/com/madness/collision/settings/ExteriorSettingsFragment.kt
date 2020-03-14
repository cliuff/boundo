package com.madness.collision.settings

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
import com.madness.collision.pref.PrefExterior
import com.madness.collision.util.alterPadding
import com.madness.collision.util.ensureAdded
import kotlinx.android.synthetic.main.fragment_exterior_settings.*

internal class ExteriorSettingsFragment : Fragment(), Democratic {

    private lateinit var mContext: Context
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var mPref: PrefExterior

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.settings_exterior)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        mPref = PrefExterior.newInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_exterior_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ensureAdded(R.id.settingsExteriorPref, mPref)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            settingsExteriorPref.alterPadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            settingsExteriorPref.alterPadding(bottom = it)
        }
    }
}


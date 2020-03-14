package com.madness.collision.unit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.FragmentUnitsBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.X
import com.madness.collision.util.availableWidth
import kotlin.math.roundToInt

internal class UnitsFragment : Fragment(), Democratic {

    companion object {

        @JvmStatic
        fun newInstance() : UnitsFragment {
            return UnitsFragment()
        }
    }

    private lateinit var mViews: FragmentUnitsBinding
    private lateinit var mRecyclerView: RecyclerView

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.units)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        mViews = FragmentUnitsBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context: Context = context ?: return
        val mainViewModel: MainViewModel by activityViewModels()
        democratize(mainViewModel)

        mRecyclerView = mViews.unitsRecyclerView

        val unitWidth = X.size(context, 400f, X.DP)
        val spanCount = (availableWidth / unitWidth).roundToInt().run {
            if (this < 2) 1 else this
        }
        val mAdapter = UnitsAdapter(context, mainViewModel)
        mAdapter.spanCount = spanCount
        mRecyclerView.layoutManager = mAdapter.suggestLayoutManager(context)
        mRecyclerView.adapter = mAdapter

        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            mAdapter.topCover = it
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            mAdapter.bottomCover = it
        }
    }
}

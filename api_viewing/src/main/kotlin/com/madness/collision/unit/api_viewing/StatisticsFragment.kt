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
import com.madness.collision.R as MainR
import com.madness.collision.Democratic
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.alterPadding
import com.madness.collision.util.ensureAdded
import kotlinx.android.synthetic.main.fragment_statistics.*

internal class StatisticsFragment: Fragment(), Democratic {
    companion object {
        const val ARG_TYPE = "type"
        @JvmStatic
        fun newInstance(type: Int) = StatisticsFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, type)
            }
        }
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(if (EasyAccess.isViewingTarget) MainR.string.apiSdkTarget else MainR.string.apiSdkMin)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val unit: Int = arguments?.getInt(ARG_TYPE) ?: ApiUnit.ALL_APPS
        ensureAdded(R.id.avStatisticsContainer, ChartFragment.newInstance(unit))
        ensureAdded(R.id.avStatisticsContainer, StatsFragment.newInstance(unit))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.run {
            val mainViewModel: MainViewModel by activityViewModels()
            democratize(mainViewModel)
            mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
                avStatisticsContainer.alterPadding(top = it)
            }
            mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
                avStatisticsContainer.alterPadding(bottom = it)
            }
        }
    }

}

/*
 * Copyright 2021 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit.api_viewing.stats

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import com.madness.collision.R as MainR
import com.madness.collision.Democratic
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.databinding.FragmentStatisticsBinding
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.alterPadding
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.ensureAdded

internal class StatisticsFragment: TaggedFragment(), Democratic {

    override val category: String = "AV"
    override val id: String = "Statistics"

    companion object {
        const val ARG_TYPE = "type"

        const val STATE_KEY_HOR = "IsHor"
        const val STATE_KEY_CHART = "ChartFragment"
        const val STATE_KEY_STATS = "StatsFragment"

        @JvmStatic
        fun newInstance(type: Int) = StatisticsFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, type)
            }
        }
    }

    private lateinit var viewBinding: FragmentStatisticsBinding
    private lateinit var chartFragment: ChartFragment
    private lateinit var statsFragment: StatsFragment

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(if (EasyAccess.isViewingTarget) MainR.string.apiSdkTarget else MainR.string.apiSdkMin)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        viewBinding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isHor = viewBinding.avStatisticsContainer == null
        val unit: Int = arguments?.getInt(ARG_TYPE) ?: ApiUnit.ALL_APPS
        val fm = childFragmentManager
        var doNew = savedInstanceState == null
        if (savedInstanceState != null) {
            val fC = fm.getSavedFragment<ChartFragment>(savedInstanceState, STATE_KEY_CHART)?.also {
                chartFragment = it
            }
            val fS = fm.getSavedFragment<StatsFragment>(savedInstanceState, STATE_KEY_STATS)?.also {
                statsFragment = it
            }
            if (fC != null && fS != null) {
                val wasHor = savedInstanceState.getBoolean(STATE_KEY_HOR)
                doNew = isHor != wasHor
                if (doNew) fm.beginTransaction().remove(chartFragment).remove(statsFragment)
                        .commitNowAllowingStateLoss()
            } else {
                doNew = true
            }
        }
        if (doNew) {
            chartFragment = ChartFragment.newInstance(unit)
            statsFragment = StatsFragment.newInstance(unit)
        }
        ensureAdded(if (isHor) R.id.avStatisticsContainerPieChart else R.id.avStatisticsContainer, chartFragment)
        ensureAdded(if (isHor) R.id.avStatisticsContainerList else R.id.avStatisticsContainer, statsFragment)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.run {
            val mainViewModel: MainViewModel by activityViewModels()
            democratize(mainViewModel)
            val container = viewBinding.avStatisticsContainer ?: viewBinding.avStatisticsRoot
            mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
                container.alterPadding(top = it)
            }
            mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
                val isHor = viewBinding.avStatisticsContainer == null
                container.alterPadding(bottom = if (isHor) it else asBottomMargin(it))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save them both or neither
        if (chartFragment.isAdded && statsFragment.isAdded) {
            val isHor = viewBinding.avStatisticsContainer == null
            outState.putBoolean(STATE_KEY_HOR, isHor)
            childFragmentManager.saveFragment(outState, STATE_KEY_CHART, chartFragment)
            childFragmentManager.saveFragment(outState, STATE_KEY_STATS, statsFragment)
        }
        super.onSaveInstanceState(outState)
    }

}

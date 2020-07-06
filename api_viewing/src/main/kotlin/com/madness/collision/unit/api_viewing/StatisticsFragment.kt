/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.unit.api_viewing

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.madness.collision.R as MainR
import com.madness.collision.Democratic
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.alterPadding
import com.madness.collision.util.ensureAdded
import kotlinx.android.synthetic.main.fragment_statistics.*

internal class StatisticsFragment: TaggedFragment(), Democratic {

    override val category: String = "AV"
    override val id: String = "Statistics"

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
        val isHor = view.findViewById<View>(R.id.avStatisticsContainer) == null
        ensureAdded(if (isHor) R.id.avStatisticsContainerPieChart else R.id.avStatisticsContainer, ChartFragment.newInstance(unit))
        ensureAdded(if (isHor) R.id.avStatisticsContainerList else R.id.avStatisticsContainer, StatsFragment.newInstance(unit))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.run {
            val mainViewModel: MainViewModel by activityViewModels()
            democratize(mainViewModel)
            val container = avStatisticsContainer ?: avStatisticsRoot
            mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
                container.alterPadding(top = it)
            }
            mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
                container.alterPadding(bottom = it)
            }
        }
    }

}

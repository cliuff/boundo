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

package com.madness.collision.unit.api_viewing.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.databinding.FragmentStatsBinding
import com.madness.collision.unit.api_viewing.ui.list.AppListViewModel
import com.madness.collision.util.TaggedFragment

internal class StatsFragment : TaggedFragment() {

    override val category: String = "AV"
    override val id: String = "Stats"

    companion object {
        const val ARG_TYPE = "type"

        @JvmStatic
        fun newInstance(type: Int) = StatsFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, type)
            }
        }
    }

    private lateinit var viewBinding: FragmentStatsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentStatsBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        val unit: Int = arguments?.getInt(ARG_TYPE) ?: ApiUnit.ALL_APPS
        val viewModel = AppListViewModel.appListStats ?: return
        val stats = when (unit) {
            ApiUnit.USER -> if (EasyAccess.isViewingTarget) viewModel.apiCountUser else viewModel.minApiCountUser
            ApiUnit.SYS -> if (EasyAccess.isViewingTarget) viewModel.apiCountSystem else viewModel.minApiCountSystem
            ApiUnit.ALL_APPS -> if (EasyAccess.isViewingTarget) viewModel.apiCountAll else viewModel.minApiCountAll
            else -> null
        }
        val adapter = StatsAdapter(context)
        stats?.let { adapter.stats = it }
        viewBinding.avStatsRecycler.setHasFixedSize(true)
        viewBinding.avStatsRecycler.setItemViewCacheSize(adapter.itemCount)
        viewBinding.avStatsRecycler.adapter = adapter
        viewBinding.root.doOnLayout {
            viewBinding.avStatsRecycler.post {
                adapter.resolveSpanCount(this, 290f) { viewBinding.root.width }
                viewBinding.avStatsRecycler.layoutManager = adapter.suggestLayoutManager()
            }
        }
    }
}

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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlin.math.roundToInt
import com.madness.collision.unit.api_viewing.R as MyR

internal class StatsFragment: TaggedFragment(){

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(MyR.layout.fragment_stats, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        val adapter = StatsAdapter(context)

        val unit: Int = arguments?.getInt(ARG_TYPE) ?: ApiUnit.ALL_APPS
        val viewModel: ApiViewingViewModel by activityViewModels()
//        val (aiCountUser, aiCountSystem) = viewModel.aiCount
        val fWidth = avStatsRecycler.width
        val unitWidth = X.size(context, 500f, X.DP)
        val spanCount = (fWidth / unitWidth).roundToInt().run {
            if (this < 2) 1 else this
        }
        adapter.spanCount = spanCount
        avStatsRecycler.layoutManager = adapter.suggestLayoutManager(context)
        when (unit) {
            ApiUnit.USER -> if (EasyAccess.isViewingTarget) viewModel.apiCountUser else viewModel.minApiCountUser
            ApiUnit.SYS -> if (EasyAccess.isViewingTarget) viewModel.apiCountSystem else viewModel.minApiCountSystem
            ApiUnit.ALL_APPS -> if (EasyAccess.isViewingTarget) viewModel.apiCountAll else viewModel.minApiCountAll
            else -> null
        }?.let { adapter.stats = it }
        avStatsRecycler.setHasFixedSize(true)
        avStatsRecycler.setItemViewCacheSize(adapter.itemCount)
        avStatsRecycler.adapter = adapter
    }
}

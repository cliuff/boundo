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

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.forEach
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.MPPointF
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.ApiViewingViewModel
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.FragmentChartBinding
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import kotlin.math.roundToInt

internal class ChartFragment: TaggedFragment(){

    override val category: String = "AV"
    override val id: String = "Chart"

    companion object {
        const val ARG_TYPE = "type"

        @JvmStatic
        fun newInstance(type: Int) = ChartFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, type)
            }
        }
    }

    private lateinit var viewBinding: FragmentChartBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentChartBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        val pieChart = viewBinding.avChartPieChart

        val unit: Int = arguments?.getInt(ARG_TYPE) ?: ApiUnit.ALL_APPS
        val viewModel: ApiViewingViewModel by activityViewModels()
        val stats = when (unit) {
            ApiUnit.USER -> if (EasyAccess.isViewingTarget) viewModel.apiCountUser else viewModel.minApiCountUser
            ApiUnit.SYS -> if (EasyAccess.isViewingTarget) viewModel.apiCountSystem else viewModel.minApiCountSystem
            ApiUnit.ALL_APPS -> if (EasyAccess.isViewingTarget) viewModel.apiCountAll else viewModel.minApiCountAll
            else -> return
        }
        val chartEntries = ArrayList<PieEntry>(stats.size())
        val chartEntryColors = ArrayList<Int>(stats.size())
        val iconSize = X.size(context, 20f, X.DP).roundToInt()
        stats.forEach { key, value ->
            val apiVer = VerInfo(key, true)
//            val apiName = Utils.getAndroidCodenameByAPI(context, key)
//            val label = apiVersion + context.getString(R.string.textParentheses, apiName) // exclude apiName when entry value is small to decrease overlapping
            chartEntries.add(PieEntry(value.toFloat(), apiVer.sdk).apply {
                if (!EasyAccess.isSweet) return@apply
                val seal = SealManager.getSealForIllustration(context, apiVer.letter, iconSize)
                if (seal != null) {
                    icon = BitmapDrawable(context.resources, X.toMax(seal, iconSize))
                }
            })
            chartEntryColors.add(SealManager.getItemColorForIllustration(context, key))
        }
        val chartDataSet = PieDataSet(chartEntries, null).apply {
            colors = chartEntryColors
            // value text: the proportion value of an entry
            valueTextSize = 9f
            valueTextColor = ThemeUtil.getColor(context, R.attr.colorTextSub)
            this.sliceSpace = 2.5f
            iconsOffset = MPPointF.getInstance(0f, 28f)
            valueFormatter = object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    return String.format("%.1f", value) + " %"
                }
            }
        }
        val chartData = PieData(chartDataSet)
        pieChart.run {
            val colorOnBack = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
//            holeRadius = 40f
//            transparentCircleRadius = 45f
            setHoleColor(ThemeUtil.getColor(context, R.attr.colorABackground))
            centerText = getString(if (EasyAccess.isViewingTarget) R.string.apiSdkTarget else R.string.apiSdkMin)
            setCenterTextColor(colorOnBack)
            setEntryLabelTextSize(15f)
            setEntryLabelColor(ThemeUtil.getColor(context, R.attr.colorAOnSurface))
            // decrease overlapping
            minAngleForSlices = 13f
            setUsePercentValues(true)
//            setOnChartValueSelectedListener(object : OnChartValueSelectedListener{
//                override fun onNothingSelected() {
//                }
//
//                override fun onValueSelected(e: Entry?, h: Highlight?) {
//                }
//            })
            description = null
            legend.textColor = colorOnBack
            legend.textSize = 12f
            data = chartData
        }

    }
}

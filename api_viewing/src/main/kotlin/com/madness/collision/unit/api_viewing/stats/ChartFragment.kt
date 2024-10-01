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
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.forEach
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.MPPointF
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.FragmentChartBinding
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.unit.api_viewing.ui.list.AppListViewModel
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return

        val unit: Int = arguments?.getInt(ARG_TYPE) ?: ApiUnit.ALL_APPS
        val viewModel = AppListViewModel.appListStats ?: return
        val stats = when (unit) {
            ApiUnit.USER -> if (EasyAccess.isViewingTarget) viewModel.apiCountUser else viewModel.minApiCountUser
            ApiUnit.SYS -> if (EasyAccess.isViewingTarget) viewModel.apiCountSystem else viewModel.minApiCountSystem
            ApiUnit.ALL_APPS -> if (EasyAccess.isViewingTarget) viewModel.apiCountAll else viewModel.minApiCountAll
            else -> return
        }
        val screenWidth = X.getCurrentAppResolution(context).x
        val isSmallScreen = screenWidth < X.size(context, 360f, X.DP)
        val enList = buildList(stats.size()) {
            val iconSize = X.size(context, if (isSmallScreen) 16f else 20f, X.DP).roundToInt()
            val itemLength = X.size(context, 45f, X.DP).roundToInt()
            stats.forEach { k, v -> add(generateEntry(k, v, iconSize, itemLength, context)) }
        }
        val chartDataSet = loadDataSet(enList, isSmallScreen, context)
        configPieChart(PieData(chartDataSet), isSmallScreen)
    }

    class PieChartEntry(val value: Int, val label: String, val color: Int, val icon: Drawable?)

    private fun generateEntry(api: Int, count: Int, iconSize: Int, itemLength: Int, context: Context): PieChartEntry {
        val apiVer = VerInfo(api, isExact = true, isCompact = true)
        return PieChartEntry(
            value = count,
            label = when {
                apiVer.api == OsUtils.DEV -> "Dev"
                apiVer.sdk.isEmpty() -> "API ${apiVer.api}"
                else -> apiVer.sdk
            },
            color = SealMaker.getItemColorForIllustration(context, apiVer.api),
            icon = loadApiIcon(apiVer.letterOrDev, iconSize, itemLength, context),
        )
    }

    private fun loadApiIcon(apiLetter: Char, iconSize: Int, itemLength: Int, context: Context): Drawable? {
        try {
            kotlin.run apply@{
                SealMaker.makeSeal(context, apiLetter, itemLength) ?: return@apply
                val file = SealMaker.getSealCacheFile(apiLetter) ?: return@apply
                val bitmap = ImageUtil.getSampledBitmap(file, iconSize, iconSize) ?: return@apply
                return BitmapDrawable(context.resources, X.toMax(bitmap, iconSize))
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun loadDataSet(enList: List<PieChartEntry>, isSmallScreen: Boolean, context: Context): PieDataSet {
        val chartEntries = enList.map { en -> PieEntry(en.value.toFloat(), en.label, en.icon) }
        return PieDataSet(chartEntries, null).apply {
            colors = enList.map { en -> en.color }
            // value text: the proportion value of an entry
            valueTextSize = if (isSmallScreen) 7f else 9f
            valueTextColor = ThemeUtil.getColor(context, R.attr.colorTextSub)
            this.sliceSpace = if (isSmallScreen) 1.5f else 2.5f
            iconsOffset = MPPointF.getInstance(0f, if (isSmallScreen) 20f else 28f)
            valueFormatter = object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    val num = value.toAdapted(maxFractionDigits = 1)
                    return "$num%"
                }
            }
        }
    }

    private fun configPieChart(chartData: PieData, isSmallScreen: Boolean) = viewBinding.avChartPieChart.run {
        val colorOnBack = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
//        holeRadius = 40f
//        transparentCircleRadius = 45f
        setHoleColor(ThemeUtil.getColor(context, R.attr.colorABackground))
        centerText = getString(if (EasyAccess.isViewingTarget) R.string.apiSdkTarget else R.string.apiSdkMin)
        setCenterTextColor(colorOnBack)
        setEntryLabelTextSize(if (isSmallScreen) 9f else 15f)
        setEntryLabelColor(ThemeUtil.getColor(context, R.attr.colorAOnSurface))
        // decrease overlapping
        minAngleForSlices = if (isSmallScreen) 8f else 11f
        setUsePercentValues(true)
        description = null
        legend.textColor = colorOnBack
        legend.textSize = if (isSmallScreen) 7f else 12f
        data = chartData
    }
}

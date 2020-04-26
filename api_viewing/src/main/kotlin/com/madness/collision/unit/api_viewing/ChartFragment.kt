package com.madness.collision.unit.api_viewing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.ThemeUtil
import kotlinx.android.synthetic.main.fragment_chart.*
import com.madness.collision.unit.api_viewing.R as MyR

internal class ChartFragment: Fragment(){

    companion object {
        const val ARG_TYPE = "type"
        const val TAG = "ChartFragment"

        @JvmStatic
        fun newInstance(type: Int) = ChartFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, type)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(MyR.layout.fragment_chart, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        val pieChart = avChartPieChart

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
        stats.forEach { key, value ->
            val apiVersion = Utils.getAndroidVersionByAPI(key, true)
//            val apiName = Utils.getAndroidCodenameByAPI(context, key)
//            val label = apiVersion + context.getString(R.string.textParentheses, apiName) // exclude apiName when entry value is small to decrease overlapping
            chartEntries.add(PieEntry(value.toFloat(), apiVersion))
            chartEntryColors.add(APIAdapter.getItemColorAccent(context, key))
        }
        val chartDataSet = PieDataSet(chartEntries, null).apply {
            colors = chartEntryColors
            valueTextSize = 12f
        }
        val chartData = PieData(chartDataSet)
        pieChart.run {
//            holeRadius = 40f
//            transparentCircleRadius = 45f
            setHoleColor(ThemeUtil.getColor(context, R.attr.colorABackground))
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
            legend.textColor = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
            legend.textSize = 12f
            data = chartData
        }

    }
}

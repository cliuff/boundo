package com.madness.collision.unit.api_viewing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.forEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.alterPadding
import com.madness.collision.util.mainApplication
import com.madness.collision.util.measure
import kotlinx.android.synthetic.main.fragment_chart.*
import com.madness.collision.unit.api_viewing.R as MyR

internal class ChartFragment: DialogFragment(){

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(MyR.layout.fragment_chart, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        val pieChart = avChartPieChart
        avChartTitle.setText(if (EasyAccess.isViewingTarget) R.string.apiSdkTarget else R.string.apiSdkMin)
        avChartTitle.setTag(R.bool.avStatsTitlePaddingTop, avChartTitle.paddingTop)
        val mainViewModel: MainViewModel by activityViewModels()
        mainViewModel.insetTop.observe(viewLifecycleOwner){
            avChartTitle.alterPadding(top = avChartTitle.getTag(R.bool.avStatsTitlePaddingTop) as Int + it)
            avChartTitle.measure(shouldLimitHor = true)
            (pieChart.layoutParams as ConstraintLayout.LayoutParams).topMargin = avChartTitle.measuredHeight
        }
        mainViewModel.insetLeft.observe(viewLifecycleOwner){
            avChartRoot.alterPadding(start = it)
        }
        mainViewModel.insetRight.observe(viewLifecycleOwner){
            avChartRoot.alterPadding(end = it)
        }
        avChartRoot.alterPadding(bottom = mainApplication.insetBottom)

        dialog?.window?.let {
            SystemUtil.applyEdge2Edge(it)
            SystemUtil.applyDefaultSystemUiVisibility(context, it, mainViewModel.insetBottom.value ?: 0)
        }

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
            chartEntries.add(PieEntry(value.toFloat(), Utils.getAndroidVersionByAPI(key, true)))
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
            setUsePercentValues(true)
            description = null
            data = chartData
        }

    }

    override fun onStart() {
        super.onStart()
        if (dialog == null) return
        dialog!!.window?.run {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setWindowAnimations(R.style.AppTheme_PopUp)
        }
    }
}

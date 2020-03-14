package com.madness.collision.unit.api_viewing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.measure
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlin.math.roundToInt
import com.madness.collision.unit.api_viewing.R as MyR

internal class StatsFragment: DialogFragment(){

    companion object {
        const val ARG_TYPE = "type"
        const val TAG = "StatsFragment"

        @JvmStatic
        fun newInstance(type: Int) = StatsFragment().apply {
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
        return inflater.inflate(MyR.layout.fragment_stats, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        val adapter = StatsAdapter(context)

        avStatsTitle.setText(if (EasyAccess.isViewingTarget) R.string.apiSdkTarget else R.string.apiSdkMin)
        avStatsTitle.setTag(R.bool.avStatsTitlePaddingTop, avStatsTitle.paddingTop)
        val mainViewModel: MainViewModel by activityViewModels()
        mainViewModel.insetTop.observe(viewLifecycleOwner){
            avStatsTitle.alterPadding(top = avStatsTitle.getTag(R.bool.avStatsTitlePaddingTop) as Int + it)
            avStatsTitle.measure(shouldLimitHor = true)
            adapter.topCover = avStatsTitle.measuredHeight
        }
        mainViewModel.insetBottom.observe(viewLifecycleOwner) {
            adapter.bottomCover = it
        }
        mainViewModel.insetLeft.observe(viewLifecycleOwner){
            avStatsRoot.alterPadding(start = it)
        }
        mainViewModel.insetRight.observe(viewLifecycleOwner){
            avStatsRoot.alterPadding(end = it)
        }

        dialog?.window?.let {
            SystemUtil.applyEdge2Edge(it)
            SystemUtil.applyDefaultSystemUiVisibility(context, it, mainViewModel.insetBottom.value ?: 0)
        }

        val unit: Int = arguments?.getInt(ARG_TYPE) ?: ApiUnit.ALL_APPS
        val viewModel: ApiViewingViewModel by activityViewModels()
//        val (aiCountUser, aiCountSystem) = viewModel.aiCount
        avStatsRecycler.isNestedScrollingEnabled = false
        val unitWidth = X.size(context, 500f, X.DP)
        val spanCount = (X.getCurrentAppResolution(context).x / unitWidth).roundToInt().run {
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
        avStatsRecycler.adapter = adapter
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

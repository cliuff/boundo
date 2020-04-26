package com.madness.collision.unit.api_viewing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.X
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlin.math.roundToInt
import com.madness.collision.unit.api_viewing.R as MyR

internal class StatsFragment: Fragment(){

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
        avStatsRecycler.setHasFixedSize(true)
        avStatsRecycler.setItemViewCacheSize(adapter.itemCount)
        avStatsRecycler.adapter = adapter
    }
}

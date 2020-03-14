package com.madness.collision.unit.api_viewing

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.util.X
import com.madness.collision.util.availableWidth
import kotlinx.android.synthetic.main.av_list.*
import kotlin.math.roundToInt

internal class AppListFragment : Fragment() {

    companion object {
        private const val ARG_IS_SCROLLBAR_ENABLED = "isScrollbarEnabled"
        private const val ARG_IS_FADING_EDGE_ENABLED = "isFadingEdgeEnabled"
        private const val ARG_IS_NESTED_SCROLLING_ENABLED = "isNestedScrollingEnabled"

        fun newInstance(): AppListFragment {
            return AppListFragment()
        }

        fun newInstance(isScrollbarEnabled: Boolean, isFadingEdgeEnabled: Boolean, isNestedScrollingEnabled: Boolean): AppListFragment {
            val args = Bundle().apply {
                putBoolean(ARG_IS_SCROLLBAR_ENABLED, isScrollbarEnabled)
                putBoolean(ARG_IS_FADING_EDGE_ENABLED, isFadingEdgeEnabled)
                putBoolean(ARG_IS_NESTED_SCROLLING_ENABLED, isNestedScrollingEnabled)
            }
            return AppListFragment().apply { arguments = args }
        }
    }

    private lateinit var mContext: Context
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: APIAdapter
    private lateinit var mManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        mAdapter = APIAdapter(mContext)

        val unitWidth = X.size(mContext, 450f, X.DP)
        val spanCount = (availableWidth / unitWidth).roundToInt().run {
            if (this < 2) 1 else this
        }
        mAdapter.spanCount = spanCount
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.av_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mRecyclerView = avListRecyclerView

        arguments?.run {
            mRecyclerView.isVerticalScrollBarEnabled = getBoolean(ARG_IS_SCROLLBAR_ENABLED, mRecyclerView.isVerticalScrollBarEnabled)
            mRecyclerView.isVerticalFadingEdgeEnabled = getBoolean(ARG_IS_FADING_EDGE_ENABLED, mRecyclerView.isVerticalFadingEdgeEnabled)
            mRecyclerView.isNestedScrollingEnabled = getBoolean(ARG_IS_NESTED_SCROLLING_ENABLED, mRecyclerView.isNestedScrollingEnabled)
        }

        mManager = mAdapter.suggestLayoutManager(mContext)
        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter
    }

    fun scrollToTop() {
        mManager.scrollToPosition(0)
    }

    fun getAdapter(): APIAdapter {
        return mAdapter
    }

    fun getRecyclerView(): RecyclerView {
        return mRecyclerView
    }

    fun getLayoutManager(): RecyclerView.LayoutManager {
        return mManager
    }

    override fun onPause() {
        activity?.supportFragmentManager?.run {
            (findFragmentByTag(ApiInfoPop.TAG) as BottomSheetDialogFragment?)?.dismiss()
            (findFragmentByTag(ApiDecentFragment.TAG) as DialogFragment?)?.dismiss()
            (findFragmentByTag(AppIconFragment.TAG) as DialogFragment?)?.dismiss()
        }
        super.onPause()
    }

}

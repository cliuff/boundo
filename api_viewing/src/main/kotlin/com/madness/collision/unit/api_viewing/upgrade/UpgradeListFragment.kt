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

package com.madness.collision.unit.api_viewing.upgrade

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.databinding.AvListBinding
import com.madness.collision.unit.api_viewing.list.*
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class UpgradeListFragment : TaggedFragment(), AppList {

    override val category: String = "AV"
    override val id: String = "UpgradeList"

    companion object {
        fun newInstance(): UpgradeListFragment {
            return UpgradeListFragment()
        }
    }

    private lateinit var mContext: Context
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: APIAdapter
    private lateinit var mManager: RecyclerView.LayoutManager
    private lateinit var viewBinding: AvListBinding
    private val service = AppListService()
    private val viewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        mAdapter = UpgradeAdapter(mContext, object : APIAdapter.Listener {
            override val click: (ApiViewingApp) -> Unit = {
                AppInfoFragment(it.packageName).show(childFragmentManager, AppInfoFragment.TAG)
            }
            override val longClick: (ApiViewingApp) -> Boolean = {
                true
            }
        }, lifecycleScope)

        mAdapter.resolveSpanCount(this, 290f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = AvListBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun showAppOptions(app: ApiViewingApp) {
        service.showOptions(mContext, app, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mRecyclerView = viewBinding.avListRecyclerView.apply {
            isVerticalScrollBarEnabled = false
            isVerticalFadingEdgeEnabled = false
            isNestedScrollingEnabled = false
        }

        mManager = mAdapter.suggestLayoutManager()
        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter

        viewModel.apps4Display.observe(viewLifecycleOwner) {
            mAdapter.apps = it
        }
    }

    override fun getAdapter(): APIAdapter {
        return mAdapter
    }

    override fun getRecyclerView(): RecyclerView {
        return mRecyclerView
    }

    fun updateList(list: List<ApiViewingApp>, refreshLayout: SwipeRefreshLayout? = null) {
        if (list.isEmpty() && viewModel.apps4DisplayValue.isEmpty()) return
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.updateApps4Display(list)
        }
        updateListRes(refreshLayout)
    }

    /**
     * Update synchronously
     */
    suspend fun updateListSync(list: List<ApiViewingApp>, refreshLayout: SwipeRefreshLayout? = null) {
        if (list.isEmpty() && viewModel.apps4DisplayValue.isEmpty()) return
        withContext(Dispatchers.Main) {
            viewModel.updateApps4Display(list)
        }
        updateListRes(refreshLayout)
    }

    /**
     * Start loading icons
     */
    private fun updateListRes(refreshLayout: SwipeRefreshLayout?) {
        // use launchWhenStarted to avoid mRecyclerView not initialized bug when linking from app store
        lifecycleScope.launchWhenStarted {
            mRecyclerView.post {
                loadAppIcons(refreshLayout)
            }
        }
    }

    fun loadAppIcons(refreshLayout: SwipeRefreshLayout? = null) {
        service.loadAppIcons(this, this, refreshLayout)
    }

    override fun onPause() {
        childFragmentManager.run {
            (findFragmentByTag(AppInfoFragment.TAG) as BottomSheetDialogFragment?)?.dismiss()
        }
        super.onPause()
    }
}

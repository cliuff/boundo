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

package com.madness.collision.unit.api_viewing.list

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.madness.collision.unit.api_viewing.MyUnit
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AvListBinding
import com.madness.collision.unit.api_viewing.ui.info.AppInfoFragment
import com.madness.collision.util.*
import com.madness.collision.util.ui.appLocale
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

internal class AppListFragment : TaggedFragment(), AppList, Filterable, AppInfoFragment.Callback {

    override val category: String = "AV"
    override val id: String = "AppList"

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
    private lateinit var viewBinding: AvListBinding
    private val service = AppListService()
    private val viewModel: AppListViewModel by viewModels()
    private val popOwner = AppPopOwner()

    override fun getAppOwner(): AppInfoFragment.AppOwner {
        return object : AppInfoFragment.AppOwner {
            override val size: Int get() = viewModel.apps4DisplayValue.size

            override fun get(index: Int): ApiViewingApp? {
                return viewModel.apps4DisplayValue.getOrNull(index)
            }

            override fun getIndex(app: ApiViewingApp): Int {
                return viewModel.apps4DisplayValue.indexOf(app)
            }
        }
    }

    override fun onAppChanged(app: ApiViewingApp) {
        popOwner.updateState(app)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        mAdapter = APIAdapter(mContext, object : APIAdapter.Listener {
            override val click: (ApiViewingApp) -> Unit = {
                popOwner.pop(this@AppListFragment, it)
            }
            override val longClick: (ApiViewingApp) -> Boolean = {
                true
            }
        }, lifecycleScope)

        mAdapter.resolveSpanCount(this, 290f)
        popOwner.register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = AvListBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mRecyclerView = viewBinding.avListRecyclerView

        arguments?.run {
            mRecyclerView.isVerticalScrollBarEnabled = getBoolean(ARG_IS_SCROLLBAR_ENABLED, mRecyclerView.isVerticalScrollBarEnabled)
            mRecyclerView.isVerticalFadingEdgeEnabled = getBoolean(ARG_IS_FADING_EDGE_ENABLED, mRecyclerView.isVerticalFadingEdgeEnabled)
            mRecyclerView.isNestedScrollingEnabled = getBoolean(ARG_IS_NESTED_SCROLLING_ENABLED, mRecyclerView.isNestedScrollingEnabled)
        }

        mManager = mAdapter.suggestLayoutManager()
        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter

        viewModel.apps4Display.observe(viewLifecycleOwner) {
            mAdapter.apps = it
            timeUpdateJob?.cancel()
            if (mAdapter.getSortMethod() != MyUnit.SORT_POSITION_API_TIME) return@observe
            if (it.isNullOrEmpty()) return@observe
            timeUpdateJob = scheduleTimeUpdate()
        }
    }

    private var timeUpdateJob: Job? = null

    private fun scheduleTimeUpdate(lifecycle: Lifecycle = viewLifecycleOwner.lifecycle)
    = lifecycle.coroutineScope.launch {
        val scheduleTime = SystemClock.uptimeMillis()
        val man = mManager as LinearLayoutManager
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // delay only right after scheduling
            if (SystemClock.uptimeMillis() - scheduleTime < 500) delay(30.seconds)
            while (isActive) {
                val startIndex = man.findFirstVisibleItemPosition()
                val endIndex = man.findLastVisibleItemPosition()
                if (startIndex <= endIndex) {
                    val indexCount = endIndex - startIndex + 1
                    val payload = APIAdapter.Payload.UpdateTime
                    mAdapter.notifyItemRangeChanged(startIndex, indexCount, payload)
                    Log.d("av.list", "Updated time for $indexCount items from $startIndex")
                }
                delay(45.seconds)
            }
        }
    }

    fun scrollToTop() {
        mManager.scrollToPosition(0)
    }

    override fun getAdapter(): APIAdapter {
        return mAdapter
    }

    override fun getRecyclerView(): RecyclerView {
        return mRecyclerView
    }

    private fun updateCacheSize() {
        val manager = mManager as LinearLayoutManager
        val unitSize = manager.findLastVisibleItemPosition() - manager.findFirstVisibleItemPosition()
        updateCacheSize(unitSize)
    }

    private fun updateCacheSize(unitSize: Int) {
        val cacheSize = if (unitSize < 20) (30 + unitSize * 10) else (100 + unitSize * 7)
        EasyAccess.loadLimitHalf = cacheSize
        EasyAccess.loadAmount = unitSize
        EasyAccess.preloadLimit = EasyAccess.loadLimitHalf - EasyAccess.loadAmount
    }

    fun updateList(list: List<ApiViewingApp>, refreshLayout: SwipeRefreshLayout? = null) {
        if (list.isEmpty() && viewModel.apps4DisplayValue.isEmpty()) {
            refreshLayout ?: return
            lifecycleScope.launch(Dispatchers.Main) {
                refreshLayout.isRefreshing = false
            }
            return
        }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.updateApps4Display(list)
        }
        updateListRes(refreshLayout)
    }

    /**
     * Update synchronously
     */
    suspend fun updateListSync(list: List<ApiViewingApp>, refreshLayout: SwipeRefreshLayout? = null) {
        if (list.isEmpty() && viewModel.apps4DisplayValue.isEmpty()) {
            refreshLayout ?: return
            withContext(Dispatchers.Main) {
                refreshLayout.isRefreshing = false
            }
            return
        }
        withContext(Dispatchers.Main) {
            viewModel.updateApps4Display(list)
        }
        updateListRes(refreshLayout)
    }

    /**
     * Update list cache size and start loading icons
     */
    private fun updateListRes(refreshLayout: SwipeRefreshLayout?) {
        // use launchWhenStarted to avoid mRecyclerView not initialized bug when linking from app store
        lifecycleScope.launchWhenStarted {
            mRecyclerView.post {
                updateCacheSize()
                loadAppIcons(refreshLayout)
            }
        }
    }

    fun loadAppIcons(refreshLayout: SwipeRefreshLayout? = null) {
        service.loadAppIcons(this, this, refreshLayout)
    }

    fun clearBottomAppIcons() = lifecycleScope.launch(Dispatchers.Default) {
        try {
            var index = viewModel.apps4DisplayValue.size - 1
            val cacheSize = EasyAccess.loadLimitHalf * 2 + 10
            while (index >= cacheSize) {
                val app = viewModel.apps4DisplayValue[index]
                if (!app.preload) app.clearIcons()
                index--
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    abstract class Filter: android.widget.Filter() {
        var isAddition: Boolean = false
        abstract fun onCancel()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val appList = if (isAddition) viewModel.apps4DisplayValue
                else (viewModel.reservedApps ?: emptyList())
                val filterResults = FilterResults()
                if (appList.isEmpty()) {
                    filterResults.count = 0
                    return filterResults
                }
                val filtered: MutableList<ApiViewingApp> = mutableListOf()
                val filterText = charSequence.toString()
                val iterator: Iterator<ApiViewingApp> = appList.iterator()
                // Check store links
                val appFromStore = Utils.checkStoreLink(filterText)
                if (appFromStore != null) {
                    while (iterator.hasNext()) {
                        val info = iterator.next()
                        if (info.packageName == appFromStore) {
                            filtered.add(info)
                            break
                        }
                    }
                    filterResults.values = filtered
                    filterResults.count = filtered.size
                    return filterResults
                }
                val locale = appLocale
                val input4Comparision: String = filterText.lowercase(locale)
                while (iterator.hasNext()) {
                    val info = iterator.next()
                    val appName = info.name.replace(" ", "").lowercase(locale)
                    if (appName.contains(input4Comparision)
                            || info.packageName.lowercase(locale).contains(input4Comparision)) {
                        filtered.add(info)
                        continue
                    }
                    val ver = if (EasyAccess.isViewingTarget) VerInfo(info.targetAPI, info.targetSDK, info.targetSDKLetter)
                    else VerInfo(info.minAPI, info.minSDK, info.minSDKLetter)
                    if (filterText == ver.apiText || ver.sdk.startsWith(filterText)) {
                        filtered.add(info)
                    }
                }
                filterResults.values = filtered
                filterResults.count = filtered.size
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                val reValues = filterResults.values
                val re = if (filterResults.count != 0 && reValues is MutableList<*>) {
                    reValues.filterIsInstance<ApiViewingApp>()
                } else {
                    emptyList()
                }
                // reserve app list
                viewModel.reserveApps()
                updateList(re)
            }

            override fun onCancel() {
                viewModel.clearReserved()
            }
        }
    }

}

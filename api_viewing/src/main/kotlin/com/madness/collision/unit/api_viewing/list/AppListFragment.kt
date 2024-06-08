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
import com.madness.collision.unit.api_viewing.MyUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.databinding.AvListBinding
import com.madness.collision.unit.api_viewing.ui.info.AppInfoFragment
import com.madness.collision.util.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

internal class AppListFragment : TaggedFragment(), Filterable, AppInfoFragment.Callback {

    override val category: String = "AV"
    override val id: String = "AppList"

    private lateinit var mContext: Context
    private lateinit var mAdapter: APIAdapter
    private lateinit var mManager: RecyclerView.LayoutManager
    private lateinit var viewBinding: AvListBinding
    private val viewModel: AppListViewModel by viewModels()
    private val popOwner = AppPopOwner()

    override fun getAppOwner(): AppInfoFragment.AppOwner {
        return object : AppInfoFragment.AppOwner {
            private val appList get() = viewModel.apps4Display.value.orEmpty()
            override val size: Int get() = appList.size

            override fun get(index: Int): ApiViewingApp? {
                return appList.getOrNull(index)
            }

            override fun getIndex(app: ApiViewingApp): Int {
                return appList.indexOf(app)
            }

            override fun findInAll(pkgName: String): ApiViewingApp? {
                return appList.find { it.packageName == pkgName }
                    ?: DataMaintainer.get(mContext, viewLifecycleOwner).selectApp(pkgName)
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
        // commented out: disable re-pop
//        popOwner.register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = AvListBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mManager = mAdapter.suggestLayoutManager()
        viewBinding.avListRecyclerView.layoutManager = mManager
        viewBinding.avListRecyclerView.adapter = mAdapter

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

    fun scrollToTop() = mManager.scrollToPosition(0)
    fun getAdapter(): APIAdapter = mAdapter
    fun getRecyclerView(): RecyclerView = viewBinding.avListRecyclerView
    fun updateList(list: List<ApiViewingApp>) = viewModel.updateApps4Display(list)

    abstract class Filter: android.widget.Filter() {
        var isAddition: Boolean = false
        abstract fun onCancel()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val filtered = viewModel.filterApps(charSequence, isAddition)
                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
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
                viewModel.updateApps4Display(re)
            }

            override fun onCancel() {
                viewModel.clearReserved()
            }
        }
    }

}

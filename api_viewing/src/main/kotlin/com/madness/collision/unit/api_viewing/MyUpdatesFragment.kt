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

package com.madness.collision.unit.api_viewing

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Updatable
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

internal class MyUpdatesFragment : TaggedFragment(), Updatable {

    override val category: String = "AV"
    override val id: String = "MyUpdates"

    companion object {
        private var appTimestamp: Long = 0L
        private var sessionTimestamp: Long = 0L
        var changedPackages: List<PackageInfo>? = null

        fun checkUpdate(hostFragment: Fragment): Boolean {
            val context = hostFragment.context ?: return false
            if (hostFragment.activity == null || hostFragment.isDetached || !hostFragment.isAdded) return false
            val mainViewModel: MainViewModel by hostFragment.activityViewModels()
            val mainTimestamp = mainViewModel.timestamp
            val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            // display recent updates in last week if no history (by default)
            val lastTimestamp = prefSettings.getLong(P.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis() - 604800000)
            val shouldUpdate = (sessionTimestamp == 0L || sessionTimestamp != mainTimestamp) && lastTimestamp < mainTimestamp
            changedPackages = if (shouldUpdate) {
                Utils.getChangedPackages(context, lastTimestamp).also {
                    prefSettings.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis()) }
                }
            } else {
                Utils.getChangedPackages(context, appTimestamp)
            }
            if (shouldUpdate) {
                appTimestamp = lastTimestamp
                sessionTimestamp = mainTimestamp
            }
            return !changedPackages.isNullOrEmpty()
        }
    }

    private lateinit var mContext: Context
    private lateinit var mList: List<ApiViewingApp>
    private lateinit var mListFragment: AppListFragment
    private lateinit var mAdapter: APIAdapter

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        mList = emptyList()
        EasyAccess.init(mContext)
        mListFragment = AppListFragment.newInstance(isScrollbarEnabled = false, isFadingEdgeEnabled = false, isNestedScrollingEnabled = false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.av_updates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ensureAdded(R.id.avUpdatesRecentsListContainer, mListFragment, true)
        mAdapter = mListFragment.getAdapter()
        mAdapter.setSortMethod(MyUnit.SORT_POSITION_API_TIME)
        val space = X.size(mContext, 5f, X.DP).roundToInt()
        mAdapter.topCover = space
        mAdapter.bottomCover = space
        updateState()
    }

    override fun updateState() {
        lifecycleScope.launch(Dispatchers.Default) {
            if (changedPackages == null) {
                checkUpdate(this@MyUpdatesFragment)
            }
            val mChangedPackages = changedPackages ?: emptyList()
            val spanCount = if (activity == null || isDetached || !isAdded) 1 else mAdapter.spanCount
            val listLimitSize = min(mChangedPackages.size, 10 * spanCount)
            mList = if (mChangedPackages.isEmpty()) mList else {
                mChangedPackages.subList(0, listLimitSize).mapIndexed { index, p ->
                    val app = ApiViewingApp(mContext, p, preloadProcess = true, archive = false).load(mContext)
                    withContext(Dispatchers.Main) {
                        mAdapter.notifyListItemChanged(index)
                    }
                    app
                }.let { ApiViewingViewModel.sortList(it, MyUnit.SORT_POSITION_API_TIME) }
            }
            changedPackages = null

            withContext(Dispatchers.Main) updateUI@ {
                mAdapter.apps = mList
                val recycler = mListFragment.getRecyclerView()
                recycler.setHasFixedSize(true)
                recycler.setItemViewCacheSize(mAdapter.itemCount)
                val visibility = if (mList.isEmpty()) View.GONE else View.VISIBLE
                val view = view ?: return@updateUI
                view.findViewById<TextView>(R.id.avUpdatesRecentsTitle)?.visibility = visibility
                view.findViewById<TextView>(R.id.avUpdatesRecentsMore)?.run {
                    this.visibility = visibility
                    setOnClickListener {
                        if (activity == null || isDetached || !isAdded) return@setOnClickListener
                        val mainViewModel: MainViewModel by activityViewModels()
                        mainViewModel.displayUnit(MyBridge.unitName, shouldShowNavAfterBack = true)
                    }
                }
            }
        }
    }

}

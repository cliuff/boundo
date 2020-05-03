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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.madness.collision.main.MainViewModel
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.P
import com.madness.collision.util.X
import com.madness.collision.util.ensureAdded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

internal class MyUpdatesFragment : Fragment() {

    companion object {
        var appTimestamp: Long = 0L
        var sessionTimestamp: Long = 0L
        var changedPackages: List<PackageInfo>? = null

        fun checkUpdate(hostFragment: Fragment): Boolean {
            val context = hostFragment.context ?: return false
            val mainViewModel: MainViewModel by hostFragment.activityViewModels()
            val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            val lastTimestamp = prefSettings.getLong(P.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis())
            val shouldUpdate = (sessionTimestamp == 0L || sessionTimestamp != mainViewModel.timestamp) && lastTimestamp < mainViewModel.timestamp
            changedPackages = if (shouldUpdate) {
                MiscApp.getChangedPackages(context)
            } else {
                MiscApp.getChangedPackages(context, appTimestamp)
            }
            if (shouldUpdate) {
                appTimestamp = lastTimestamp
                sessionTimestamp = mainViewModel.timestamp
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
        GlobalScope.launch {
            mAdapter = mListFragment.getAdapter()
            mAdapter.setSortMethod(MyUnit.SORT_POSITION_API_TIME)
            val space = X.size(mContext, 5f, X.DP).roundToInt()
            mAdapter.topCover = space
            mAdapter.bottomCover = space

            if (changedPackages == null) {
                checkUpdate(this@MyUpdatesFragment)
            }
            val mChangedPackages = changedPackages!!
            mList = if (mChangedPackages.isEmpty()) mList else mChangedPackages.subList(0, min(mChangedPackages.size, 10)) .mapIndexed { index, p ->
                ApiViewingApp(mContext, p, preloadProcess = true, archive = false).setOnLoadedListener {
                    launch(Dispatchers.Main) {
                        mAdapter.notifyListItemChanged(index)
                    }
                }.load(mContext)
            }.let { ApiViewingViewModel.sortList(it, MyUnit.SORT_POSITION_API_TIME) }
            changedPackages = null

            launch(Dispatchers.Main) {
                mAdapter.apps = mList
                val recycler = mListFragment.getRecyclerView()
                recycler.setHasFixedSize(true)
                recycler.setItemViewCacheSize(mAdapter.itemCount)
                val visibility = if (mList.isEmpty()) View.GONE else View.VISIBLE
                view.findViewById<TextView>(R.id.avUpdatesRecentsTitle)?.visibility = visibility
                view.findViewById<TextView>(R.id.avUpdatesRecentsMore)?.run {
                    this.visibility = visibility
                    setOnClickListener {
                        val mainViewModel: MainViewModel by activityViewModels()
                        mainViewModel.displayUnit(MyBridge.unitName, shouldShowNavAfterBack = true)
                    }
                }
            }
        }
    }

}

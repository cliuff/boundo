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
import com.madness.collision.unit.api_viewing.database.AppMaintainer
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.origin.AppRetriever
import com.madness.collision.unit.api_viewing.upgrade.UpgradeAdapter
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.unit.api_viewing.upgrade.UpgradeComparator
import com.madness.collision.unit.api_viewing.upgrade.UpgradeListFragment
import com.madness.collision.util.*
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.math.roundToInt

internal class MyUpdatesFragment : TaggedFragment(), Updatable {

    override val category: String = "AV"
    override val id: String = "MyUpdates"

    companion object {
        const val STATE_KEY_LIST = "ListFragment"
        const val STATE_KEY_NEW_LIST = "NewListFragment"
        const val STATE_KEY_UPG_LIST = "UpgListFragment"

        private var appTimestamp: Long = 0L
        private var sessionTimestamp: Long = 0L

        /**
         * Whether showing last week history
         */
        var isNewApp = false
        var previousRecords: List<ApiViewingApp>? = null
        var changedPackages: List<PackageInfo>? = null

        fun isNewSession(mainTimestamp: Long): Boolean {
            return sessionTimestamp == 0L || sessionTimestamp != mainTimestamp
        }

        fun checkUpdate(hostFragment: Fragment): Boolean {
            val context = hostFragment.context ?: return false
            if (hostFragment.activity == null || hostFragment.isDetached || !hostFragment.isAdded) return false
            val mainViewModel: MainViewModel by hostFragment.activityViewModels()
            val mainTimestamp = mainViewModel.timestamp
            val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            var lastTimestamp = prefSettings.getLong(P.PACKAGE_CHANGED_TIMESTAMP, -1)
            isNewApp = lastTimestamp == -1L
            // display recent updates in last week if no history (by default)
            if (isNewApp) lastTimestamp = System.currentTimeMillis() - 604800000
            val isValidSession = lastTimestamp < mainTimestamp
            if (isNewSession(mainTimestamp) && isValidSession) {
                Utils.getChangedPackages(context, lastTimestamp).let {
                    previousRecords = it.first
                    changedPackages = it.second
                }
                prefSettings.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis()) }
                appTimestamp = lastTimestamp
                sessionTimestamp = mainTimestamp
            } else {
                Utils.getChangedPackages(context, appTimestamp).let {
                    previousRecords = it.first
                    changedPackages = it.second
                }
            }
            return !changedPackages.isNullOrEmpty()
        }
    }

    private lateinit var mContext: Context
    private lateinit var newList: List<ApiViewingApp>
    private lateinit var newListFragment: AppListFragment
    private lateinit var newAdapter: APIAdapter
    private lateinit var upgradeList: List<Upgrade>
    private lateinit var upgradeListFragment: UpgradeListFragment
    private lateinit var upgradeAdapter: UpgradeAdapter
    private lateinit var mList: List<ApiViewingApp>
    private lateinit var mListFragment: AppListFragment
    private lateinit var mAdapter: APIAdapter

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        newList = emptyList()
        upgradeList = emptyList()
        mList = emptyList()
        EasyAccess.init(mContext)
        newListFragment = if (savedInstanceState == null) AppListFragment.newInstance(
                isScrollbarEnabled = false, isFadingEdgeEnabled = false, isNestedScrollingEnabled = false)
        else childFragmentManager.getFragment(savedInstanceState, STATE_KEY_NEW_LIST) as AppListFragment
        upgradeListFragment = if (savedInstanceState == null) UpgradeListFragment.newInstance()
        else childFragmentManager.getFragment(savedInstanceState, STATE_KEY_UPG_LIST) as UpgradeListFragment
        mListFragment = if (savedInstanceState == null) AppListFragment.newInstance(
                isScrollbarEnabled = false, isFadingEdgeEnabled = false, isNestedScrollingEnabled = false)
        else childFragmentManager.getFragment(savedInstanceState, STATE_KEY_LIST) as AppListFragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.av_updates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ensureAdded(R.id.avUpdNewListContainer, newListFragment, true)
        ensureAdded(R.id.avUpdUpgListContainer, upgradeListFragment, true)
        ensureAdded(R.id.avUpdatesRecentsListContainer, mListFragment, true)
        val space = X.size(mContext, 5f, X.DP).roundToInt()
        newAdapter = newListFragment.getAdapter().apply {
            setSortMethod(MyUnit.SORT_POSITION_API_TIME)
            topCover = space
            bottomCover = space
        }
        upgradeAdapter = upgradeListFragment.getAdapter().apply {
            setSortMethod(MyUnit.SORT_POSITION_API_TIME)
            topCover = space
            bottomCover = space
        } as UpgradeAdapter
        mAdapter = mListFragment.getAdapter().apply {
            setSortMethod(MyUnit.SORT_POSITION_API_TIME)
            topCover = space
            bottomCover = space
        }
        updateState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        childFragmentManager.putFragment(outState, STATE_KEY_NEW_LIST, newListFragment)
        childFragmentManager.putFragment(outState, STATE_KEY_UPG_LIST, upgradeListFragment)
        childFragmentManager.putFragment(outState, STATE_KEY_LIST, mListFragment)
        super.onSaveInstanceState(outState)
    }

    override fun updateState() {
        lifecycleScope.launch(Dispatchers.Default) {
            if (changedPackages == null) {
                checkUpdate(this@MyUpdatesFragment)
            }
            val mChangedPackages = changedPackages ?: emptyList()
            val noRecords = previousRecords == null
            val mPreviousRecords = previousRecords?.associateBy { it.packageName } ?: emptyMap()
            changedPackages = null
            previousRecords = null
            val spanCount = if (activity == null || isDetached || !isAdded) 1 else mAdapter.spanCount
            val listLimitSize = min(mChangedPackages.size, 10 * spanCount)
            if (mChangedPackages.isEmpty()) {
                mList = emptyList()
                newList = emptyList()
                upgradeList = emptyList()
            } else {
                val anApp = AppMaintainer.get(mContext, lifecycleScope)
                val packages = mChangedPackages.subList(0, listLimitSize)
                val appList = AppRetriever.mapToApp(packages, mContext, anApp)
                mList = ApiViewingViewModel.sortList(appList, MyUnit.SORT_POSITION_API_TIME)
                newList = if (isNewApp || noRecords) emptyList() else {
                    val newPackages = mChangedPackages.filter { p ->
                        mPreviousRecords[p.packageName] == null
                    }
                    val newA = AppRetriever.mapToApp(newPackages, mContext, anApp.clone() as ApiViewingApp)
                    ApiViewingViewModel.sortList(newA, MyUnit.SORT_POSITION_API_TIME)
                }
                val upgradesA = ArrayList<Upgrade>(appList.size)
                val getSize = mChangedPackages.size - appList.size
                val getPackages = ArrayList<PackageInfo>(getSize)
                val getPrevious = ArrayList<ApiViewingApp>(getSize)
                for (p in mChangedPackages) {
                    val previous = mPreviousRecords[p.packageName] ?: continue
                    val new = appList.find { it.packageName == p.packageName }
                    if (new == null) {
                        getPackages.add(p)
                        getPrevious.add(previous)
                    } else {
                        Upgrade.get(previous, new)?.let { upgradesA.add(it) }
                    }
                }
                val getApps = AppRetriever.mapToApp(getPackages, mContext, anApp.clone() as ApiViewingApp)
                val upgradesB = getPrevious.mapIndexedNotNull { index, previous ->
                    Upgrade.get(previous, getApps[index])
                }
                upgradeList = UpgradeComparator.compareTime(upgradesA + upgradesB)
            }

            withContext(Dispatchers.Main) updateUI@ {
                newAdapter.apps = newList
                newListFragment.getRecyclerView().run {
                    setHasFixedSize(true)
                    setItemViewCacheSize(newAdapter.itemCount)
                }
                val newVisibility = if (newList.isEmpty()) View.GONE else View.VISIBLE
                upgradeAdapter.upgrades = upgradeList
                upgradeListFragment.getRecyclerView().run {
                    setHasFixedSize(true)
                    setItemViewCacheSize(upgradeAdapter.itemCount)
                }
                val upgVisibility = if (upgradeList.isEmpty()) View.GONE else View.VISIBLE
                mAdapter.apps = mList
                mListFragment.getRecyclerView().run {
                    setHasFixedSize(true)
                    setItemViewCacheSize(mAdapter.itemCount)
                }
                val visibility = if (mList.isEmpty()) View.GONE else View.VISIBLE
                val view = view ?: return@updateUI
                view.findViewById<TextView>(R.id.avUpdNewTitle)?.visibility = newVisibility
                view.findViewById<TextView>(R.id.avUpdUpgTitle)?.visibility = upgVisibility
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

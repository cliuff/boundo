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
import com.madness.collision.unit.api_viewing.list.AppList
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.origin.AppRetriever
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.unit.api_viewing.upgrade.UpgradeComparator
import com.madness.collision.unit.api_viewing.upgrade.UpgradeListFragment
import com.madness.collision.util.*
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
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
        const val STATE_KEY_VER_LIST = "VerUpdListFragment"

        private const val I_NEW = 0
        private const val I_UPG = 1
        private const val I_VER = 2
        private const val I_UPD = 3

        private var appTimestamp: Long = 0L
        private var sessionTimestamp: Long = 0L

        private var newAppTimestamp = 0L
        /**
         * Whether showing last week history
         */
        val isNewApp: Boolean
            get() = newAppTimestamp > 0L
        var previousRecords: List<ApiViewingApp>? = null
        var changedPackages: List<PackageInfo>? = null
        var isBrandNewSession = false

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
            // app opened the first time in lifetime, keep this new-app session untouched until reopened
            if (lastTimestamp == -1L) newAppTimestamp = System.currentTimeMillis()
            // app reopened, which signals new-app session ended
            else if (mainTimestamp > newAppTimestamp) newAppTimestamp = 0L
            // display recent updates in last week if no history (by default)
            if (isNewApp) lastTimestamp = System.currentTimeMillis() - 604800000
            val isValidSession = lastTimestamp < mainTimestamp
            isBrandNewSession = isNewSession(mainTimestamp) && isValidSession
            if (isBrandNewSession) {
                Utils.getChangedPackages(context, hostFragment, lastTimestamp).let {
                    previousRecords = it.first
                    changedPackages = it.second
                }
                prefSettings.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, System.currentTimeMillis()) }
                appTimestamp = lastTimestamp
                sessionTimestamp = mainTimestamp
            } else {
                Utils.getChangedPackages(context, hostFragment, appTimestamp).let {
                    previousRecords = it.first
                    changedPackages = it.second
                }
            }
            return !changedPackages.isNullOrEmpty()
        }
    }

    private lateinit var mContext: Context
    private lateinit var sections: List<AppSection>

    private class AppSection (val stateKey: String, val containerId: Int, fragmentGetter: (AppSection) -> Fragment) {
        val fragment: Fragment = fragmentGetter(this)
        lateinit var adapter: APIAdapter
        var list: List<*> = emptyList<Any>()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        EasyAccess.init(mContext)
        val fm = childFragmentManager
        val state = savedInstanceState
        sections = listOf(
            AppSection(STATE_KEY_NEW_LIST, R.id.avUpdNewListContainer) {
                fm.getSavedFragment(state, it.stateKey) ?: AppListFragment.newInstance(
                    isScrollbarEnabled = false, isFadingEdgeEnabled = false, isNestedScrollingEnabled = false)
            },
            AppSection(STATE_KEY_UPG_LIST, R.id.avUpdUpgListContainer) {
                fm.getSavedFragment(state, it.stateKey) ?: UpgradeListFragment.newInstance()
            },
            AppSection(STATE_KEY_VER_LIST, R.id.avUpdVerListContainer) {
                fm.getSavedFragment(state, it.stateKey) ?: AppListFragment.newInstance(
                    isScrollbarEnabled = false, isFadingEdgeEnabled = false, isNestedScrollingEnabled = false)
            },
            AppSection(STATE_KEY_LIST, R.id.avUpdatesRecentsListContainer) {
                fm.getSavedFragment(state, it.stateKey) ?: AppListFragment.newInstance(
                    isScrollbarEnabled = false, isFadingEdgeEnabled = false, isNestedScrollingEnabled = false)
            },
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.av_updates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val space = X.size(mContext, 5f, X.DP).roundToInt()
        sections.forEach {
            ensureAdded(it.containerId, it.fragment, true)
            it.adapter = (it.fragment as AppList).getAdapter().apply {
                setSortMethod(MyUnit.SORT_POSITION_API_TIME)
                topCover = space
                bottomCover = space
            }
        }
        updateState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        sections.forEach {
            childFragmentManager.saveFragment(outState, it.stateKey, it.fragment)
        }
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
            val spanCount = if (activity == null || isDetached || !isAdded) 1 else sections[I_UPD].adapter.spanCount
            val listLimitSize = min(mChangedPackages.size, 15 * spanCount)
            if (mChangedPackages.isEmpty()) {
                sections.forEach { it.list = emptyList<Any>() }
            } else {
                val anApp = AppMaintainer.get(mContext, this@MyUpdatesFragment)
                val packages = mChangedPackages.subList(0, listLimitSize)
                val appList = AppRetriever.mapToApp(packages, mContext, anApp)

                // separate new ones from list
                val (newAppList, familiarAppList) = if (isNewApp || noRecords) {
                    emptyList<ApiViewingApp>() to appList
                } else appList.partition {
                    mPreviousRecords[it.packageName] == null
                }
                sections[I_NEW].list = ApiViewingViewModel.sortList(newAppList, MyUnit.SORT_POSITION_API_TIME)

                val (verUpdList, pckUpdList) = familiarAppList.partition {
                    val prev = mPreviousRecords[it.packageName] ?: return@partition false
                    it.verCode != prev.verCode
                }
                sections[I_VER].list = ApiViewingViewModel.sortList(verUpdList, MyUnit.SORT_POSITION_API_TIME)
                sections[I_UPD].list = ApiViewingViewModel.sortList(pckUpdList, MyUnit.SORT_POSITION_API_TIME)

                // upgrades that can be found in appList
                val upgradesA = ArrayList<Upgrade>(familiarAppList.size)
                // size of upgrades that are missing, i.e. excluded from appList
                val getSize = mChangedPackages.size - listLimitSize
                val getPackages = ArrayList<PackageInfo>(getSize)
                val getPrevious = ArrayList<ApiViewingApp>(getSize)
                for (p in mChangedPackages) {
                    val previous = mPreviousRecords[p.packageName] ?: continue
                    // get updated app
                    val new = appList.find { it.packageName == p.packageName }
                    if (new == null) {
                        getPackages.add(p)
                        getPrevious.add(previous)
                    } else {
                        Upgrade.get(previous, new)?.let { upgradesA.add(it) }
                    }
                }
                // get missing apps
                val getApps = AppRetriever.mapToApp(getPackages, mContext, anApp.clone() as ApiViewingApp)
                // get missing upgrades
                val upgradesB = getPrevious.mapIndexedNotNull { index, previous ->
                    Upgrade.get(previous, getApps[index])
                }
                sections[I_UPG].list = UpgradeComparator.compareTime(upgradesA + upgradesB)
            }

            withContext(Dispatchers.Main) updateUI@ {
                updateView(isBrandNewSession)
            }
        }
    }

    private fun updateView(isNewSession: Boolean) {
        sections.forEach {
            it.adapter.appList = it.list
            (it.fragment as AppList).getRecyclerView().run {
                setHasFixedSize(true)
                setItemViewCacheSize(it.adapter.itemCount)
            }
        }

        val view = view ?: return
        listOf(
            R.id.avUpdNewTitle to I_NEW,
            R.id.avUpdUpgTitle to I_UPG,
            R.id.avUpdVerTitle to I_VER,
            R.id.avUpdatesRecentsTitle to I_UPD
        ).forEach {
            val visibility = if (sections[it.second].list.isEmpty()) View.GONE else View.VISIBLE
            view.findViewById<TextView>(it.first)?.run {
                this.visibility = visibility
                if (it.second != I_UPD) return@run
                setText(if (isNewSession) R.string.av_upd_pck_upd else R.string.av_updates_recents)
            }
        }
        view.findViewById<TextView>(R.id.avUpdatesRecentsMore)?.run {
            val hasNoUpdates = sections[I_VER].list.isEmpty() && sections[I_UPD].list.isEmpty()
            this.visibility = if (hasNoUpdates) View.GONE else View.VISIBLE
            setOnClickListener {
                if (activity == null || isDetached || !isAdded) return@setOnClickListener
                val mainViewModel: MainViewModel by activityViewModels()
                mainViewModel.displayUnit(MyBridge.unitName, shouldShowNavAfterBack = true)
            }
        }
    }

}

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
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.diy.SpanAdapter
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Updatable
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.AppMaintainer
import com.madness.collision.unit.api_viewing.databinding.AvUpdSectionBinding
import com.madness.collision.unit.api_viewing.databinding.AvUpdatesBinding
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.list.AppInfoFragment
import com.madness.collision.unit.api_viewing.origin.AppRetriever
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.unit.api_viewing.upgrade.UpgradeAdapter
import com.madness.collision.unit.api_viewing.upgrade.UpgradeComparator
import com.madness.collision.util.P
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

internal class MyUpdatesFragment : TaggedFragment(), Updatable {

    override val category: String = "AV"
    override val id: String = "MyUpdates"

    companion object {
        private const val I_NEW = 0
        private const val I_UPG = 1
        private const val I_VER = 2
        private const val I_PCK = 3
        private const val I_REC = 4

        // timestamp to retrieve app updates, set the first retrieval
        private var appTimestamp: Long = 0L
        // in accordance to mainTimestamp, set the first retrieval
        private var sessionTimestamp: Long = 0L
        // the latest retrieval, set per retrieval
        private var lastRetrievalTime: Long = 0L
        // the second to last retrieval, set per retrieval
        private var secondLastRetrievalTime: Long = 0L

        private var newAppTimestamp = 0L
        /**
         * Whether showing last week history
         */
        val isNewApp: Boolean
            get() = newAppTimestamp > 0L
        var previousRecords: List<ApiViewingApp>? = null
        var changedPackages: List<PackageInfo>? = null
        var isBrandNewSession = false
        private var lastChangedRecords: Map<String, Long> = emptyMap()
        private val mutexUpdateCheck = Mutex()
        private val lockUpdateCheck = Any()

        fun isNewSession(mainTimestamp: Long): Boolean {
            return sessionTimestamp == 0L || sessionTimestamp != mainTimestamp
        }

        fun checkNewUpdateSync(hostFragment: Fragment): Boolean? {
            return synchronized(lockUpdateCheck) { checkNewUpdateActual(hostFragment) }
        }

        suspend fun checkNewUpdate(hostFragment: Fragment): Boolean? {
            return mutexUpdateCheck.withLock { checkNewUpdateActual(hostFragment) }
        }

        private fun checkNewUpdateActual(hostFragment: Fragment): Boolean? {
            val context = hostFragment.context ?: return null
            if (hostFragment.activity == null || hostFragment.isDetached || !hostFragment.isAdded) return null
            val mainViewModel: MainViewModel by hostFragment.activityViewModels()
            val mainTimestamp = mainViewModel.timestamp
            val (prev, changed) = getChangedPackages(context, mainTimestamp, hostFragment)
            previousRecords = prev
            changedPackages = changed
            val lastChanged = lastChangedRecords
            lastChangedRecords = changed.associate { it.packageName to it.lastUpdateTime }
            val lastRecords = lastChanged.map { "${it.key}-${it.value}" }.joinToString()
            val changedRecords = changed.joinToString { "${it.packageName}-${it.lastUpdateTime}" }
            Log.d("AvUpdates", "Checked new update: \n> $lastRecords\n> $changedRecords")
            if (changed.isEmpty()) return null
            if (changed.size != lastChanged.size) return true
            return changed.any { it.lastUpdateTime != lastChanged[it.packageName] }
        }

        private fun getChangedPackages(
            context: Context,
            mainTimestamp: Long,
            lifecycleOwner: LifecycleOwner,
        ): Pair<List<ApiViewingApp>?, List<PackageInfo>> {
            val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            var lastTimestamp = prefSettings.getLong(P.PACKAGE_CHANGED_TIMESTAMP, -1)
            // app opened the first time in lifetime, keep this new-app session untouched until reopened
            if (lastTimestamp == -1L) newAppTimestamp = System.currentTimeMillis()
            // app reopened, which signals new-app session ended
            else if (mainTimestamp > newAppTimestamp) newAppTimestamp = 0L
            // display recent updates in last week if no history (by default)
            if (isNewApp) lastTimestamp = System.currentTimeMillis() - 604_800_000
            val isValidSession = lastTimestamp < mainTimestamp
            isBrandNewSession = isNewSession(mainTimestamp) && isValidSession
            val currentTime = System.currentTimeMillis()
            secondLastRetrievalTime = lastRetrievalTime
            lastRetrievalTime = currentTime
            return if (isBrandNewSession) {
                // use last persisted timestamp to retrieve updates then persist current time
                val result = Utils.getChangedPackages(context, lifecycleOwner, lastTimestamp)
                prefSettings.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, currentTime) }
                appTimestamp = lastTimestamp
                sessionTimestamp = mainTimestamp
                result
            } else {
                // use last the same timestamp used the last time to retrieve updates
                Utils.getChangedPackages(context, lifecycleOwner, appTimestamp)
            }
        }
    }

    private lateinit var mContext: Context
    private var _viewBinding: AvUpdatesBinding? = null
    private val viewBinding: AvUpdatesBinding
        get() = _viewBinding!!
    private val sections: MutableMap<Int, List<*>> = LinkedHashMap()
    private lateinit var concatAdapter: ConcatAdapter

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AvUpdates", "onCreate() hasState=${savedInstanceState != null}")
        mContext = context ?: return
        EasyAccess.init(mContext)
        concatAdapter = ConcatAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("AvUpdates", "onCreateView() hasState=${savedInstanceState != null}")
        _viewBinding = AvUpdatesBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("AvUpdates", "onViewCreated() hasState=${savedInstanceState != null}")
        val spanCount = SpanAdapter.getSpanCount(this, 290f)
        val manager = SpanAdapter.suggestLayoutManager(requireContext(), spanCount)
        viewBinding.avUpdListRecycler.run {
            layoutManager = manager
            adapter = concatAdapter
            if (manager !is GridLayoutManager || spanCount == 1) return@run
            // title views should span a whole row
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    var pos = position
                    val sections = sectionList
                    for (i in sections.indices) {
                        when (pos) {
                            0 -> return spanCount  // title
                            1, 2, 3 -> return 1  // top cover, first item and bottom cover
                        }
                        val (_, list) = sections[i]
                        val rearFillCount = if (spanCount == 1) 0
                        else ((spanCount - (list.size % spanCount)) % spanCount)
                        // list items + title + top cover + rear fill + bottom cover
                        val secSize = list.size + 1 + spanCount + rearFillCount + 1
                        if (pos >= secSize) pos -= secSize else break
                    }
                    return if (pos == 0) spanCount else 1
                }
            }
        }
        updateStateActual(savedInstanceState != null)
    }

    override fun onPause() {
        val fMan = childFragmentManager
        (fMan.findFragmentByTag(AppInfoFragment.TAG) as? BottomSheetDialogFragment?)?.dismiss()
        super.onPause()
    }

    private suspend fun getUpdateLists(
        context: Context, changedPkgList: List<PackageInfo>, detectNew: Boolean,
        previousRecords: Map<String, ApiViewingApp>, listLimitSize: Int
    ) {
        val anApp = AppMaintainer.get(context, this@MyUpdatesFragment)
        val packages = changedPkgList.subList(0, listLimitSize)
        val appList = AppRetriever.mapToApp(packages, context, anApp.clone() as ApiViewingApp)

        // separate new ones from list
        val (newAppList, familiarAppList) = if (detectNew) {
            emptyList<ApiViewingApp>() to appList
        } else appList.partition {
            // not present in previous records
            previousRecords[it.packageName] == null
        }
        sections[I_NEW] = ApiViewingViewModel.sortList(newAppList, MyUnit.SORT_POSITION_API_TIME)

        val (verUpdList, pckUpdList) = familiarAppList.partition {
            val prev = previousRecords[it.packageName] ?: return@partition false
            it.verCode != prev.verCode
        }
        sections[I_VER] = ApiViewingViewModel.sortList(verUpdList, MyUnit.SORT_POSITION_API_TIME)
        val (latestUpdList, recentUpdList) = if (secondLastRetrievalTime <= 0) {
            pckUpdList.partition p@{
                val prev = previousRecords[it.packageName] ?: return@p false
                it.updateTime > prev.updateTime
            }
        } else {
            pckUpdList.partition { it.updateTime >= secondLastRetrievalTime }
        }
        sections[I_PCK] = ApiViewingViewModel.sortList(latestUpdList, MyUnit.SORT_POSITION_API_TIME)
        sections[I_REC] = ApiViewingViewModel.sortList(recentUpdList, MyUnit.SORT_POSITION_API_TIME)

        // upgrades that can be found in appList
        val upgradesA = ArrayList<Upgrade>(familiarAppList.size)
        // size of upgrades that are missing, i.e. excluded from appList
        val getSize = changedPkgList.size - listLimitSize
        val getPackages = ArrayList<PackageInfo>(getSize)
        val getPrevious = ArrayList<ApiViewingApp>(getSize)
        for (p in changedPkgList) {
            val previous = previousRecords[p.packageName] ?: continue
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
        val getApps = AppRetriever.mapToApp(getPackages, context, anApp)
        // get missing upgrades
        val upgradesB = getPrevious.mapIndexedNotNull { index, previous ->
            Upgrade.get(previous, getApps[index])
        }
        sections[I_UPG] = UpgradeComparator.compareTime(upgradesA + upgradesB)
    }

    private var lastUpdateTime = -1L

    override fun updateState() {
        updateStateActual(false)
    }

    private fun updateStateActual(isRestoring: Boolean) {
        // may be invalid
        // Limit update interval to workaround repeated checks,
        // an issue caused by inappropriate fragment management in UpdatesFragment.
        // For child fragments are saved automatically but not properly restored,
        // causing multiple instances of the same fragment class to co-exist at the same time.

        // The interval must not be big. This method gets invoked during restoration,
        // which happens before the restoration of UpdatesFragment,
        // thus it must be possible to be invoked shortly after (150ms) to apply updates.
        val time = SystemClock.uptimeMillis()
        val interval = time - lastUpdateTime
        if (interval < 100) {
            Log.d("AvUpdates", "Skipping this update within ${interval}ms")
            return
        }
        lastUpdateTime = time
        val fragment = this
        lifecycleScope.launch(Dispatchers.Default) {
            // skip when restoring, which happens before updates page checks, to avoid duplicates
            // restoration happens when fMan.getFragment() is called
            if (!isRestoring && changedPackages == null) {
                checkNewUpdate(fragment)
            }
            val mChangedPackages = changedPackages ?: emptyList()
            val noRecords = previousRecords == null
            val mPreviousRecords = previousRecords?.associateBy { it.packageName } ?: emptyMap()
            changedPackages = null
            previousRecords = null

            if (mChangedPackages.isNotEmpty()) {
                val detectNew = isNewApp || noRecords
                val spanCount = if (activity == null || isDetached || !isAdded) 1
                else SpanAdapter.getSpanCount(fragment, 290f)
                val listLimitSize = min(mChangedPackages.size, 15 * spanCount)
                getUpdateLists(mContext, mChangedPackages, detectNew, mPreviousRecords, listLimitSize)
            }

            updateDiff()
            withContext(Dispatchers.Main) { updateView() }
        }
    }

    private var sectionList: List<Pair<Int, List<*>>> = emptyList()

    private suspend fun updateDiff() {
        val oldList = sectionList
        val newList = sections.map { it.key to it.value }.filterNot { it.second.isEmpty() }.sortedBy { it.first }
        val logOld = oldList.joinToString { it.first.toString() }
        val logNew = newList.joinToString { it.first.toString() }
        Log.d("AvUpdates", "Applying $logOld -> $logNew")
        if (oldList.isEmpty() || newList.isEmpty()) {
            withContext(Dispatchers.Main) {
                if (oldList.isEmpty()) {
                    if (newList.isNotEmpty()) insertSections(0, newList)
                } else if (newList.isEmpty()) {
                    val adapters = concatAdapter.adapters
                    adapters.forEach { concatAdapter.removeAdapter(it) }
                }
                sectionList = newList
            }
            return
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].first == newList[newItemPosition].first
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldAppList = oldList[oldItemPosition].second
                val newAppList = newList[newItemPosition].second
                // the items in app list do not update, only insert or remove
                if (oldAppList.size != newAppList.size) return false
                for (i in oldAppList.indices) {
                    val oldApp = oldAppList[i]; val newApp = newAppList[i]
                    kotlin.run a@{
                        if (oldApp !is ApiViewingApp) return@a Unit
                        if (newApp !is ApiViewingApp) return false
                        if (oldApp.packageName != newApp.packageName) return false; null
                    } ?: continue
                    kotlin.run u@{
                        if (oldApp !is Upgrade) return@u Unit
                        if (newApp !is Upgrade) return false
                        if (oldApp.new.packageName != newApp.new.packageName) return false; null
                    } ?: continue
                    if (oldApp != newApp) return false
                }
                return true
            }
        }, false)
        withContext(Dispatchers.Main) {
            val diffList = oldList.toMutableList()
            diff.dispatchUpdatesTo(object : ListUpdateCallback {
                override fun onInserted(position: Int, count: Int) {
                    val newPos = kotlin.run {
                        if (position <= 0) return@run 0
                        val oldItem = diffList[position - 1]
                        newList.indexOfFirst { it.first == oldItem.first } + 1
                    }
                    Log.d("AvUpdates", "Diff insert: pos=$position, count=$count, newPos=$newPos")
                    val newSec = newList.subList(newPos, newPos + count)
                    insertSections(position * 2, newSec)
                    diffList.addAll(position, newSec)
                }

                override fun onRemoved(position: Int, count: Int) {
                    Log.d("AvUpdates", "Diff remove: pos=$position, count=$count")
                    val adapters = concatAdapter.adapters
                    for (offset in (0 until count).reversed()) {
                        val index = position + offset
                        val adapterIndex = index * 2
                        Log.d("AvUpdates", "Diff remove: ${diffList[index].first} at $index, adapter at $adapterIndex")
                        concatAdapter.removeAdapter(adapters[adapterIndex + 1])
                        concatAdapter.removeAdapter(adapters[adapterIndex])
                        diffList.removeAt(index)
                    }
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    Log.d("AvUpdates", "Diff move: from=$fromPosition, to=$toPosition")
                }

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    val newPos = kotlin.run {
                        val oldItem = diffList[position]
                        newList.indexOfFirst { it.first == oldItem.first }
                    }
                    Log.d("AvUpdates", "Diff change: pos=$position, count=$count, newPos=$newPos")
                    val adapters = concatAdapter.adapters
                    for (offset in 0 until count) {
                        val index = position + offset
                        val adapterIndex = index * 2
                        val adapter = adapters[adapterIndex + 1]
                        if (adapter !is APIAdapter) continue
                        Log.d("AvUpdates", "Diff change: at $index")
                        adapter.appList = newList[newPos + offset].second
                        adapter.notifyDataSetChanged()
                    }
                }
            })
            sectionList = newList
        }
    }

    private fun insertSections(position: Int, newSections: List<Pair<Int, List<*>>>) {
        if (newSections.isEmpty()) return
        val listListener = object : APIAdapter.Listener {
            override val click: (ApiViewingApp) -> Unit = {
                AppInfoFragment(it).show(childFragmentManager, AppInfoFragment.TAG)
            }
            override val longClick: (ApiViewingApp) -> Boolean = {
                true
            }
        }
        val titles = mapOf(
            I_NEW to R.string.av_upd_new,
            I_UPG to R.string.av_upd_upg,
            I_VER to R.string.av_upd_ver_upd,
            I_PCK to R.string.av_upd_pck_upd,
            I_REC to R.string.av_updates_recents,
        )
        val fragment = this
        val space = X.size(mContext, 5f, X.DP).roundToInt()
        for (i in newSections.indices) {
            val (cat, apps) = newSections[i]
            val adapter = if (cat == I_UPG) UpgradeAdapter(mContext, listListener, lifecycleScope)
            else APIAdapter(mContext, listListener, lifecycleScope)
            adapter.run {
                resolveSpanCount(fragment, 290f)
                setSortMethod(MyUnit.SORT_POSITION_API_TIME)
                topCover = space
                bottomCover = space
                appList = apps
            }
            val title = titles[cat]?.let { mContext.getString(it) }.orEmpty()
            val titleAdapter= AppSectionAdapter(title)
            val adapterPos = position + i * 2
            concatAdapter.addAdapter(adapterPos, titleAdapter)
            concatAdapter.addAdapter(adapterPos + 1, adapter)
        }
    }

    private fun updateView() {
        val hasNoUpdates = sections.all { it.value.isEmpty() }
        viewBinding.avUpdatesRecentsMore.run {
            visibility = if (hasNoUpdates) View.GONE else View.VISIBLE
            setOnClickListener {
                if (activity == null || isDetached || !isAdded) return@setOnClickListener
                val mainViewModel: MainViewModel by activityViewModels()
                mainViewModel.displayUnit(MyBridge.unitName, shouldShowNavAfterBack = true)
            }
        }
    }

}

class AppSectionViewHolder(val binding: AvUpdSectionBinding) : RecyclerView.ViewHolder(binding.root)

class AppSectionAdapter(private val title: String) : RecyclerView.Adapter<AppSectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppSectionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AvUpdSectionBinding.inflate(inflater, parent, false)
        return AppSectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppSectionViewHolder, position: Int) {
        holder.binding.avUpdSecTitle.text = title
    }

    override fun getItemCount(): Int = 1
}

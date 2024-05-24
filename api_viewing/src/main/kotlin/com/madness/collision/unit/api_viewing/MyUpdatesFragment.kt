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
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.madness.collision.diy.SpanAdapter
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Updatable
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.databinding.AvUpdSectionBinding
import com.madness.collision.unit.api_viewing.databinding.AvUpdatesBinding
import com.madness.collision.unit.api_viewing.list.*
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.ui.info.AppInfoFragment
import com.madness.collision.unit.api_viewing.ui.upd.AppUpdatesChecker
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.unit.api_viewing.upgrade.UpgradeAdapter
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.dev.idString
import com.madness.collision.util.hasUsageAccess
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

internal class MyUpdatesFragment : TaggedFragment(), Updatable, AppInfoFragment.Callback {

    override val category: String = "AV"
    override val id: String = "MyUpdates"

    companion object {
        private const val I_NEW = 0
        private const val I_UPG = 1
        private const val I_VER = 2
        private const val I_PCK = 3
        private const val I_REC = 4
        private const val I_USE = 10

        private val updatesChecker = AppUpdatesChecker()
        private val mutexUpdateCheck = Mutex()
        private val lockUpdateCheck = Any()

        fun checkNewUpdateSync(hostFragment: Fragment): Boolean? {
            return synchronized(lockUpdateCheck) { checkNewUpdateActual(hostFragment) }
        }

        suspend fun checkNewUpdate(hostFragment: Fragment): Boolean? {
            return mutexUpdateCheck.withLock { checkNewUpdateActual(hostFragment) }
        }

        private fun checkNewUpdateActual(hostFragment: Fragment): Boolean? {
            val context = hostFragment.context ?: return null
            if (hostFragment.run { activity == null || isDetached || !isAdded }) return null
            val mainViewModel: MainViewModel by hostFragment.activityViewModels()
            return updatesChecker.checkNewUpdate(mainViewModel.timestamp, context, hostFragment)
        }
    }

    private val dID = "@" + idString.takeLast(2)
    private lateinit var mContext: Context
    private var _viewBinding: AvUpdatesBinding? = null
    private val viewBinding: AvUpdatesBinding? by ::_viewBinding
    private val sections: MutableMap<Int, List<*>> = LinkedHashMap()
    private lateinit var concatAdapter: ConcatAdapter
    private val diffMutex = Mutex()
    private val popOwner = AppPopOwner()

    private suspend fun getViewBinding(): AvUpdatesBinding {
        yield()  // cooperative
        return viewBinding ?: throw CancellationException("view binding is null")
    }

    override fun getAppOwner(): AppInfoFragment.AppOwner {
        return object : AppInfoFragment.AppOwner {
            override val size: Int get() = secAppList.size

            override fun get(index: Int): ApiViewingApp? {
                return secAppList.getOrNull(index)
            }

            override fun getIndex(app: ApiViewingApp): Int {
                return secAppList.indexOf(app)
            }

            override fun findInAll(pkgName: String): ApiViewingApp? {
                return secAppList.find { it.packageName == pkgName }
                    ?: DataMaintainer.get(mContext, viewLifecycleOwner).selectApp(pkgName)
            }
        }
    }

    override fun onAppChanged(app: ApiViewingApp) {
        popOwner.updateState(app)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AvUpdates", "$dID onCreate() hasState=${savedInstanceState != null}")
        mContext = context ?: return
        EasyAccess.init(mContext)
        concatAdapter = ConcatAdapter()
        // commented out: disable re-pop
//        popOwner.register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("AvUpdates", "$dID onCreateView() hasState=${savedInstanceState != null}")
        val viewBinding = AvUpdatesBinding.inflate(inflater, container, false)
        _viewBinding = viewBinding
        return viewBinding.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("AvUpdates", "$dID onViewCreated() hasState=${savedInstanceState != null}")
        val spanCount = SpanAdapter.getSpanCount(this, 290f)
        val manager = SpanAdapter.suggestLayoutManager(requireContext(), spanCount)
        viewBinding?.avUpdListRecycler?.run {
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
        if (interval < 80) {
            Log.d("AvUpdates", "$dID Skipping this update within ${interval}ms")
            return
        }
        lastUpdateTime = time
        val fragment = this
        lifecycleScope.launch(Dispatchers.Default) {
            // skip when restoring, which happens before updates page checks, to avoid duplicates
            // restoration happens when fMan.getFragment() is called
            if (!isRestoring && updatesChecker.isCheckNeeded()) {
                checkNewUpdate(fragment)
            }

            val spanCount = if (activity == null || isDetached || !isAdded) 1
            else SpanAdapter.getSpanCount(fragment, 290f)
            val changedLimit = 15 * spanCount
            val usedLimit = when (spanCount) {
                1 -> 5
                2 -> 6
                3 -> 6
                else -> 6
            }

            updatesChecker.getSections(changedLimit, usedLimit, mContext, this@MyUpdatesFragment)
                .forEach { (index, list) -> sections[index.code] = list }
            diffMutex.withLock { updateDiff(sections) }
            val hasUpdates = sections.any { it.value.isNotEmpty() }
            withContext(Dispatchers.Main) { updateView(getViewBinding(), hasUpdates) }
        }
    }

    private var secAppList: List<ApiViewingApp> = emptyList()
    private var sectionList: List<Pair<Int, List<*>>> = emptyList()
        set(value) {
            field = value
            secAppList = value.flatMap apps@{ (_, list) ->
                if (list.isEmpty()) return@apps emptyList()
                list.mapNotNull { item ->
                    when (item) {
                        is ApiViewingApp -> item
                        is Upgrade -> item.new
                        else -> null
                    }
                }
            }
        }

    private suspend fun updateDiff(sections: Map<Int, List<*>>) {
        val oldList = sectionList
        val newList = sections.map { it.key to it.value }.filterNot { it.second.isEmpty() }.sortedBy { it.first }
        val logOld = oldList.joinToString { it.first.toString() }
        val logNew = newList.joinToString { it.first.toString() }
        Log.d("AvUpdates", "$dID Applying $logOld -> $logNew")
        if (oldList.isEmpty() || newList.isEmpty()) {
            withContext(Dispatchers.Main) {
                if (oldList.isEmpty()) {
                    if (newList.isNotEmpty()) insertSections(0, newList)
                } else if (newList.isEmpty()) {
                    val adapters = concatAdapter.adapters
                    adapters.forEach { removeAdapter(it) }
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
                    Log.d("AvUpdates", "$dID Diff insert: pos=$position, count=$count, newPos=$newPos")
                    val newSec = newList.subList(newPos, newPos + count)
                    insertSections(position * 2, newSec)
                    diffList.addAll(position, newSec)
                }

                override fun onRemoved(position: Int, count: Int) {
                    Log.d("AvUpdates", "$dID Diff remove: pos=$position, count=$count")
                    val adapters = concatAdapter.adapters
                    for (offset in (0 until count).reversed()) {
                        val index = position + offset
                        val adapterIndex = index * 2
                        Log.d("AvUpdates", "$dID Diff remove: ${diffList[index].first} at $index, adapter at $adapterIndex")
                        arrayOf(adapterIndex + 1, adapterIndex)
                            .mapNotNull(adapters::getOrNull)
                            .forEach(::removeAdapter)
                        diffList.removeAt(index)
                    }
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    Log.d("AvUpdates", "$dID Diff move: from=$fromPosition, to=$toPosition")
                }

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    val newPos = kotlin.run {
                        val oldItem = diffList[position]
                        newList.indexOfFirst { it.first == oldItem.first }
                    }
                    Log.d("AvUpdates", "$dID Diff change: pos=$position, count=$count, newPos=$newPos")
                    val adapters = concatAdapter.adapters
                    for (offset in 0 until count) {
                        val index = position + offset
                        val adapterIndex = index * 2
                        val adapter = adapters.getOrNull(adapterIndex + 1)
                        if (adapter !is APIAdapter) continue
                        Log.d("AvUpdates", "$dID Diff change: at $index")
                        adapter.appList = newList[newPos + offset].second
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
                popOwner.pop(this@MyUpdatesFragment, it)
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
            I_USE to R.string.av_upd_used,
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
        timeUpdateJob?.cancel()
        if (concatAdapter.adapters.filterIsInstance<APIAdapter>().isEmpty()) return
        if (view != null) timeUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            scheduleTimeUpdate(viewLifecycleOwner.lifecycle, dID) {
                viewBinding?.avUpdListRecycler
            }
        }
    }

    private fun removeAdapter(adapter: RecyclerView.Adapter<*>) {
        val concat = concatAdapter
        concat.removeAdapter(adapter)
        if (concat.adapters.filterIsInstance<APIAdapter>().isEmpty()) {
            timeUpdateJob?.cancel()
        }
    }

    private var timeUpdateJob: Job? = null

    private suspend fun scheduleTimeUpdate(lifecycle: Lifecycle, idTag: String, rv: () -> RecyclerView?) {
        val scheduleTime = SystemClock.uptimeMillis()
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // delay only right after scheduling
            if (SystemClock.uptimeMillis() - scheduleTime < 500) delay(30.seconds)
            while (isActive) {
                rv()?.let { recyclerView ->
                    val adapters = (recyclerView.adapter as ConcatAdapter).adapters
                    val man = recyclerView.layoutManager as LinearLayoutManager
                    val startIndex = man.findFirstVisibleItemPosition()
                    val endIndex = man.findLastVisibleItemPosition()
                    if (startIndex <= endIndex) {
                        Log.d("av.updates", "$idTag Updating time [$startIndex, $endIndex]")
                        updateTime(adapters, startIndex, endIndex, idTag)
                    }
                }
                delay(45.seconds)
            }
        }
    }

    private fun updateTime(adapters: List<Adapter<*>>, startIndex: Int, endIndex: Int, idTag: String) {
        val accu = adapters.runningFold(0) { c, it -> c + it.itemCount }
        adapters.forEachIndexed m@{ i, adapter ->
            if (adapter !is APIAdapter) return@m
            val accuCount = accu[i]
            if (startIndex >= accuCount) return@m
            val lastAccuCount = if (i > 0) accu[i - 1] else 0
            if (endIndex < accuCount && endIndex < lastAccuCount) return@m
            val changeIndex = if (startIndex >= lastAccuCount) startIndex - lastAccuCount else 0
            val changeEnd = if (endIndex < accuCount) endIndex + 1 - lastAccuCount else adapter.itemCount
            val changeCount = changeEnd - changeIndex
            adapter.notifyItemRangeChanged(changeIndex, changeCount, APIAdapter.Payload.UpdateTime)
            Log.d("av.updates", "$idTag Updated time for $changeCount items from $changeIndex")
        }
    }

    private fun updateView(viewBinding: AvUpdatesBinding, hasUpdates: Boolean) {
        viewBinding.avUpdatesRecentsMore.run {
            visibility = if (!hasUpdates) View.GONE else View.VISIBLE
            setOnClickListener {
                if (activity == null || isDetached || !isAdded) return@setOnClickListener
                val mainViewModel: MainViewModel by activityViewModels()
                mainViewModel.displayUnit(MyBridge.unitName, shouldShowNavAfterBack = true)
            }
        }
        val hasUsageAccess = context?.hasUsageAccess == true
        viewBinding.avUpdUsageAccess.run {
            if (hasUsageAccess) {
                setOnClickListener(null)
            } else {
                setOnClickListener click@{
                    val context = context ?: return@click
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            isVisible = !hasUsageAccess
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

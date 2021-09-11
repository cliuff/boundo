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

package com.madness.collision.unit.api_viewing.main

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.core.content.edit
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.*
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.databinding.FragmentApiBinding
import com.madness.collision.unit.api_viewing.stats.StatisticsFragment
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.getFullLabel
import com.madness.collision.unit.api_viewing.tag.app.getNormalLabel
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.PopupUtil
import com.madness.collision.util.notify
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.*
import com.madness.collision.R as MainR

internal class MainStatus(
    private val host: MyUnit,
    // external data dependencies
    private val dataConfig: MainDataConfig,
    private val prefSettings: SharedPreferences
) : LifecycleOwner by host {
    private val mainViewModel: MainViewModel by host.activityViewModels()

    // external context dependencies
    lateinit var context: Context
    lateinit var viewBinding: FragmentApiBinding
    lateinit var viewModel: ApiViewingViewModel  // AndroidViewModel

    private var loadItem: Int by dataConfig::loadItem
    private var sortItem: Int by dataConfig::sortItem
    private var displayItem: Int by dataConfig::displayItem
    private val settingsPreferences: SharedPreferences by ::prefSettings
    private val refreshLayout: SwipeRefreshLayout
        get() = viewBinding.apiSwipeRefresh

    // internal
    private var popSrc: PopupMenu? = null
    private var popTags: CollisionDialog? = null
    private val antiSelectedIndexes = mutableSetOf<Int>()
    private lateinit var rDisplay: RunnableDisplay

    companion object {
        const val DISPLAY_APPS_USER: Int = 0
        const val DISPLAY_APPS_SYSTEM: Int = 1
        const val DISPLAY_APPS_ALL: Int = 2
        const val DISPLAY_APPS_APK: Int = 3
        const val DISPLAY_APPS_SELECT: Int = 4
        const val DISPLAY_APPS_VOLUME: Int = 5
    }

    private inner class RunnableDisplay(var position: Int) : Runnable {
        override fun run()  {
            if (position != DISPLAY_APPS_SELECT && position != DISPLAY_APPS_VOLUME) {
                settingsPreferences.edit { putInt(PrefUtil.AV_LIST_SRC_ITEM, position) }
            }
            host.loadSortedList(loadItem, sortEfficiently = true, fg = true)
            viewModel.sortApps(sortItem)
            host.refreshList()
        }
    }

    fun setupDisplayRunnable() {
        rDisplay = RunnableDisplay(displayItem)
    }

    fun joinDisplay() {
        rDisplay.position = displayItem
        ApiTaskManager.join(task = rDisplay)
    }

    fun createListSrcPopup(context: Context, anchor: View) {
        popSrc = PopupMenu(context, anchor).apply {
            if (OsUtils.satisfy(OsUtils.Q)) setForceShowIcon(true)
            inflate(R.menu.av_list_src)
            setOnMenuItemClickListener {
                clickListSrcItem(it)
            }
        }
    }

    fun getListSrcMenuItem(item: Int): MenuItem? {
        return popSrc?.menu?.getItem(item)
    }

    fun showListSrcPopup() {
        popSrc?.show()
    }

    /**
     * Select an item
     * Update menu item selection and invoke the corresponding callback
     */
    fun selectListSrcItem(item: Int) {
        getListSrcMenuItem(item)?.let {
            // avoid duplicate refresh
            if (it.isChecked) return
            clickListSrcItem(it)
        }
    }

    private val storagePermissionLauncher = host.registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted.not()) {
            refreshLayout.isRefreshing = false
            host.notify(com.madness.collision.R.string.toast_permission_storage_denied)
            return@registerForActivityResult
        }
        joinDisplay()
    }

    private fun clickListSrcItem(item: MenuItem): Boolean {
        refreshLayout.isRefreshing = true
        loadListSrcItem(item)
        if (host.needPermission { storagePermissionLauncher.launch(it) }) return true
        // clear tag filter
        host.clearTagFilter(context)
        rDisplay.position = displayItem
        ApiTaskManager.join(task = rDisplay)
        return true
    }

    fun loadListSrcItem(item: Int) {
        getListSrcMenuItem(item)?.let {
            // avoid duplicate refresh
            if (it.isChecked) return
            loadListSrcItem(it)
        }
    }

    /**
     * Load data and views
     */
    private fun loadListSrcItem(item: MenuItem) {
        viewBinding.avListSrc.text = item.title
        item.isChecked = true
        var isStatsAvailable = false
        when (item.itemId){
            R.id.avListSrcUsr -> {
                displayItem = DISPLAY_APPS_USER
                isStatsAvailable = true
                ApiUnit.USER
            }
            R.id.avListSrcSys -> {
                displayItem = DISPLAY_APPS_SYSTEM
                isStatsAvailable = true
                ApiUnit.SYS
            }
            R.id.avListSrcAll -> {
                displayItem = DISPLAY_APPS_ALL
                isStatsAvailable = true
                ApiUnit.ALL_APPS
            }
            R.id.avListSrcDeviceApk -> {
                displayItem = DISPLAY_APPS_APK
                ApiUnit.APK
            }
            R.id.avListSrcCustom -> {
                displayItem = DISPLAY_APPS_SELECT
                if (loadItem == ApiUnit.DISPLAY) ApiUnit.DISPLAY
                else ApiUnit.SELECTED
            }
            R.id.avListSrcVolume -> {
                displayItem = DISPLAY_APPS_VOLUME
                ApiUnit.VOLUME
            }
            else -> null
        }?.let { loadItem = it }
        val listener = if (isStatsAvailable) View.OnClickListener {
            mainViewModel.displayFragment(StatisticsFragment.newInstance(loadItem))
        } else null
        viewBinding.avMainStatsContainer.setOnClickListener(listener)
    }

    fun setupTagFilterPopup(context: Context) {
        val rankedTags = AppTagManager.tags.values.sortedBy { it.rank }
        val filterTags = rankedTags.map { it.getFullLabel(context)?.toString() ?: "" }
        val tagIcons = rankedTags.map { it.icon.drawableResId }
        val popTags = PopupUtil.selectMulti(
            context, R.string.av_main_filter_tip, filterTags, tagIcons, emptySet()) { pop, _, indexes ->
            pop.dismiss()
            closeFilterTagMenu(indexes, rankedTags)
        }
        val container: ViewGroup = popTags.findViewById(MainR.id.popupSelectMultiContainer)
        val longClickListener = View.OnLongClickListener {
            val checkedIndex = it.tag as Int
            if (it is CompoundButton) {
                it.isChecked = true
                if (checkedIndex in antiSelectedIndexes) {
                    it.paintFlags = it.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    antiSelectedIndexes.remove(checkedIndex)
                } else {
                    it.paintFlags = it.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    antiSelectedIndexes.add(checkedIndex)
                }
            }
            true
        }
        container.forEach {
            it.setOnLongClickListener(longClickListener)
        }
        this.popTags = popTags
    }

    private fun closeFilterTagMenu(checkedIndexes: Set<Int>, tags: List<AppTagInfo>) {
        refreshLayout.isRefreshing = true
        lifecycleScope.launch(Dispatchers.Default) {
            val value = checkedIndexes.associate {
                val name = tags[it].id
                val isAntied = it in antiSelectedIndexes
                name to TriStateSelectable(name, !isAntied)
            }
            val firstCheckedTagId = checkedIndexes.firstOrNull()
            val singleTitle = if (checkedIndexes.size == 1) tags[firstCheckedTagId!!].getNormalLabel(context) else null
            val strikeThru = firstCheckedTagId in antiSelectedIndexes
            withContext(Dispatchers.Main) {
                updateTagFilterView(context, singleTitle, strikeThru, value.isNotEmpty())
            }
            // cannot detect whether changed, previous state cannot be determined
            // because filter state share data with normal state
            AppTag.loadTagSettings(context, value, false)
            val completeList = viewModel.screen4Display(loadItem)
            val displayList = if (value.isEmpty()) completeList
            else filterByTag(context, completeList)
            withContext(Dispatchers.Main) {
                host.updateList(displayList)
                refreshLayout.isRefreshing = false
            }
        }
    }

    @MainThread
    private fun updateTagFilterView(context: Context, title: CharSequence?, strikeThru: Boolean, longClick: Boolean) {
        viewBinding.avMainFilterText.run {
            text = title?.toString()
            if (title != null) {
                val flagStrikeThru = Paint.STRIKE_THRU_TEXT_FLAG
                paintFlags = if (strikeThru) (paintFlags or flagStrikeThru)
                else (paintFlags and flagStrikeThru.inv())
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        val longClickListener = if (longClick) View.OnLongClickListener {
            host.clearTagFilter(context)
            host.updateList(viewModel.screen4Display(loadItem))
            true
        } else null
        viewBinding.avMainFilterContainer.setOnLongClickListener(longClickListener)
    }

    /**
     * Filter app in parallel.
     */
    private suspend fun filterByTag(context: Context, appList: List<ApiViewingApp>)
    : List<ApiViewingApp> = coroutineScope {
        appList.map {
            async(Dispatchers.Default) {
                val result = AppTag.filterTags(context, it)
                if (result) it else null
            }
        }.mapNotNull { it.await() }
    }

    fun showTagsPopup() {
        popTags?.show()
    }
}

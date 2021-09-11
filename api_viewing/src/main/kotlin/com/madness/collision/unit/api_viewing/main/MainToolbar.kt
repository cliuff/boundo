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
import android.view.MenuItem
import android.view.View
import android.widget.Filter
import android.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.*
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.databinding.FragmentApiBinding
import com.madness.collision.unit.api_viewing.device.DeviceApi
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.FilePop
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.notify
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.madness.collision.R as MainR

internal class MainToolbar(
    private val host: MyUnit,
    // external data dependencies
    private val dataConfig: MainDataConfig,
    private val prefSettings: SharedPreferences,
) : LifecycleOwner by host {
    private val activity: FragmentActivity?
        get() = host.activity
    private val mainViewModel: MainViewModel by host.activityViewModels()

    // external context dependencies
    lateinit var context: Context
    lateinit var viewBinding: FragmentApiBinding
    lateinit var viewModel: ApiViewingViewModel  // AndroidViewModel
    lateinit var listFragment: AppListFragment

    private var sortItem: Int by dataConfig::sortItem
    private val settingsPreferences: SharedPreferences by ::prefSettings
    private val refreshLayout: SwipeRefreshLayout
        get() = viewBinding.apiSwipeRefresh
    private val adapter: APIAdapter
        get() = listFragment.getAdapter()

    // internal
    private val service = AppMainService()
    private var searchBackPressedCallback: OnBackPressedCallback? = null
    private var popSort: PopupMenu? = null
    private lateinit var toolbar: Toolbar
    private lateinit var rSort: RunnableSort

    private inner class RunnableSort(var position: Int) : Runnable {
        override fun run()  {
            adapter.setSortMethod(position)
            settingsPreferences.edit { putInt(PrefUtil.AV_SORT_ITEM, position) }
            viewModel.sortApps(sortItem)
            host.refreshList()
        }
    }

    fun setupSortRunnable() {
        rSort = RunnableSort(sortItem)
    }

    fun removeSortPopup() {
        popSort = null
    }

    fun createOptions(context: Context, toolbar: Toolbar) {
        val textRes = if (EasyAccess.isViewingTarget) R.string.sdkcheck_dialog_targetsdktext
        else R.string.sdkcheck_dialog_minsdktext
        val titleAffix = context.getString(textRes)
        toolbar.title = context.getString(MainR.string.apiViewer) + " • $titleAffix"
        this.toolbar = toolbar
    }

    private val storagePermissionLauncher = host.registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted.not()) {
            refreshLayout.isRefreshing = false
            host.notify(com.madness.collision.R.string.toast_permission_storage_denied)
            return@registerForActivityResult
        }
        host.reloadList()
    }

    private fun Filter.filterBy(constraint: CharSequence) {
        filter(constraint) {
            host.doListUpdateAftermath()
            refreshLayout.isRefreshing = false
        }
    }

    fun selectOption(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.apiTBRefresh -> {
                refreshLayout.isRefreshing = true
                if (host.needPermission { storagePermissionLauncher.launch(it) }) return true
                host.reloadList()
                return true
            }
            R.id.apiTBSearch -> {
                val searchView = item.actionView as SearchView
                searchView.queryHint = context.getText(MainR.string.sdk_search_hint)
                val filter = listFragment.filter
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    private var sOri: String = ""

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        val window = activity?.window ?: return true
                        SystemUtil.hideImeCompat(context, searchView, window)
                        searchView.clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val sAft: String = newText?.replace(" ", "") ?: ""
                        if (sOri.compareTo(sAft, true) == 0) return true
                        refreshLayout.isRefreshing = true
                        if (sAft.isEmpty()) lifecycleScope.launch(Dispatchers.Default) {
                            host.refreshList()
                            filter.onCancel()
                        } else {
                            filter.isAddition = sOri.isEmpty() || sAft.startsWith(sOri)
                            filter.filterBy(sAft)
                        }
                        sOri = sAft
                        return true
                    }
                })
                ensureSearchActionCollapse(item)
                return true
            }
            R.id.apiTBSort -> {
                if (popSort == null) {
                    // when in overflow menu, menu item has no icon button
                    // thus anchor would be null
                    val anchor = toolbar.findViewById<View>(R.id.apiTBSort) ?: return false
                    popSort = PopupMenu(context, anchor).apply {
                        if (OsUtils.satisfy(OsUtils.Q)) setForceShowIcon(true)
                        inflate(R.menu.api_sort)
                        setOnMenuItemClickListener {
                            clickSortItem(it)
                        }
                        menu.getItem(sortItem).isChecked = true
                    }
                }
                popSort!!.show()
                return true
            }
            R.id.apiTBSettings -> {
                val settings = MyBridge.getSettings()
                mainViewModel.displayFragment(settings)
                return true
            }
            R.id.apiTBManual -> {
                CollisionDialog.alert(context, R.string.avManual).show()
                return true
            }
            R.id.apiTBViewingTarget -> {
                EasyAccess.isViewingTarget = !EasyAccess.isViewingTarget
                settingsPreferences.edit { putBoolean(PrefUtil.AV_VIEWING_TARGET, EasyAccess.isViewingTarget) }
                val textRes = if (EasyAccess.isViewingTarget) R.string.sdkcheck_dialog_targetsdktext else R.string.sdkcheck_dialog_minsdktext
                val titleAffix = context.getString(textRes)
                toolbar.title = context.getString(MainR.string.apiViewer) + " • $titleAffix"
                adapter.notifyDataSetChanged()
                return true
            }
            R.id.avMainTbShare -> {
                lifecycleScope.launch(Dispatchers.Default) {
                    exportList(context)
                }
                return true
            }
            R.id.avMainTbDevice -> {
                DeviceApi().show(context)
                return true
            }
        }
        return false
    }

    private suspend fun exportList(context: Context) {
        val file = service.exportList(context, adapter.apps) ?: return
        val label = "App List"
        withContext(Dispatchers.Main) {
            FilePop.by(context, file, "text/csv", MainR.string.fileActionsShare, imageLabel = label)
                .show(host.childFragmentManager, FilePop.TAG)
        }
    }

    private fun ensureSearchActionCollapse(menuItem: MenuItem) {
        val activity = activity ?: return
        // action view is not expanded for the time being, make it in a listener
        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                item ?: return true
                searchBackPressedCallback?.remove()
                searchBackPressedCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (item.isActionViewExpanded) {
                            item.collapseActionView()
                        }
                    }
                }.also { activity.onBackPressedDispatcher.addCallback(it) }
                return true
            }
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                item ?: return true
                removeSearchBackCallback()
                return true
            }
        })
    }

    private fun clickSortItem(item: MenuItem): Boolean  {
        refreshLayout.isRefreshing = true
        sortItem = when (item.itemId) {
            R.id.menuApiSortAPIL -> MyUnit.SORT_POSITION_API_LOW
            R.id.menuApiSortAPIH -> MyUnit.SORT_POSITION_API_HIGH
            R.id.menuApiSortAPIName -> MyUnit.SORT_POSITION_API_NAME
            R.id.menuApiSortAPITime -> MyUnit.SORT_POSITION_API_TIME
            else -> sortItem
        }
        item.isChecked = true
        // clear tag filter
        host.clearTagFilter(context)
        rSort.position = sortItem
        ApiTaskManager.join(task = rSort)
        return true
    }

    fun removeSearchBackCallback() {
        searchBackPressedCallback?.remove()
        searchBackPressedCallback = null
    }
}

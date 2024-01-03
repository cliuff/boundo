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

package com.madness.collision.wearable.av

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.PopupMenu
import android.widget.Spinner
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.wear.widget.WearableLinearLayoutManager
import com.madness.collision.wearable.R
import com.madness.collision.wearable.av.data.ApiUnit
import com.madness.collision.wearable.av.data.ApiViewingApp
import com.madness.collision.wearable.av.data.EasyAccess
import com.madness.collision.wearable.databinding.FragmentApiBinding
import com.madness.collision.wearable.main.MainViewModel
import com.madness.collision.wearable.misc.MiscApp
import com.madness.collision.wearable.util.P
import com.madness.collision.wearable.util.X
import kotlinx.coroutines.Dispatchers

internal class ApiFragment: Fragment(), AdapterView.OnItemSelectedListener, MenuItem.OnMenuItemClickListener{
    companion object {
        private const val DISPLAY_APPS_USER: Int = 0
        private const val DISPLAY_APPS_SYSTEM: Int = 1
        private const val DISPLAY_APPS_ALL: Int = 2

        const val SORT_POSITION_API_LOW: Int  = 0
        const val SORT_POSITION_API_HIGH: Int  = 1
        const val SORT_POSITION_API_NAME: Int  = 2
        const val SORT_POSITION_API_TIME: Int  = 3
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: ApiViewingViewModel by activityViewModels()

    private var sortItem: Int = SORT_POSITION_API_LOW
    private var displayItem: Int = DISPLAY_APPS_USER
    private val loadedItems: ApiUnit
        get() = viewModel.loadedItems
    private var loadItem: Int = ApiUnit.NON
    private var listInsetBottom: Int = 0
    private lateinit var rSort: RunnableSort
    private lateinit var rDisplay: RunnableDisplay

    // views
    private lateinit var spDisplayMethod: Spinner
    private lateinit var adapter: APIAdapter
    private lateinit var manager: WearableLinearLayoutManager
    private var popSort: PopupMenu? = null
    private var mViews: FragmentApiBinding? = null

    // context related
    private lateinit var pm: PackageManager
    private lateinit var settingsPreferences: SharedPreferences

    private fun sortMethod(){
        val activity = activity ?: return
        if (popSort == null) {
            popSort = PopupMenu(context, activity.findViewById(0))
            popSort!!.run {
                menuInflater.inflate(0, menu)
                setOnMenuItemClickListener(this@ApiFragment::onMenuItemClick)
                menu.getItem(sortItem).isChecked = true
            }
        }
        popSort!!.show()
    }

    override fun onStop() {
        ApiTaskManager.cancelAll()
        super.onStop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mViews = FragmentApiBinding.inflate(inflater, container, false)
        this.mViews = mViews
        return mViews.root
    }

    private fun handleRefreshList(context: Context){
        refreshAdapterList(viewModel.screen4Display(loadItem))
        if (loadedItems.isLoading(loadItem)) return
        ApiTaskManager.join {
            try{
                for (index in viewModel.apps4DisplayValue.indices){
                    if (index == 15) ApiTaskManager.now(Dispatchers.Main) {
//                        refreshLayout.isRefreshing = false todo
                    }
//                                if (i >= EasyAccess.preloadLimit) continue
                    if (index >= viewModel.apps4DisplayValue.size) break
                    val app = viewModel.apps4DisplayValue[index]
                    if (app.preload) {
                        val applicationInfo: ApplicationInfo? = MiscApp.getApplicationInfo(context, packageName = app.packageName)
                        applicationInfo ?: continue
                        app.load(context, applicationInfo)
                        ApiTaskManager.now(Dispatchers.Main) {
                            adapter.notifyListItemChanged(index + adapter.indexOffset)
                        }
                    }
                }
//                ApiTaskManager.now(Dispatchers.Main) {
//                    refreshLayout.isRefreshing = false todo
//                }
                val iterator4Cache = viewModel.apps4Cache.iterator()
                while (iterator4Cache.hasNext()){
                    val app = iterator4Cache.next()
                    if (app.preload) {
                        val applicationInfo: ApplicationInfo? = MiscApp.getApplicationInfo(context, packageName = app.packageName)
                        applicationInfo ?: continue
                        app.load(context, applicationInfo)
                    }
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun ceaseRefresh(unit: Int){
//        if (loadItem == unit) refreshLayout.isRefreshing = false todo
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        val mViews = mViews ?: return

//        refreshLayout.isRefreshing = true todo
        pm = context.packageManager

        settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)

        EasyAccess.isSweet = settingsPreferences.getBoolean(
                getString(R.string.avSweet),
                resources.getBoolean(R.bool.avSweetDefault)
        )
        EasyAccess.shouldIncludeDisabled = settingsPreferences.getBoolean(
                getString(R.string.avIncludeDisabled),
                resources.getBoolean(R.bool.avIncludeDisabledDefault)
        )

        adapter = APIAdapter(context)

        manager = WearableLinearLayoutManager(context)
        mViews.avRecycler.let {
            it.layoutManager = manager
            it.isNestedScrollingEnabled = false
            it.adapter = adapter
            // this adds extra padding to make 1st and last items center vertically of the screen,
            // unfortunately it (setting recycler padding too) disables scrollbar for unknown reason,
            // so we manually add padding in sandwich adapter instead
//            it.isEdgeItemsCenteringEnabled = true
        }
        mViews.root.post {
            val listPadding = when {
                resources.configuration.isScreenRound ->
                    (mViews.root.height / 2) - X.size(context, 30f, X.DP).toInt()
                else -> X.size(context, 12f, X.DP).toInt()
            }
            adapter.topCover = listPadding
            adapter.bottomCover = listPadding
        }
        viewModel.apps4Display.observe(viewLifecycleOwner){
            adapter.apps = it
        }

        sortItem = settingsPreferences.getInt("SDKCheckSortSpinnerSelection1", SORT_POSITION_API_TIME)
        rSort = RunnableSort(sortItem)
        adapter.setSortMethod(sortItem)

//        spDisplayMethod = apiSpinnerDisplay
//        val displayMethod = arrayOf(
//                getString(R.string.sdkcheck_displayspinner_user),
//                getString(R.string.apiDisplaySys),
//                getString(R.string.sdkcheck_displayspinner_usersystem),
//                getString(R.string.apiDisplayAPK),
//                getString(R.string.apiDisplayFile),
//                getString(R.string.apiDisplayVolume)
//        )
//        spDisplayMethod.adapter = ArrayAdapter(context, R.layout.pop_list_item, displayMethod)
//        spDisplayMethod.onItemSelectedListener = this
        displayItem = settingsPreferences.getInt("SDKCheckDisplaySpinnerSelection", 0)
        rDisplay = RunnableDisplay(displayItem)
//        spDisplayMethod.setSelection(displayItem)

        val dp5 = X.size(context, 5f, X.DP).toInt()
        mainViewModel.insetBottom.observe(viewLifecycleOwner) {
            listInsetBottom = if (it == 0) dp5 * 7 else it
            adapter.listInsetBottom = listInsetBottom
        }
    }

    private fun refreshVisibleItems(){
        val first = (manager.findFirstVisibleItemPosition() - 3).run {
            if (this < 0) 0 else this
        }
        val last = (manager.findLastVisibleItemPosition() + 4).run {
            if (this > adapter.itemCount - 1) adapter.itemCount - 1 else this
        }
        adapter.notifyListItemRangeChanged(first, last - first + 1)
    }

    override fun onResume() {
        super.onResume()
        val context = context ?: return
        if (loadedItems.shouldLoad(ApiUnit.ALL_APPS)){
            loadedItems.finish(ApiUnit.ALL_APPS)
            viewModel.addAllApps(context)
            viewModel.sortApps(SORT_POSITION_API_NAME)
            ApiTaskManager.join(task = rSort)
        }else{
            refreshVisibleItems()
        }
    }

    fun onPageVisible() {
        val mViews = mViews ?: return
        // request focus to support rotary input
        mViews.avRecycler.run { post { requestFocus() } }
    }

    private fun refreshList() {
        ApiTaskManager.now {
            viewModel.screenOut(loadItem)
//            viewModel.updateApps4Display()
            loadedItems.unLoad(loadItem)
            loadSortedList(loadItem, sortEfficiently = true, fg = true)
            context?.let { handleRefreshList(it) }
        }
    }

    private fun refreshAdapterList(list: List<ApiViewingApp> = viewModel.apps4Cache) {
        viewModel.updateApps4Display(list)
        ApiTaskManager.now(Dispatchers.Main) {
            manager.scrollToPosition(0)
        }
    }

    /**
     * @param sortEfficiently sort or not
     * @param fg do foreground loading (do sorting if needed, while no sorting as background)
     */
    private fun loadSortedList(item: Int , sortEfficiently: Boolean , fg: Boolean ) {
        val context = context ?: return

        if (!loadedItems.shouldLoad(item)) {
            if (!sortEfficiently && fg) {
                viewModel.sortApps(sortItem)
                viewModel.updateApps4Display()
            }
            return
        }

        when (item){
            ApiUnit.USER -> {
                loadedItems.loading(item)
                viewModel.addUserApps(context)
                loadedItems.finish(item)
            }
            ApiUnit.SYS -> {
                loadedItems.loading(item)
                viewModel.addSystemApps(context)
                loadedItems.finish(item)
            }
            ApiUnit.ALL_APPS -> {
                val bUser = loadedItems.shouldLoad(ApiUnit.USER)
                val bSys = loadedItems.shouldLoad(ApiUnit.SYS)
                loadedItems.loading(item)
                if (bUser && bSys){
                    viewModel.addAllApps(context)
                }else if (bUser){
                    viewModel.addUserApps(context)
                }else if (bSys){
                    viewModel.addSystemApps(context)
                }
                loadedItems.finish(item)
            }
            ApiUnit.NON -> return
        }

        val item2Load = loadedItems.item2Load()
        val accomplished = item2Load == ApiUnit.NON
        if (accomplished || fg) {
            if (fg) viewModel.sortApps(sortItem)
            if (accomplished) return
        }
        ApiTaskManager.now { loadSortedList(item2Load, sortEfficiently = true, fg = false) }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long )  {
        if (parent === spDisplayMethod){
//            refreshLayout.isRefreshing = true todo
            displayItem = position
            when (position){
                DISPLAY_APPS_USER -> ApiUnit.USER
                DISPLAY_APPS_SYSTEM -> ApiUnit.SYS
                DISPLAY_APPS_ALL -> ApiUnit.ALL_APPS
                else -> null
            }?.let { loadItem = it }
            rDisplay.position = position
            ApiTaskManager.join(task = rDisplay)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>)  {

    }

    override fun onMenuItemClick(item: MenuItem): Boolean  {
//        refreshLayout.isRefreshing = true todo
//        when (item.itemId){
//            R.id.menuApiSortAPIL ->
//                sortItem = SORT_POSITION_API_LOW
//            R.id.menuApiSortAPIH ->
//                sortItem = SORT_POSITION_API_HIGH
//            R.id.menuApiSortAPIName ->
//                sortItem = SORT_POSITION_API_NAME
//            R.id.menuApiSortAPITime ->
//                sortItem = SORT_POSITION_API_TIME
//        }
        item.isChecked = true
        rSort.position = sortItem
        ApiTaskManager.join(task = rSort)
        return true
    }

    private interface RunnableAPI{
        var position: Int
    }

    private inner class RunnableSort(override var position: Int) : Runnable, RunnableAPI {
        override fun run()  {
            adapter.setSortMethod(position)
            settingsPreferences.edit { putInt("SDKCheckSortSpinnerSelection1", position) }
            viewModel.sortApps(sortItem)
            context?.let { handleRefreshList(it) }
        }
    }

    private inner class RunnableDisplay(override var position: Int) : Runnable, RunnableAPI {
        override fun run()  {
            settingsPreferences.edit { putInt("SDKCheckDisplaySpinnerSelection", position) }
            loadSortedList(loadItem, sortEfficiently = true, fg = true)
            viewModel.sortApps(sortItem)
            context?.let { handleRefreshList(it) }
        }
    }
}

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

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.core.content.res.use
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.checkbox.MaterialCheckBox
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.main.MyHideBottomViewOnScrollBehavior
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.unit.api_viewing.util.SheetUtil
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.fragment_api.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import com.madness.collision.unit.api_viewing.R as MyR

class MyUnit: com.madness.collision.unit.Unit() {

    override val id: String = "AV"

    companion object {
//        const val ARG_INTENT = "intent"
        const val EXTRA_DATA_STREAM = AccessAV.EXTRA_DATA_STREAM

        const val HANDLE_DISPLAY_APK = AccessAV.HANDLE_DISPLAY_APK

        const val DISPLAY_APPS_USER: Int = 0
        const val DISPLAY_APPS_SYSTEM: Int = 1
        const val DISPLAY_APPS_ALL: Int = 2
        const val DISPLAY_APPS_APK: Int = 3
        const val DISPLAY_APPS_SELECT: Int = 4
        const val DISPLAY_APPS_VOLUME: Int = 5

//        private val TAG = ApiFragment::class.java.simpleName
        private const val REQUEST_OPEN_DIRECTORY = 2
        private const val REQUEST_OPEN_VOLUME = 3
        private const val REQUEST_EXTERNAL_STORAGE: Int  = 0
        private const val REQUEST_EXTERNAL_STORAGE_RE: Int  = 1
        private const val INTENT_GET_FILE: Int  = 6

        const val SORT_POSITION_API_LOW: Int  = 0
        const val SORT_POSITION_API_HIGH: Int  = 1
        const val SORT_POSITION_API_NAME: Int  = 2
        const val SORT_POSITION_API_TIME: Int  = 3

        const val EXTRA_LAUNCH_MODE: String  = AccessAV.EXTRA_LAUNCH_MODE
        const val LAUNCH_MODE_SEARCH: Int  = AccessAV.LAUNCH_MODE_SEARCH
        /**
         * from url link sharing
         */
        const val LAUNCH_MODE_LINK: Int  = AccessAV.LAUNCH_MODE_LINK

        const val APP_CACHE_PREFIX = "BoundoApp4Cache"
    }

    private val viewModel: ApiViewingViewModel by activityViewModels()

    // data
    private var extras: Bundle? = null
    private var mode = 0
    private var sortItem: Int = SORT_POSITION_API_TIME
    private var displayItem: Int = DISPLAY_APPS_USER
    private val loadedItems: ApiUnit
        get() = viewModel.loadedItems
    private var loadItem: Int = ApiUnit.NON
    private var floatTop: Int = 0
    // 4 text watcher in search function
    private var sOri: String = ""
    private var tbDrawable: Drawable? = null
    private var iconColor = 0
    private lateinit var rSort: RunnableSort
    private lateinit var rDisplay: RunnableDisplay

    // views
    lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var adapter: APIAdapter
    // set as null when hidden, so as to fix anchor problem with toolbar
    private var popSort: PopupMenu? = null
    private var popSrc: PopupMenu? = null
//    private var popFilterTag: PopupWindow? = null
    private val antiSelectedIndexes = mutableSetOf<Int>()

    // context related
    private lateinit var pm: PackageManager
    private lateinit var settingsPreferences: SharedPreferences
    private lateinit var toolbar: Toolbar
    private lateinit var listFragment: AppListFragment

    private var searchBackPressedCallback: OnBackPressedCallback? = null

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        val textRes = if (EasyAccess.isViewingTarget) MyR.string.sdkcheck_dialog_targetsdktext else MyR.string.sdkcheck_dialog_minsdktext
        val titleAffix = getString(textRes)
        toolbar.title = getString(R.string.apiViewer) + " • $titleAffix"
        inflateAndTint(MyR.menu.toolbar_api, toolbar, iconColor)
        toolbar.setOnClickListener {
            scrollToTop()
            loadAppIcons()
            clearBottomAppIcons()
        }
        toolbar.background.mutate().constantState?.newDrawable()?.let { tbDrawable = it }
        this.iconColor = iconColor
        this.toolbar = toolbar
        // After image operations, cache is cleared, leaving list empty.
        // Ensures list refreshes after user comes back.
        if (loadedItems.isVacant && viewModel.apps4Cache.isEmpty()) {
            refreshLayout.isRefreshing = true
            refreshList()
        }
        return true
    }

    private val mScrollBehavior: MyHideBottomViewOnScrollBehavior<View>?
        get() {
            val params = apiDisplay.layoutParams as CoordinatorLayout.LayoutParams
            return params.behavior as MyHideBottomViewOnScrollBehavior
        }

    private fun scrollToTop() {
        if (!isAdded) return
        listFragment.scrollToTop()
        if (apiSpinnerDisplayBack.visibility == View.GONE) return
        mScrollBehavior?.slideUp(apiDisplay)
    }

    override fun selectOption(item: MenuItem): Boolean {
        when(item.itemId){
            MyR.id.apiTBRefresh -> {
                refreshLayout.isRefreshing = true
                if (needPermission(REQUEST_EXTERNAL_STORAGE_RE)) return true
                refreshList()
                return true
            }
            MyR.id.apiTBSearch -> {
                val searchView = item.actionView as SearchView
                val displayList = adapter.apps
                searchView.queryHint = getText(R.string.sdk_search_hint)
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        val context = context ?: return true
                        val window = activity?.window ?: return true
                        SystemUtil.hideImeCompat(context, searchView, window)
                        searchView.clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val sAft: String = newText?.replace(" ", "") ?: ""
                        if (sOri.compareTo(sAft, true) == 0){
                            return true
                        }
                        refreshLayout.isRefreshing = true
                        if (sAft.isEmpty()) {
                            context?.let { context -> ApiTaskManager.now { handleRefreshList(context) } }
                        }else {
                            val isAddition = sAft.startsWith(sOri)
                            getFilter(isAddition, displayList).filter(sAft)
                        }
                        sOri = sAft
                        return true
                    }
                })
                ensureSearchActionCollapse(item)
                return true
            }
            MyR.id.apiTBSort -> {
                if (popSort == null) {
                    // when in overflow menu, menu item has no icon button
                    // thus anchor would be null
                    val anchor = toolbar.findViewById<View>(MyR.id.apiTBSort) ?: return false
                    popSort = PopupMenu(context, anchor).apply {
                        if (X.aboveOn(X.Q)) {
                            setForceShowIcon(true)
                        }
                        inflate(MyR.menu.api_sort)
                        setOnMenuItemClickListener {
                            clickSortItem(it)
                        }
                        menu.getItem(sortItem).isChecked = true
                    }
                }
                popSort!!.show()
                return true
            }
            MyR.id.apiTBSettings -> {
                val settings = MyBridge.getSettings() ?: return false
                mainViewModel.displayFragment(settings)
                return true
            }
            MyR.id.apiTBManual -> {
                val context = context ?: return false
                CollisionDialog.alert(context, MyR.string.avManual).show()
                return true
            }
            MyR.id.apiTBViewingTarget -> {
                EasyAccess.isViewingTarget = !EasyAccess.isViewingTarget
                settingsPreferences.edit { putBoolean(PrefUtil.AV_VIEWING_TARGET, EasyAccess.isViewingTarget) }
                val textRes = if (EasyAccess.isViewingTarget) MyR.string.sdkcheck_dialog_targetsdktext else MyR.string.sdkcheck_dialog_minsdktext
                val titleAffix = getString(textRes)
                toolbar.title = getString(R.string.apiViewer) + " • $titleAffix"
                adapter.notifyDataSetChanged()
                return true
            }
            MyR.id.avMainTbShare -> {
                val context = context ?: return false
                exportList(context)
                return true
            }
        }
        return false
    }

    private fun exportList(context: Context) {
        GlobalScope.launch {
            val path = F.createPath(F.cachePublicPath(context), "Temp", "AV", "AppList.csv")
            val file = File(path)
            if (!F.prepare4(file)) return@launch
            SheetUtil.csvWriterAll(getList(context), file)
            launch(Dispatchers.Main) {
                FilePop.by(context, file, "text/csv", R.string.fileActionsShare)
                        .show(childFragmentManager, FilePop.TAG)
            }
        }
    }

    private fun getList(context: Context): List<Array<String>> {
//        val list = adapter.apps
//        val re = ArrayList<Array<String>>(list.size + 1)
//        re.add(arrayOf(context.getString(R.string.apiDetailsPackageName)))
//        list.forEach {
//            re.add(arrayOf(it.name))
//        }
//        return re
        return adapter.apps.map {
            arrayOf(it.name)
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
                searchBackPressedCallback?.remove()
                searchBackPressedCallback = null
                return true
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context ?: return
        val colorTb = ThemeUtil.getColor(context, R.attr.colorTb)
        if (tbDrawable == null) tbDrawable = ColorDrawable(colorTb)

        settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        // notify on Android O
        if (!settingsPreferences.getBoolean(PrefUtil.AV_INIT_NOTIFY, PrefUtil.AV_INIT_NOTIFY_DEFAULT)){
            settingsPreferences.edit { putBoolean(PrefUtil.AV_INIT_NOTIFY, true) }
            notify(R.string.api_viewer_initialize)
        }

        EasyAccess.init(context, settingsPreferences)

        listFragment = AppListFragment.newInstance()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            popSort = null
            // remove this callback otherwise app cannot go back
            searchBackPressedCallback?.remove()
            searchBackPressedCallback = null
        } else {
            // check settings
            GlobalScope.launch {
                val context = context ?: return@launch
                val isChanged = EasyAccess.load(context, settingsPreferences, false)
                if (isChanged) {
                    delay(1)
                    launch(Dispatchers.Main) {
                        avMainFilterText.text = null
                        avMainFilterText.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        super.onHiddenChanged(hidden)
    }

    override fun onStop() {
        searchBackPressedCallback?.remove()
        searchBackPressedCallback = null
        ApiTaskManager.cancelAll()
        super.onStop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(MyR.layout.fragment_api, container, false)
    }

    private fun handleRefreshList(context: Context){
        refreshAdapterList(viewModel.screen4Display(loadItem))
        if (loadedItems.isLoading(loadItem)) return
    }

    private fun loadAppIcons() {
        ApiTaskManager.join {
            try{
                for (index in viewModel.apps4DisplayValue.indices){
                    if (index >= EasyAccess.preloadLimit) break
                    if (index >= viewModel.apps4DisplayValue.size) break
                    adapter.ensureItem(index, refreshLayout)
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun clearBottomAppIcons() {
        ApiTaskManager.join {
            try{
                var index = viewModel.apps4DisplayValue.size - 1
                val cacheSize = EasyAccess.loadLimitHalf * 2 + 10
                while (index >= cacheSize){
                    val app = viewModel.apps4DisplayValue[index]
                    if (!app.preload) app.clearIcons()
                    index--
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun updateCacheSize() {
        val manager = listFragment.getLayoutManager() as LinearLayoutManager
        val unitSize = manager.findLastVisibleItemPosition() - manager.findFirstVisibleItemPosition()
        val cacheSize = if (unitSize < 20) (30 + unitSize * 10) else (100 + unitSize * 7)
        EasyAccess.loadLimitHalf = cacheSize
        EasyAccess.loadAmount = unitSize
        EasyAccess.preloadLimit = EasyAccess.loadLimitHalf - EasyAccess.loadAmount
    }

    private fun displayApk(context: Context, apkPath: String){
        if (loadItem == ApiUnit.APK) {
            viewModel.addFile(context, apkPath)
            viewModel.sortApps(sortItem)
            refreshAdapterList(viewModel.screen4Display(loadItem))
        } else {
            ApiTaskManager.now { viewModel.addFile(context, apkPath) }
        }
    }

    private fun displayFile(context: Context, uri: Uri){
        if (loadItem == ApiUnit.APK || loadItem == ApiUnit.SELECTED || loadItem == ApiUnit.VOLUME || mode == LAUNCH_MODE_LINK || loadItem == ApiUnit.DISPLAY) {
            viewModel.addFile(context, uri)
            viewModel.sortApps(sortItem)
            refreshAdapterList(viewModel.screen4Display(loadItem))
        } else {
            ApiTaskManager.now { viewModel.addFile(context, uri) }
        }
    }

    /**
     * from text processing activity, app list is empty
     */
    private fun displaySearch(context: Context, text: String){
        if (text.isEmpty()) return
        val installedApps: List<PackageInfo> = pm.getInstalledPackages(0)
        val apps = mutableListOf<ApiViewingApp>()
        for (appInfo in installedApps) {
            val label = appInfo.applicationInfo.loadLabel(pm)
            if (label.contains(text, true)) {
                apps.add(ApiViewingApp(context, appInfo, preloadProcess = true, archive = false))
            }
        }
        viewModel.addApps(apps)
        viewModel.sortApps(sortItem)
        handleRefreshList(context)
    }

    /**
     * ordinary search, filter app list
     */
    private fun displaySearchFilter(text: String){
        getFilter(false, adapter.apps).filter(text)
    }

    private fun ceaseRefresh(unit: Int){
        if (loadItem != unit) return
        ApiTaskManager.now(Dispatchers.Main) {
            refreshLayout.isRefreshing = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return

        refreshLayout = apiSwipeRefresh
        refreshLayout.isRefreshing = true
        pm = context.packageManager

        ensureAdded(MyR.id.avViewListContainer, listFragment, true)
        adapter = listFragment.getAdapter()
        viewModel.apps4Display.observe(viewLifecycleOwner){
            adapter.apps = it
            listFragment.getRecyclerView().post {
                updateCacheSize()
                loadAppIcons()
            }
        }
        // todo use RecyclerView 1.2 to restore scroll position
        // myAdapter.setStateRestorationStrategy(StateRestorationStrategy.WHEN_NOT_EMPTY);

        sortItem = settingsPreferences.getInt(PrefUtil.AV_SORT_ITEM, PrefUtil.AV_SORT_ITEM_DEFAULT)
        rSort = RunnableSort(sortItem)
        adapter.setSortMethod(sortItem)

        extras = arguments
        extras?.run {
            mode = if (getInt(EXTRA_LAUNCH_MODE, 0) == LAUNCH_MODE_LINK){
                // below: from share action
                LAUNCH_MODE_LINK
            }else {
                // below: from text processing activity or text sharing
                LAUNCH_MODE_SEARCH
            }
            apiSpinnerDisplayBack.visibility = View.GONE
            ApiTaskManager.now {
                loadSortedList(ApiUnit.NON, sortEfficiently = true, fg = true)
            }
        }

        democratize()
        val dp5 = X.size(context, 5f, X.DP).toInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            val listInsetTop: Int
            when (mode) {
                LAUNCH_MODE_SEARCH -> {
                    floatTop = 0
                    listInsetTop = it
                }
                LAUNCH_MODE_LINK -> {
                    floatTop = it
                    listInsetTop = floatTop + dp5
                }
                else -> {
                    apiDisplay.run { alterPadding(top = it) }
                    apiSpinnerDisplayBack.measure()
                    floatTop = apiSpinnerDisplayBack.measuredHeight + it + dp5 * 2
                    listInsetTop = floatTop + dp5
                }
            }
            refreshLayout.setProgressViewOffset(false, listInsetTop + 2 * dp5, listInsetTop + 7 * dp5)
            adapter.topCover = listInsetTop
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            adapter.bottomCover = if (it == 0) dp5 * 7 else it
        }

        refreshLayout.setOnRefreshListener(this::refreshList)

        MainActivity.syncScroll(mScrollBehavior)

        if (mode != LAUNCH_MODE_SEARCH && mode != LAUNCH_MODE_LINK){
            displayItem = settingsPreferences.getInt(PrefUtil.AV_LIST_SRC_ITEM, PrefUtil.AV_LIST_SRC_ITEM_DEFAULT)
            popSrc = PopupMenu(context, apiSpinnerDisplayBack).apply {
                if (X.aboveOn(X.Q)) {
                    setForceShowIcon(true)
                }
                inflate(MyR.menu.av_list_src)
                setOnMenuItemClickListener {
                    clickListSrcItem(it)
                }
            }
            avListSrc.setOnClickListener {
                popSrc?.show()
            }
            rDisplay = RunnableDisplay(displayItem)
            val filterTags = context.resources.obtainTypedArray(MyR.array.prefAvTagsEntries)
            val popTags = PopupUtil.selectMulti(context, MyR.string.av_main_filter_tip, filterTags, emptySet()) {
                pop, container, indexes ->
                pop.dismiss()
                closeFilterTagMenu(container, indexes)
            }
            val container: ViewGroup = popTags.findViewById(R.id.popupSelectMultiContainer)
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
//            val width = ViewGroup.LayoutParams.WRAP_CONTENT
//            popFilterTag = PopupWindow(filterTagDelegate, width, width).apply {
//                setOnDismissListener {
//                }
//            }
            avMainFilterContainer.setOnClickListener {
//                popTags?.showAsDropDown(avMainFilterCard)
                popTags.show()
            }
            X.curvedCard(apiStatsBack)
            X.curvedCard(avMainFilterCard)
            X.curvedCard(apiSpinnerDisplayBack)
        }

        if (X.aboveOn(X.N)){
            val apkDisplayDragListener = object : View.OnDragListener{
                var permission: DragAndDropPermissions? = null
                override fun onDrag(v: View?, event: DragEvent?): Boolean {
                    event ?: return false
                    when (event.action) {
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            notifyBriefly(R.string.apiDragDropHint)
                        }
                        DragEvent.ACTION_DROP -> {
                            permission = activity?.requestDragAndDropPermissions(event)
                            dragDropAction(context, event.clipData)
                        }
                        DragEvent.ACTION_DRAG_ENDED -> permission?.release()
                    }
                    return true
                }
            }
            apiContainer.setOnDragListener(apkDisplayDragListener)
        }
    }

    override fun onStart() {
        super.onStart()
        // attention: these statements will be invoked after screen off and on,
        // where data is loaded already
        if (mode != LAUNCH_MODE_SEARCH && mode != LAUNCH_MODE_LINK) {
            // fix refreshing animation not shown
            GlobalScope.launch {
                delay(1)
                launch(Dispatchers.Main) {
                    selectListSrcItem(displayItem)
                }
            }
        }
        if (mode == LAUNCH_MODE_SEARCH && extras != null) {
            // fix refreshing animation not shown
            // invoke only the first time
            if (loadedItems.isBusy && viewModel.apps4Cache.isEmpty()) {
                GlobalScope.launch {
                    delay(1)
                    launch(Dispatchers.Main) {
                        refreshLayout.isRefreshing = true
                    }
                }
            }
        }
    }

    private fun clickSortItem(item: MenuItem): Boolean  {
        refreshLayout.isRefreshing = true
        when (item.itemId){
            MyR.id.menuApiSortAPIL ->
                sortItem = SORT_POSITION_API_LOW
            MyR.id.menuApiSortAPIH ->
                sortItem = SORT_POSITION_API_HIGH
            MyR.id.menuApiSortAPIName ->
                sortItem = SORT_POSITION_API_NAME
            MyR.id.menuApiSortAPITime ->
                sortItem = SORT_POSITION_API_TIME
        }
        item.isChecked = true
        // clear tag filter
        val context = context
        if (context != null) {
            clearTagFilter(context)
        }
        rSort.position = sortItem
        ApiTaskManager.join(task = rSort)
        return true
    }

    /**
     * Select an item
     * Update menu item selection and invoke the corresponding callback
     */
    private fun selectListSrcItem(item: Int) {
        popSrc?.menu?.getItem(item)?.let {
            // avoid duplicate refresh
            if (it.isChecked) return
            clickListSrcItem(it)
        }
    }

    private fun clickListSrcItem(item: MenuItem): Boolean {
        refreshLayout.isRefreshing = true
        avListSrc.text = item.title
        item.isChecked = true
        var isStatsAvailable = false
        when (item.itemId){
            MyR.id.avListSrcUsr -> {
                displayItem = DISPLAY_APPS_USER
                isStatsAvailable = true
                ApiUnit.USER
            }
            MyR.id.avListSrcSys -> {
                displayItem = DISPLAY_APPS_SYSTEM
                isStatsAvailable = true
                ApiUnit.SYS
            }
            MyR.id.avListSrcAll -> {
                displayItem = DISPLAY_APPS_ALL
                isStatsAvailable = true
                ApiUnit.ALL_APPS
            }
            MyR.id.avListSrcDeviceApk -> {
                displayItem = DISPLAY_APPS_APK
                ApiUnit.APK
            }
            MyR.id.avListSrcCustom -> {
                displayItem = DISPLAY_APPS_SELECT
                if (loadItem == ApiUnit.DISPLAY) ApiUnit.DISPLAY
                else ApiUnit.SELECTED
            }
            MyR.id.avListSrcVolume -> {
                displayItem = DISPLAY_APPS_VOLUME
                ApiUnit.VOLUME
            }
            else -> null
        }?.let { loadItem = it }
        if (needPermission(REQUEST_EXTERNAL_STORAGE)) return true
        // clear tag filter
        val context = context
        if (context != null) {
            clearTagFilter(context)
        }
        rDisplay.position = displayItem
        ApiTaskManager.join(task = rDisplay)
        ApiTaskManager.now(Dispatchers.Main){
            val listener = if (isStatsAvailable) View.OnClickListener {
                mainViewModel.displayFragment(StatisticsFragment.newInstance(loadItem))
            } else null
            avMainStatsContainer.setOnClickListener(listener)
        }
        return true
    }

    private fun closeFilterTagMenu(container: ViewGroup, checkedIndexes: Set<Int>) {
        val context = context ?: return
        refreshLayout.isRefreshing = true
        GlobalScope.launch {
            var singleTitle: CharSequence? = null
            val value = context.resources.obtainTypedArray(MyR.array.prefAvTagsValues).use {
                values ->
                checkedIndexes.associate {
                    val name = values.getString(it) ?: ""
                    val isAntied = it in antiSelectedIndexes
                    name to TriStateSelectable(name, !isAntied)
                }
            }
            if (checkedIndexes.size == 1) {
                val checkedItem = container[checkedIndexes.first()] as MaterialCheckBox
                singleTitle = checkedItem.text
            }
            launch(Dispatchers.Main) {
                if (value.isEmpty()) {
                    avMainFilterText.text = null
                    avMainFilterText.visibility = View.GONE
                    avMainFilterContainer.setOnLongClickListener(null)
                } else {
                    if (value.size == 1) {
                        avMainFilterText.text = singleTitle?.toString()
                        avMainFilterText.visibility = View.VISIBLE
                        if (checkedIndexes.first() in antiSelectedIndexes) {
                            avMainFilterText.paintFlags = avMainFilterText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        } else {
                            avMainFilterText.paintFlags = avMainFilterText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                    } else {
                        avMainFilterText.text = null
                        avMainFilterText.visibility = View.GONE
                    }
                    avMainFilterContainer.setOnLongClickListener {
                        clearTagFilter(context)
                        refreshAdapterList(viewModel.screen4Display(loadItem))
                        true
                    }
                }
            }
            // cannot detect whether changed, previous state cannot be determined
            // because filter state share data with normal state
            AppTag.loadTagSettings(context, value, false)
            val completeList = viewModel.screen4Display(loadItem)
            val displayList = if (value.isEmpty()) completeList
            else filterByTag(context, this, completeList)
            launch(Dispatchers.Main) {
                refreshAdapterList(displayList)
                refreshLayout.isRefreshing = false
            }
        }
    }

    private suspend fun filterByTag(context: Context, scope: CoroutineScope, appList: List<ApiViewingApp>): List<ApiViewingApp> {
        return withContext(scope.coroutineContext) {
            appList.filter {
                AppTag.filterTags(context, it)
            }
        }
    }

    private fun clearTagFilter(context: Context) {
        avMainFilterText.text = null
        avMainFilterText.visibility = View.GONE
        AppTag.loadTagSettings(context, settingsPreferences, false)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        val context = context ?: return
        X.toast(context, "out of memory", Toast.LENGTH_LONG)
    }

    private fun refreshList() {
        ApiTaskManager.now {
            ApiInfoPop.clearStores()
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
            apiStats?.text = adapter.listCount.toString()
            apiStats?.visibility = View.VISIBLE
            scrollToTop()
        }
    }

    /**
     * @param sortEfficiently sort or not
     * @param fg do foreground loading (do sorting if needed, while no sorting as background)
     */
    private fun loadSortedList(item: Int , sortEfficiently: Boolean , fg: Boolean ) {
        val context = context ?: return

        if (mode == LAUNCH_MODE_SEARCH && extras != null) {
            // load as ApiUnit.ALL_APPS,
            // because loadedItems is used to check loading and avoid repeated loading
            if (!loadedItems.shouldLoad(ApiUnit.ALL_APPS)) return
            loadedItems.loading(ApiUnit.ALL_APPS)
            val text = extras!!.getString(Intent.EXTRA_TEXT) ?: ""
            displaySearch(context, text)
            loadedItems.finish(ApiUnit.ALL_APPS)
            return
        }

        if (mode == LAUNCH_MODE_LINK && extras != null) {
            val reIntent = Intent().apply {
                data = this@MyUnit.extras!!.getParcelable(EXTRA_DATA_STREAM)
            }
            onActivityResult(INTENT_GET_FILE, AppCompatActivity.RESULT_OK, reIntent)
            return
        }

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
            ApiUnit.APK -> {
                loadedItems.loading(item)
                ApiTaskManager.now {
                    if (X.aboveOn(X.Q)) {
                        accessiblePrimaryExternal(context) {
                            displayApkFromTreeDocumentUriQuery(context, it)
                        }
                    } else {
                        try {
                            val external: File = Environment.getExternalStorageDirectory()
                            X.listFiles4API(object : Handler(Looper.getMainLooper()){
                                override fun handleMessage(msg: Message) {
                                    super.handleMessage(msg)
                                    if (msg.what == HANDLE_DISPLAY_APK) {
                                        val apkPath = msg.obj as String
                                        displayApk(context, apkPath)
                                    }
                                }
                            }, external)
                        } catch (e: Exception){ e.printStackTrace() }
                    }
                }.invokeOnCompletion {
                    ceaseRefresh(ApiUnit.APK)
                    loadedItems.finish(item)
                }
            }
            ApiUnit.SELECTED -> {
                loadedItems.loading(item)
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.type = "application/vnd.android.package-archive"
                startActivityForResult(intent, INTENT_GET_FILE)
            }
            ApiUnit.VOLUME -> {
                loadedItems.loading(item)
                startActivityForResult(
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            // make it possible to access children
                            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        REQUEST_OPEN_VOLUME
                )
            }
            ApiUnit.NON -> return
        }

        val item2Load = loadedItems.item2Load(settingsPreferences)
        val accomplished = item2Load == ApiUnit.NON
        if (accomplished || fg) {
            if (fg) viewModel.sortApps(sortItem)
            if (accomplished) return
        }
        if (item2Load == ApiUnit.APK){
            if (X.aboveOn(X.Q)){
//                if (!accessiblePrimaryExternal(context, null)) return
                return // prohibit background loading due to storage consumption issue
            }else{
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (PermissionUtil.check(context, permissions).isNotEmpty()) return
            }
        }
        ApiTaskManager.now { loadSortedList(item2Load, sortEfficiently = true, fg = false) }
    }

    private fun accessiblePrimaryExternal(context: Context, task: ((uri: Uri) -> Unit)?): Boolean{
        val uriExternalPrimary = settingsPreferences.getString(P.URI_SCANNING_FOLDER, "") ?: ""
        for (uriPermission in context.contentResolver.persistedUriPermissions) {
            val uriString = uriPermission.uri.toString()
            if (uriString != uriExternalPrimary) continue
            val uri = uriPermission.uri
            task?.invoke(uri)
            return true
        }
        return false
    }

    private fun getFilter(isAddition: Boolean, displayList: List<ApiViewingApp>): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val appList = if (isAddition) adapter.apps else displayList
                val filterResults = FilterResults()
                if (appList.isEmpty()) {
                    filterResults.count = 0
                    return filterResults
                }
                val filtered: MutableList<ApiViewingApp> = mutableListOf()
                val locale = SystemUtil.getLocaleApp()
                val filterText = charSequence.toString()
                val input4Comparision: String = filterText.toLowerCase(locale)
                val iterator: Iterator<ApiViewingApp> = appList.iterator()
                while (iterator.hasNext()){
                    val info = iterator.next()
                    val appName = info.name.replace(" ", "").toLowerCase(locale)
                    if (appName.contains(input4Comparision)
                            || info.packageName.toLowerCase(locale).contains(input4Comparision)) {
                        filtered.add(info)
                        continue
                    }
                    val ver = if (EasyAccess.isViewingTarget) VerInfo(info.targetAPI, info.targetSDK, info.targetSDKLetter)
                    else VerInfo(info.minAPI, info.minSDK, info.minSDKLetter)
                    if (filterText == ver.api.toString() || ver.sdk.startsWith(filterText)) {
                        filtered.add(info)
                    }
                }
                filterResults.values = filtered
                filterResults.count = filtered.size
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                val reValues = filterResults.values
                val re = if (filterResults.count != 0 && reValues is MutableList<*>) {
                    reValues.filterIsInstance<ApiViewingApp>()
                } else {
                    emptyList()
                }
                refreshAdapterList(re)
                refreshLayout.isRefreshing = false
            }
        }
    }

    private fun needPermission(requestCode: Int ): Boolean {
        val context = context ?: return false
        return if (displayItem == DISPLAY_APPS_APK){
            if (X.aboveOn(X.Q)) {
                var flagGranted = false
                accessiblePrimaryExternal(context){ flagGranted = true }
                if (flagGranted) return false
                startActivityForResult(
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            // make it possible to persist
                            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            // make it possible to access children
                            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        REQUEST_OPEN_DIRECTORY
                )
                X.toast(context, MyR.string.av_apks_select_folder, Toast.LENGTH_LONG)
            } else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (PermissionUtil.check(context, permissions).isNotEmpty()) {
                    if (X.aboveOn(X.M))
                        requestPermissions(permissions, requestCode)
                    else {
                        refreshLayout.isRefreshing = false
                        notify(R.string.toast_permission_storage_denied)
                    }
                }
            }
            true
        }else false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray)  {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            refreshLayout.isRefreshing = false
            notify(R.string.toast_permission_storage_denied)
            return
        }
        if (requestCode == REQUEST_EXTERNAL_STORAGE){
            rDisplay.position = displayItem
            ApiTaskManager.join(task = rDisplay)
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE_RE){
            refreshList()
        }
    }

    private interface RunnableAPI{
        var position: Int
    }

    private inner class RunnableSort(override var position: Int) : Runnable, RunnableAPI {
        override fun run()  {
            adapter.setSortMethod(position)
            settingsPreferences.edit { putInt(PrefUtil.AV_SORT_ITEM, position) }
            viewModel.sortApps(sortItem)
            context?.let { handleRefreshList(it) }
        }
    }

    private inner class RunnableDisplay(override var position: Int) : Runnable, RunnableAPI {
        override fun run()  {
            if (position != DISPLAY_APPS_SELECT && position != DISPLAY_APPS_VOLUME) {
                settingsPreferences.edit { putInt(PrefUtil.AV_LIST_SRC_ITEM, position) }
            }
            loadSortedList(loadItem, sortEfficiently = true, fg = true)
            viewModel.sortApps(sortItem)
            context?.let { handleRefreshList(it) }
        }
    }

    private fun dragDropAction(context: Context, clipData: ClipData?){
        ApiTaskManager.now {
            val cd: ClipData? = clipData
            if (cd == null) {
                notifyBriefly(R.string.text_no_content)
                return@now
            }
            val description = cd.description
            if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                loadItem = ApiUnit.DISPLAY
                ApiTaskManager.now(Dispatchers.Main) {
                    selectListSrcItem(DISPLAY_APPS_SELECT)
                }
            }
            val mimeType = description.getMimeType(0)
            items@ for (i in 0 until cd.itemCount) {
                val item = cd.getItemAt(i)
                val itemUri = item.uri
                when (mimeType) {
                    ClipDescription.MIMETYPE_TEXT_PLAIN -> {
                        displaySearchFilter(item.text.toString())
                    }
                    else -> {
                        if (!DocumentsContract.isDocumentUri(context, itemUri)){
                            displayFile(context, itemUri)
                            continue@items
                        }
                        displayApkFromDocumentUri(context, itemUri)
                    }
                }
            }
        }.invokeOnCompletion {
            ceaseRefresh(ApiUnit.DISPLAY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent? )  {
        super.onActivityResult(requestCode, resultCode, data)
        val context = context ?: return
        when (requestCode) {
            INTENT_GET_FILE -> {
                ApiTaskManager.now {
                    // Obtain file's uri from data.getData().
                    // If there are multiple files, achieve it by calling data.getClipData() instead.
                    val uri: Uri? = data?.data
                    val cd: ClipData? = data?.clipData
                    if (resultCode != AppCompatActivity.RESULT_OK || (uri == null && cd == null)) {
                        notify(R.string.text_no_content)
                        return@now
                    }
                    val pathsSize: Int = (if (uri != null) 1 else cd!!.itemCount)
                    if (uri != null) {
                        displayFile(context, uri)
                    } else {
                        for (i in 0 until pathsSize) {
                            displayFile(context, cd!!.getItemAt(i).uri)
                        }
                    }
                }.invokeOnCompletion {
                    ceaseRefresh(if (mode == LAUNCH_MODE_LINK) ApiUnit.NON else ApiUnit.SELECTED)
                }
            }
            REQUEST_OPEN_DIRECTORY -> {
                if (resultCode != Activity.RESULT_OK) {
                    notifyBriefly(R.string.text_no_content)
                    ceaseRefresh(ApiUnit.APK)
                    return
                }
                data?.data?.let {
                    ApiTaskManager.now {
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(it, takeFlags)
                        val uriString = it.toString()
                        settingsPreferences.edit { putString(P.URI_SCANNING_FOLDER, uriString) }
                        rDisplay.position = displayItem
                        ApiTaskManager.join(task = rDisplay)
                    }
                }
            }
            REQUEST_OPEN_VOLUME -> {
                if (resultCode != Activity.RESULT_OK) {
                    notifyBriefly(R.string.text_no_content)
                    ceaseRefresh(ApiUnit.VOLUME)
                    return
                }
                // todo
                data?.data?.let {
                    ApiTaskManager.now {
                        displayApkFromTreeDocumentUriQuery(context, it)
                    }.invokeOnCompletion {
                        ceaseRefresh(ApiUnit.VOLUME)
                    }
                }
            }
        }
    }

    private fun displayApkFromDocumentUri(context: Context, uri: Uri) {
        val doc = DocumentFile.fromSingleUri(context, uri)!!
        displayApkFromDocument(context, doc)
    }

    private fun displayApkFromDocument(context: Context, doc: DocumentFile, isTreeUriKnown: Boolean = false) {
        if (!doc.canRead()) return
        val docUri = doc.uri
        if (doc.isFile) {
            if (doc.type == "application/vnd.android.package-archive" && doc.name?.contains(APP_CACHE_PREFIX) != true) {
                displayFile(context, docUri)
            }
            return
        }

        if (!isTreeUriKnown) {
            val mTreeUri = if (X.aboveOn(X.N) && !DocumentsContract.isTreeUri(docUri)) {
                val docId = DocumentsContract.getDocumentId(docUri)
                DocumentsContract.buildTreeDocumentUri(docUri.authority, docId)
            } else {
                docUri
            }
            displayApkFromDocument(context, DocumentFile.fromTreeUri(context, mTreeUri)!!, true)
            return
        }
        for (childDoc in doc.listFiles()) {
            displayApkFromDocument(context, childDoc, true)
        }
    }

    /**
     * get apk from this uri
     */
    private fun displayApkFromTreeDocumentUriQuery(context: Context, treeUri: Uri, docId: String = DocumentsContract.getTreeDocumentId(treeUri)) {
//        if (!DocumentsContract.isDocumentUri(context, treeUri)) return
        val contentResolver = context.contentResolver ?: return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

        val columns = arrayOf(
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        val childCursor = contentResolver.query(childrenUri, columns, null, null, null)
        childCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(0) ?: ""
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val id = cursor.getString(1)
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, id)?.also {
                        displayApkFromTreeDocumentUriQuery(context, treeUri, id)
                    }
                    continue
                }
                if (mimeType != "application/vnd.android.package-archive") continue
                val id = cursor.getString(1)
                val name = cursor.getString(2)
                if (name.contains(APP_CACHE_PREFIX)) continue
                DocumentsContract.buildDocumentUriUsingTree(treeUri, id)?.also {
                    displayFile(context, it)
                }
            }
        }
    }
}

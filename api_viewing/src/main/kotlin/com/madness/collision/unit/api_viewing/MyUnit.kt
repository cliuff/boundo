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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.checkbox.MaterialCheckBox
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.main.MyHideBottomViewOnScrollBehavior
import com.madness.collision.misc.MiscApp
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.databinding.FragmentApiBinding
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.list.ApiInfoPop
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.stats.StatisticsFragment
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.*
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import com.madness.collision.unit.api_viewing.R as MyR

class MyUnit: com.madness.collision.unit.Unit() {

    override val id: String = "AV"

    companion object {
//        const val ARG_INTENT = "intent"

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

        const val STATE_KEY_LIST = "ListFragment"
    }

    class LaunchMethod(val mode: Int) {
        companion object {
            const val EXTRA_DATA_STREAM = AccessAV.EXTRA_DATA_STREAM
            const val EXTRA_LAUNCH_MODE = AccessAV.EXTRA_LAUNCH_MODE
            const val LAUNCH_MODE_NORMAL = 0
            const val LAUNCH_MODE_SEARCH = AccessAV.LAUNCH_MODE_SEARCH

            /**
             * from url link sharing
             */
            const val LAUNCH_MODE_LINK = AccessAV.LAUNCH_MODE_LINK
        }

        var textExtra: String? = null
            private set
        var dataStreamExtra: Uri? = null
            private set

        constructor(bundle: Bundle?) : this(when {
            bundle == null -> LAUNCH_MODE_NORMAL
            // below: from share action
            bundle.getInt(EXTRA_LAUNCH_MODE, LAUNCH_MODE_NORMAL) == LAUNCH_MODE_LINK -> LAUNCH_MODE_LINK
            // below: from text processing activity or text sharing
            else -> LAUNCH_MODE_SEARCH
        }) {
            when (mode) {
                LAUNCH_MODE_SEARCH -> textExtra = bundle?.getString(Intent.EXTRA_TEXT) ?: ""
                LAUNCH_MODE_LINK -> dataStreamExtra = bundle?.getParcelable(EXTRA_DATA_STREAM)
            }
        }
    }

    private val viewModel: ApiViewingViewModel by activityViewModels()

    // data
    private lateinit var launchMethod: LaunchMethod
    private var sortItem: Int = SORT_POSITION_API_TIME
    private var displayItem: Int = DISPLAY_APPS_USER
    private val loadedItems: ApiUnit
        get() = viewModel.loadedItems
    private var loadItem: Int = ApiUnit.NON
    private var floatTop: Int = 0
    private var tbDrawable: Drawable? = null
    private var iconColor = 0
    private lateinit var rSort: RunnableSort
    private lateinit var rDisplay: RunnableDisplay
    private val service = AppMainService()
    private lateinit var apkRetriever: ApkRetriever

    // views
    private val refreshLayout: SwipeRefreshLayout
        get() = viewBinding.apiSwipeRefresh
    private lateinit var adapter: APIAdapter
    // set as null when hidden, so as to fix anchor problem with toolbar
    private var popSort: PopupMenu? = null
    private var popSrc: PopupMenu? = null
//    private var popFilterTag: PopupWindow? = null
    private val antiSelectedIndexes = mutableSetOf<Int>()

    // context related
    private lateinit var settingsPreferences: SharedPreferences
    private lateinit var toolbar: Toolbar
    private lateinit var listFragment: AppListFragment
    private lateinit var viewBinding: FragmentApiBinding

    private var searchBackPressedCallback: OnBackPressedCallback? = null

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        val textRes = if (EasyAccess.isViewingTarget) MyR.string.sdkcheck_dialog_targetsdktext else MyR.string.sdkcheck_dialog_minsdktext
        val titleAffix = getString(textRes)
        toolbar.title = getString(R.string.apiViewer) + " • $titleAffix"
        inflateAndTint(MyR.menu.toolbar_api, toolbar, iconColor)
        toolbar.setOnClickListener {
            scrollToTop()
            listFragment.loadAppIcons(refreshLayout)
            listFragment.clearBottomAppIcons()
        }
        toolbar.background.mutate().constantState?.newDrawable()?.let { tbDrawable = it }
        this.iconColor = iconColor
        this.toolbar = toolbar
        // After image operations, cache is cleared, leaving list empty.
        // Ensures list refreshes after user comes back.
        if (loadedItems.isVacant && viewModel.apps4Cache.isEmpty()) {
            refreshLayout.isRefreshing = true
            reloadList()
        }
        return true
    }

    private val mScrollBehavior: MyHideBottomViewOnScrollBehavior<View>
        get() {
            val params = viewBinding.apiDisplay.layoutParams as CoordinatorLayout.LayoutParams
            return params.behavior as MyHideBottomViewOnScrollBehavior
        }

    private fun scrollToTop() {
        if (!isAdded) return
        listFragment.scrollToTop()
        if (viewBinding.apiSpinnerDisplayBack.visibility == View.GONE) return
        mScrollBehavior.slideUp(viewBinding.apiDisplay)
    }

    override fun selectOption(item: MenuItem): Boolean {
        when(item.itemId){
            MyR.id.apiTBRefresh -> {
                refreshLayout.isRefreshing = true
                if (needPermission(REQUEST_EXTERNAL_STORAGE_RE)) return true
                reloadList()
                return true
            }
            MyR.id.apiTBSearch -> {
                val searchView = item.actionView as SearchView
                searchView.queryHint = getText(R.string.sdk_search_hint)
                val filter = listFragment.filter
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    private var sOri: String = ""

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        val context = context ?: return true
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
                            refreshList()
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
            MyR.id.apiTBSort -> {
                if (popSort == null) {
                    // when in overflow menu, menu item has no icon button
                    // thus anchor would be null
                    val anchor = toolbar.findViewById<View>(MyR.id.apiTBSort) ?: return false
                    popSort = PopupMenu(context, anchor).apply {
                        if (OsUtils.satisfy(OsUtils.Q)) setForceShowIcon(true)
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
                val settings = MyBridge.getSettings()
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

    private fun exportList(context: Context) = lifecycleScope.launch(Dispatchers.Default) {
        val file = service.exportList(context, adapter.apps) ?: return@launch
        val label = "App List"
        withContext(Dispatchers.Main) {
            FilePop.by(context, file, "text/csv", R.string.fileActionsShare, imageLabel = label)
                    .show(childFragmentManager, FilePop.TAG)
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
        EasyAccess.init(context, settingsPreferences)

        apkRetriever = ApkRetriever(context)

        listFragment = if (savedInstanceState == null) AppListFragment.newInstance()
        else childFragmentManager.getFragment(savedInstanceState, STATE_KEY_LIST) as AppListFragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        viewBinding = FragmentApiBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onStop() {
        searchBackPressedCallback?.remove()
        searchBackPressedCallback = null
        ApiTaskManager.cancelAll()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        childFragmentManager.putFragment(outState, STATE_KEY_LIST, listFragment)
        super.onSaveInstanceState(outState)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            popSort = null
            // remove this callback otherwise app cannot go back
            searchBackPressedCallback?.remove()
            searchBackPressedCallback = null
        } else {
            val context = context ?: return
            // check settings
            lifecycleScope.launch(Dispatchers.Default) {
                val isChanged = EasyAccess.load(context, settingsPreferences, false)
                if (isChanged) {
                    delay(1)
                    withContext(Dispatchers.Main) {
                        viewBinding.avMainFilterText.text = null
                        viewBinding.avMainFilterText.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        super.onHiddenChanged(hidden)
    }

    private fun displayApk(context: Context, apkPath: String) {
        if (loadItem == ApiUnit.APK) {
            viewModel.addFile(context, apkPath)
            viewModel.sortApps(sortItem)
            updateList(viewModel.screen4Display(loadItem))
        } else lifecycleScope.launch(Dispatchers.Default) {
            viewModel.addFile(context, apkPath)
        }
    }

    private fun displayFile(context: Context, uri: Uri) {
        val doNow = when (loadItem) {
            ApiUnit.APK, ApiUnit.SELECTED, ApiUnit.VOLUME, ApiUnit.DISPLAY -> true
            else -> launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK
        }
        if (doNow) {
            viewModel.addFile(context, uri)
            viewModel.sortApps(sortItem)
            updateList(viewModel.screen4Display(loadItem))
        } else lifecycleScope.launch(Dispatchers.Default) {
            viewModel.addFile(context, uri)
        }
    }

    /**
     * from text processing activity, app list is empty
     */
    private fun displaySearch(context: Context, text: String){
        if (text.isEmpty()) return
        // Check store links
        val appFromStore = Utils.checkStoreLink(text)
        if (appFromStore != null) {
            val pi = MiscApp.getPackageInfo(context, packageName = appFromStore)
            if (pi != null) {
                val app = ApiViewingApp(context, pi, preloadProcess = true, archive = false)
                app.load(context, pi.applicationInfo)
                viewModel.addApps(app)
            }
        } else {
            val pm = context.packageManager
            val installedApps: List<PackageInfo> = pm.getInstalledPackages(0)
            val apps = mutableListOf<ApiViewingApp>()
            for (appInfo in installedApps) {
                val label = appInfo.applicationInfo.loadLabel(pm)
                if (label.contains(text, true)) {
                    apps.add(ApiViewingApp(context, appInfo, preloadProcess = true, archive = false))
                }
            }
            viewModel.addApps(apps)
        }
        viewModel.sortApps(sortItem)
        refreshList()
    }

    private fun Filter.filterBy(constraint: CharSequence) {
        filter(constraint) {
            doListUpdateAftermath()
            refreshLayout.isRefreshing = false
        }
    }

    private fun ceaseRefresh(unit: Int) {
        if (loadItem != unit) return
        lifecycleScope.launch(Dispatchers.Main) {
            refreshLayout.isRefreshing = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        val isRestore = savedInstanceState != null

        if (!isRestore) refreshLayout.isRefreshing = true

        ensureAdded(MyR.id.avViewListContainer, listFragment, true)
        adapter = listFragment.getAdapter()
        // todo use RecyclerView 1.2 to restore scroll position
        // myAdapter.setStateRestorationStrategy(StateRestorationStrategy.WHEN_NOT_EMPTY);

        sortItem = settingsPreferences.getInt(PrefUtil.AV_SORT_ITEM, PrefUtil.AV_SORT_ITEM_DEFAULT)
        rSort = RunnableSort(sortItem)
        adapter.setSortMethod(sortItem)

        launchMethod = LaunchMethod(arguments)
        val isSpecial = when (launchMethod.mode) {
            LaunchMethod.LAUNCH_MODE_LINK, LaunchMethod.LAUNCH_MODE_SEARCH -> true
            else -> false
        }
        if (isSpecial) {
            viewBinding.apiDisplay.visibility = View.GONE
            if (!isRestore) lifecycleScope.launch(Dispatchers.Default) {
                loadSortedList(ApiUnit.NON, sortEfficiently = true, fg = true)
            }
        }

        democratize()
        val dp5 = X.size(context, 5f, X.DP).toInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            val listInsetTop: Int
            when (launchMethod.mode) {
                LaunchMethod.LAUNCH_MODE_SEARCH -> {
                    floatTop = 0
                    listInsetTop = it
                }
                LaunchMethod.LAUNCH_MODE_LINK -> {
                    floatTop = it
                    listInsetTop = floatTop + dp5
                }
                else -> {
                    viewBinding.apiDisplay.run { alterPadding(top = it) }
                    viewBinding.apiSpinnerDisplayBack.measure()
                    floatTop = viewBinding.apiSpinnerDisplayBack.measuredHeight + it + dp5 * 2
                    listInsetTop = floatTop + dp5
                }
            }
            refreshLayout.setProgressViewOffset(false, listInsetTop + 2 * dp5, listInsetTop + 7 * dp5)
            adapter.topCover = listInsetTop
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            adapter.bottomCover = asBottomMargin(it)
        }

        refreshLayout.setOnRefreshListener(this::reloadList)

        MainActivity.syncScroll(mScrollBehavior)

        if (!isSpecial) {
            displayItem = settingsPreferences.getInt(PrefUtil.AV_LIST_SRC_ITEM, PrefUtil.AV_LIST_SRC_ITEM_DEFAULT)
            popSrc = PopupMenu(context, viewBinding.apiSpinnerDisplayBack).apply {
                if (OsUtils.satisfy(OsUtils.Q)) setForceShowIcon(true)
                inflate(MyR.menu.av_list_src)
                setOnMenuItemClickListener {
                    clickListSrcItem(it)
                }
            }
            viewBinding.avListSrc.setOnClickListener {
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
            viewBinding.avMainFilterContainer.setOnClickListener {
//                popTags?.showAsDropDown(avMainFilterCard)
                popTags.show()
            }
            X.curvedCard(viewBinding.apiStatsBack)
            X.curvedCard(viewBinding.avMainFilterCard)
            X.curvedCard(viewBinding.apiSpinnerDisplayBack)
        }

        if (OsUtils.satisfy(OsUtils.N)) {
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
            viewBinding.apiContainer.setOnDragListener(apkDisplayDragListener)
        }

        if (launchMethod.mode != LaunchMethod.LAUNCH_MODE_SEARCH
                && launchMethod.mode != LaunchMethod.LAUNCH_MODE_LINK) {
            // fix refreshing animation not shown
            lifecycleScope.launch(Dispatchers.Default) {
                delay(1)
                withContext(Dispatchers.Main) {
                    if (!isRestore) {
                        selectListSrcItem(displayItem)
                    } else {
                        loadListSrcItem(displayItem)
                        updateStats()
                    }
                }
            }
        }
        if (!isRestore && launchMethod.mode == LaunchMethod.LAUNCH_MODE_SEARCH) {
            // fix refreshing animation not shown
            // invoke only the first time
            if (loadedItems.isBusy && viewModel.apps4Cache.isEmpty()) {
                lifecycleScope.launch(Dispatchers.Default) {
                    delay(1)
                    withContext(Dispatchers.Main) {
                        refreshLayout.isRefreshing = true
                    }
                }
            }
        }
    }

    private fun clickSortItem(item: MenuItem): Boolean  {
        refreshLayout.isRefreshing = true
        sortItem = when (item.itemId) {
            MyR.id.menuApiSortAPIL -> SORT_POSITION_API_LOW
            MyR.id.menuApiSortAPIH -> SORT_POSITION_API_HIGH
            MyR.id.menuApiSortAPIName -> SORT_POSITION_API_NAME
            MyR.id.menuApiSortAPITime -> SORT_POSITION_API_TIME
            else -> sortItem
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
        loadListSrcItem(item)
        if (needPermission(REQUEST_EXTERNAL_STORAGE)) return true
        // clear tag filter
        val context = context
        if (context != null) {
            clearTagFilter(context)
        }
        rDisplay.position = displayItem
        ApiTaskManager.join(task = rDisplay)
        return true
    }

    private fun loadListSrcItem(item: Int) {
        popSrc?.menu?.getItem(item)?.let {
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
        val listener = if (isStatsAvailable) View.OnClickListener {
            mainViewModel.displayFragment(StatisticsFragment.newInstance(loadItem))
        } else null
        viewBinding.avMainStatsContainer.setOnClickListener(listener)
    }

    private fun closeFilterTagMenu(container: ViewGroup, checkedIndexes: Set<Int>) {
        val context = context ?: return
        refreshLayout.isRefreshing = true
        lifecycleScope.launch(Dispatchers.Default) {
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
            withContext(Dispatchers.Main) {
                if (value.isEmpty()) {
                    viewBinding.avMainFilterText.text = null
                    viewBinding.avMainFilterText.visibility = View.GONE
                    viewBinding.avMainFilterContainer.setOnLongClickListener(null)
                } else {
                    if (value.size == 1) {
                        viewBinding.avMainFilterText.text = singleTitle?.toString()
                        viewBinding.avMainFilterText.visibility = View.VISIBLE
                        if (checkedIndexes.first() in antiSelectedIndexes) {
                            viewBinding.avMainFilterText.paintFlags = viewBinding.avMainFilterText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        } else {
                            viewBinding.avMainFilterText.paintFlags = viewBinding.avMainFilterText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                    } else {
                        viewBinding.avMainFilterText.text = null
                        viewBinding.avMainFilterText.visibility = View.GONE
                    }
                    viewBinding.avMainFilterContainer.setOnLongClickListener {
                        clearTagFilter(context)
                        updateList(viewModel.screen4Display(loadItem))
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
            withContext(Dispatchers.Main) {
                updateList(displayList)
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
        viewBinding.avMainFilterText.text = null
        viewBinding.avMainFilterText.visibility = View.GONE
        AppTag.loadTagSettings(context, settingsPreferences, false)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        val context = context ?: return
        X.toast(context, "Out of memory", Toast.LENGTH_LONG)
    }

    /**
     * Reload app list completely
     */
    private fun reloadList() = lifecycleScope.launch(Dispatchers.Default) {
        ApiInfoPop.clearStores()
        viewModel.screenOut(loadItem)
//        viewModel.updateApps4Display()
        loadedItems.unLoad(loadItem)
        loadSortedList(loadItem, sortEfficiently = true, fg = true)
        refreshList()
    }

    /**
     * Refresh app list, serves as a shortcut to [updateList]
     */
    private fun refreshList() {
        updateList(viewModel.screen4Display(loadItem))
    }

    /**
     * Update adapter list, update stats, and scroll to top
     */
    private fun updateList(list: List<ApiViewingApp> = viewModel.apps4Cache) {
        lifecycleScope.launch(Dispatchers.Default) {
            listFragment.updateListSync(list, refreshLayout)
            doListUpdateAftermath()
        }
    }

    private fun doListUpdateAftermath() = lifecycleScope.launch {
        updateStats()
        scrollToTop()
    }

    private fun updateStats() {
        viewBinding.apiStats.text = adapter.listCount.toString()
        viewBinding.apiStats.visibility = View.VISIBLE
    }

    /**
     * @param sortEfficiently sort or not
     * @param fg do foreground loading (do sorting if needed, while no sorting as background)
     */
    private fun loadSortedList(item: Int , sortEfficiently: Boolean , fg: Boolean ) {
        val context = context ?: return

        if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_SEARCH) {
            val textExtra = launchMethod.textExtra
            if (textExtra != null) {
                // load as ApiUnit.ALL_APPS,
                // because loadedItems is used to check loading and avoid repeated loading
                if (!loadedItems.shouldLoad(ApiUnit.ALL_APPS)) return
                loadedItems.loading(ApiUnit.ALL_APPS)
                displaySearch(context, textExtra)
                loadedItems.finish(ApiUnit.ALL_APPS)
                return
            }
        }

        if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK) {
            val dataStreamExtra = launchMethod.dataStreamExtra
            if (dataStreamExtra != null) {
                val reIntent = Intent().apply {
                    data = dataStreamExtra
                }
                onActivityResult(INTENT_GET_FILE, AppCompatActivity.RESULT_OK, reIntent)
                return
            }
        }

        if (!loadedItems.shouldLoad(item)) {
            if (!sortEfficiently && fg) {
                viewModel.sortApps(sortItem)
                listFragment.updateList(viewModel.apps4Cache, refreshLayout)
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
                } else if (bUser){
                    viewModel.addUserApps(context)
                } else if (bSys){
                    viewModel.addSystemApps(context)
                }
                loadedItems.finish(item)
            }
            ApiUnit.APK -> {
                loadedItems.loading(item)
                lifecycleScope.launch(Dispatchers.Default) {
                    if (OsUtils.satisfy(OsUtils.Q)) accessiblePrimaryExternal(context) { treeUri ->
                        apkRetriever.fromUri(treeUri) {
                            displayFile(context, it)
                        }
                    } else {
                        try {
                            val external: File = getExternalStorageDirectory()
                            apkRetriever.fromFileFolder(external) {
                                displayApk(context, it.path)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
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
        if (item2Load == ApiUnit.APK) {
            if (OsUtils.satisfy(OsUtils.Q)) {
//                if (!accessiblePrimaryExternal(context, null)) return
                return // prohibit background loading due to storage consumption issue
            } else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (PermissionUtil.check(context, permissions).isNotEmpty()) return
            }
        }
        ApiTaskManager.now { loadSortedList(item2Load, sortEfficiently = true, fg = false) }
    }

    @Suppress("deprecation")
    private fun getExternalStorageDirectory(): File {
        return Environment.getExternalStorageDirectory()
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

    private fun needPermission(requestCode: Int ): Boolean {
        val context = context ?: return false
        return if (displayItem == DISPLAY_APPS_APK){
            if (OsUtils.satisfy(OsUtils.Q)) {
                var flagGranted = false
                accessiblePrimaryExternal(context){ flagGranted = true }
                if (flagGranted) return false
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    // make it possible to persist
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    // make it possible to access children
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(intent, REQUEST_OPEN_DIRECTORY)
                X.toast(context, MyR.string.av_apks_select_folder, Toast.LENGTH_LONG)
            } else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (PermissionUtil.check(context, permissions).isNotEmpty()) {
                    if (OsUtils.satisfy(OsUtils.M)) {
                        requestPermissions(permissions, requestCode)
                    } else {
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
            reloadList()
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
            refreshList()
        }
    }

    private inner class RunnableDisplay(override var position: Int) : Runnable, RunnableAPI {
        override fun run()  {
            if (position != DISPLAY_APPS_SELECT && position != DISPLAY_APPS_VOLUME) {
                settingsPreferences.edit { putInt(PrefUtil.AV_LIST_SRC_ITEM, position) }
            }
            loadSortedList(loadItem, sortEfficiently = true, fg = true)
            viewModel.sortApps(sortItem)
            refreshList()
        }
    }

    private fun dragDropAction(context: Context, clipData: ClipData?) = lifecycleScope.launch(Dispatchers.Default) {
        val cd: ClipData? = clipData
        if (cd == null) {
            notifyBriefly(R.string.text_no_content)
            return@launch
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
                    listFragment.filter.filterBy(item.text)
                }
                else -> {
                    if (!DocumentsContract.isDocumentUri(context, itemUri)) {
                        displayFile(context, itemUri)
                        continue@items
                    }
                    apkRetriever.fromDocumentUri(itemUri) {
                        displayFile(context, it)
                    }
                }
            }
        }
        ceaseRefresh(ApiUnit.DISPLAY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent? )  {
        super.onActivityResult(requestCode, resultCode, data)
        val context = context ?: return
        when (requestCode) {
            INTENT_GET_FILE -> lifecycleScope.launch(Dispatchers.Default) {
                // Obtain file's uri from data.getData().
                // If there are multiple files, achieve it by calling data.getClipData() instead.
                val uri: Uri? = data?.data
                val cd: ClipData? = data?.clipData
                if (resultCode != AppCompatActivity.RESULT_OK || (uri == null && cd == null)) {
                    notify(R.string.text_no_content)
                    return@launch
                }
                val pathsSize: Int = (if (uri != null) 1 else cd!!.itemCount)
                if (uri != null) {
                    displayFile(context, uri)
                } else {
                    for (i in 0 until pathsSize) {
                        displayFile(context, cd!!.getItemAt(i).uri)
                    }
                }
                ceaseRefresh(if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK) ApiUnit.NON else ApiUnit.SELECTED)
            }
            REQUEST_OPEN_DIRECTORY -> {
                if (resultCode != Activity.RESULT_OK) {
                    notifyBriefly(R.string.text_no_content)
                    ceaseRefresh(ApiUnit.APK)
                    return
                }
                val uri = data?.data
                if (uri != null) lifecycleScope.launch(Dispatchers.Default) {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    settingsPreferences.edit {
                        putString(P.URI_SCANNING_FOLDER, uri.toString())
                    }
                    rDisplay.position = displayItem
                    ApiTaskManager.join(task = rDisplay)
                }
            }
            REQUEST_OPEN_VOLUME -> {
                if (resultCode != Activity.RESULT_OK) {
                    notifyBriefly(R.string.text_no_content)
                    ceaseRefresh(ApiUnit.VOLUME)
                    return
                }
                // todo
                val uri = data?.data
                if (uri != null) lifecycleScope.launch(Dispatchers.Default) {
                    apkRetriever.fromUri(uri) {
                        displayFile(context, it)
                    }
                    ceaseRefresh(ApiUnit.VOLUME)
                }
            }
        }
    }
}

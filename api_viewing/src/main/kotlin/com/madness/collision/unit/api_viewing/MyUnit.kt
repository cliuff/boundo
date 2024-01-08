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
import android.content.*
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.*
import android.widget.Filter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.databinding.FragmentApiBinding
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.main.*
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.*
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.madness.collision.unit.api_viewing.R as MyR

class MyUnit: com.madness.collision.unit.Unit() {

    override val id: String = "AV"

    companion object {
//        const val ARG_INTENT = "intent"
//        private val TAG = ApiFragment::class.java.simpleName

        const val SORT_POSITION_API_LOW: Int  = 0
        const val SORT_POSITION_API_HIGH: Int  = 1
        const val SORT_POSITION_API_NAME: Int  = 2
        const val SORT_POSITION_API_TIME: Int  = 3

        const val STATE_KEY_LIST = "ListFragment"
    }

    private val viewModel: ApiViewingViewModel by activityViewModels()

    // data
    private lateinit var launchMethod: LaunchMethod
    private val dataConfig = MainDataConfig()
    private var loadItem: Int by dataConfig::loadItem
    private var sortItem: Int by dataConfig::sortItem
    private var displayItem: Int by dataConfig::displayItem
    private val loadedItems: ApiUnit
        get() = viewModel.loadedItems
    private lateinit var apkRetriever: ApkRetriever
    private lateinit var settingsPreferences: SharedPreferences

    // managers
    private lateinit var toolbarMan: MainToolbar
    private lateinit var statusMan: MainStatus
    private val operationMan = MainOperations()

    // views
    private val refreshLayout: SwipeRefreshLayout
        get() = viewBinding.apiSwipeRefresh
    private lateinit var adapter: APIAdapter
//    private var popFilterTag: PopupWindow? = null

    // context related
    private lateinit var listFragment: AppListFragment
    private lateinit var viewBinding: FragmentApiBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbarMan.createOptions(context, toolbar)
        configNavigation(toolbar, iconColor)
        inflateAndTint(MyR.menu.toolbar_api, toolbar, iconColor)
        toolbar.setOnClickListener {
            scrollToTop()
            listFragment.loadAppIcons(refreshLayout)
            listFragment.clearBottomAppIcons()
        }
        return true
    }

    inner class MainStateHandler {
        private val filter = listFragment.filter

        private fun handleSort(newSortPos: Int) {
            val sortPosition = if (newSortPos >= 0) newSortPos else sortItem
            sortItem = sortPosition
            refreshLayout.isRefreshing = true
            context?.let(::clearTagFilter)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                adapter.setSortMethod(sortPosition)
                settingsPreferences.edit { putInt(PrefUtil.AV_SORT_ITEM, sortPosition) }
                viewModel.sortApps(sortPosition)
                refreshList()
            }
        }

        private fun handleSearchState(searchState: SearchState) {
            when (searchState) {
                SearchState.None -> TODO()
                SearchState.EmptyQuery -> {
                    refreshLayout.isRefreshing = true
                    lifecycleScope.launch(Dispatchers.Default) {
                        refreshList()
                        filter.onCancel()
                    }
                }
                is SearchState.QueryText -> {
                    refreshLayout.isRefreshing = true
                    filter.isAddition = searchState.isAddition
                    filter.filterBy(searchState.value)
                }
                SearchState.SubmitQuery -> Unit
            }
        }

        private fun Filter.filterBy(constraint: CharSequence) {
            filter(constraint) {
                doListUpdateAftermath()
                refreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * After image operations, cache is cleared, leaving list empty.
     * Ensures list refreshes after user comes back.
     */
    private fun checkRefresh() {
        val doReload = loadedItems.isVacant && viewModel.apps4Cache.isEmpty()
        if (!doReload) return
        lifecycleScope.launch(Dispatchers.Default) {
            if (loadedItems.isBusy) return@launch
            withContext(Dispatchers.Main) {
                refreshLayout.isRefreshing = true
            }
            loadSortedList(loadItem, sortEfficiently = true, fg = true)
            refreshList()
        }
    }

    private val mScrollBehavior: HideBottomViewOnScrollBehavior<View>
        get() {
            val params = viewBinding.apiDisplay.layoutParams as CoordinatorLayout.LayoutParams
            return params.behavior as HideBottomViewOnScrollBehavior
        }

    private fun scrollToTop() {
        if (!isAdded) return
        listFragment.scrollToTop()
        if (viewBinding.apiSpinnerDisplayBack.visibility == View.GONE) return
        mScrollBehavior.slideUp(viewBinding.apiDisplay)
    }

    override fun selectOption(item: MenuItem): Boolean {
        return toolbarMan.selectOption(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context ?: return

        settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        EasyAccess.init(context, settingsPreferences)

        apkRetriever = ApkRetriever(context)

        listFragment = childFragmentManager.getSavedFragment(savedInstanceState, STATE_KEY_LIST)
                ?: AppListFragment.newInstance()

        toolbarMan = MainToolbar(this, dataConfig, settingsPreferences).apply {
            this.context = context
            this.viewModel = this@MyUnit.viewModel
            this.listFragment = this@MyUnit.listFragment
        }
        statusMan = MainStatus(this, dataConfig, settingsPreferences).apply {
            this.context = context
            this.viewModel = this@MyUnit.viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentApiBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onStop() {
        toolbarMan.removeSearchBackCallback()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        childFragmentManager.saveFragment(outState, STATE_KEY_LIST, listFragment)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (lifecycleEventTime.compareValues(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_CREATE) > 0) {
            context?.let(::checkLoadSettings)
        }
    }

    private fun checkLoadSettings(context: Context) {
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
        checkRefresh()
    }

    private fun displayApk(uri: Uri) {
        apkRetriever.resolveUri(uri, viewModel::addArchiveApp)
        viewModel.sortApps(sortItem)
        refreshList()
    }

    /**
     * from text processing activity, app list is empty
     */
    private fun displaySearch(context: Context, text: String){
        if (text.isEmpty()) return
        // Check store links
        val appFromStore = Utils.checkStoreLink(text)
        val dao = DataMaintainer.get(context, this)
        if (appFromStore != null) {
            dao.selectApp(appFromStore)?.load(context)?.let { viewModel.addApps(it) }
        } else {
            val list = dao.selectAllApps()
                .filter { it.name.contains(text, ignoreCase = true) }
                .onEach { it.load(context) }
            viewModel.addApps(list)
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
        lifecycleScope.launch(Dispatchers.Default) {
            // delay to reduce UI update clustering
            delay(200)
            withContext(Dispatchers.Main) {
                refreshLayout.isRefreshing = false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbarMan.viewBinding = this@MyUnit.viewBinding
        statusMan.viewBinding = this@MyUnit.viewBinding
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
        adapter.setSortMethod(sortItem)

        launchMethod = LaunchMethod(arguments)

        democratize()
        observeStates(context)

        refreshLayout.setOnRefreshListener(this::reloadList)

        val isSpecial = when (launchMethod.mode) {
            LaunchMethod.LAUNCH_MODE_LINK, LaunchMethod.LAUNCH_MODE_SEARCH -> true
            else -> false
        }
        if (isSpecial) {
            viewBinding.apiDisplay.visibility = View.GONE
            if (!isRestore) lifecycleScope.launch(Dispatchers.Default) {
                loadSortedList(ApiUnit.NON, sortEfficiently = true, fg = true)
            }
        } else {
            displayItem = settingsPreferences.getInt(PrefUtil.AV_LIST_SRC_ITEM, PrefUtil.AV_LIST_SRC_ITEM_DEFAULT)
            statusMan.createListSrcPopup(context, viewBinding.apiSpinnerDisplayBack)
            viewBinding.avListSrc.setOnClickListener { statusMan.showListSrcPopup() }
            statusMan.setupTagFilterPopup(context)
//            val width = ViewGroup.LayoutParams.WRAP_CONTENT
//            popFilterTag = PopupWindow(filterTagDelegate, width, width).apply {
//                setOnDismissListener {
//                }
//            }
            viewBinding.avMainFilterContainer.setOnClickListener {
//                popTags?.showAsDropDown(avMainFilterCard)
                statusMan.showTagsPopup()
            }
            X.curvedCard(viewBinding.apiStatsBack)
            X.curvedCard(viewBinding.avMainFilterCard)
            X.curvedCard(viewBinding.apiSpinnerDisplayBack)
        }

        if (OsUtils.satisfy(OsUtils.N)) setupDragAndDrop(context)

        when (launchMethod.mode) {
            LaunchMethod.LAUNCH_MODE_SEARCH -> if (!isRestore) fixRefreshAnim()
            LaunchMethod.LAUNCH_MODE_LINK -> { }
            else -> fixRefresh(isRestore)
        }
    }

    private fun observeStates(context: Context) {
        val dp5 = X.size(context, 5f, X.DP).toInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            val listInsetTop = when (launchMethod.mode) {
                LaunchMethod.LAUNCH_MODE_SEARCH -> it
                LaunchMethod.LAUNCH_MODE_LINK -> it + dp5
                else -> {
                    viewBinding.apiDisplay.run { alterPadding(top = it) }
                    viewBinding.apiSpinnerDisplayBack.measure()
                    val floatTop = viewBinding.apiSpinnerDisplayBack.measuredHeight + it + dp5 * 2
                    floatTop + dp5
                }
            }
            refreshLayout.setProgressViewOffset(false, listInsetTop + 2 * dp5, listInsetTop + 7 * dp5)
            adapter.topCover = listInsetTop
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            adapter.bottomCover = asBottomMargin(it)
        }
    }

    // fix refreshing animation not shown
    private fun fixRefresh(isRestore: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1)
            if (!isRestore) {
                statusMan.selectListSrcItem(displayItem)
            } else {
                statusMan.loadListSrcItem(displayItem)
                updateStats()
            }
        }
    }

    // fix refreshing animation not shown, invoke only the first time
    private fun fixRefreshAnim() {
        val doFix = loadedItems.isBusy && viewModel.apps4Cache.isEmpty()
        if (!doFix) return
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1)
            refreshLayout.isRefreshing = true
        }
    }

    @RequiresApi(OsUtils.N)
    private fun setupDragAndDrop(context: Context) {
        viewBinding.apiContainer.setOnDragListener { _, event: DragEvent? ->
            event ?: return@setOnDragListener false
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> notifyBriefly(R.string.apiDragDropHint)
                DragEvent.ACTION_DROP -> {
                    val permission = activity?.requestDragAndDropPermissions(event)
                    lifecycleScope.launch(Dispatchers.Default) {
                        dragDropAction(context, event.clipData)
                        permission?.release()
                    }
                }
            }
            true
        }
    }

    fun clearTagFilter(context: Context) {
        viewBinding.avMainFilterText.text = null
        viewBinding.avMainFilterText.visibility = View.GONE
        AppTag.loadTagSettings(settingsPreferences, false)
    }

    /**
     * Reload app list completely
     */
    fun reloadList() = lifecycleScope.launch(Dispatchers.Default) {
        viewModel.screenOut(loadItem)
//        viewModel.updateApps4Display()
        loadedItems.unLoad(loadItem)
        loadSortedList(loadItem, sortEfficiently = true, fg = true)
        refreshList()
    }

    /**
     * Refresh app list, serves as a shortcut to [updateList]
     */
    fun refreshList() {
        updateList(viewModel.screen4Display(loadItem))
    }

    /**
     * Update adapter list, update stats, and scroll to top
     */
    fun updateList(list: List<ApiViewingApp> = viewModel.apps4Cache) {
        lifecycleScope.launch(Dispatchers.Default) {
            listFragment.updateListSync(list, refreshLayout)
            doListUpdateAftermath()
        }
    }

    fun doListUpdateAftermath() = lifecycleScope.launch {
        updateStats()
        scrollToTop()
    }

    private fun updateStats() {
        viewBinding.apiStats.text = adapter.listCount.adapted
        viewBinding.apiStats.visibility = View.VISIBLE
    }

    /**
     * @param sortEfficiently sort or not
     * @param fg do foreground loading (do sorting if needed, while no sorting as background)
     */
    fun loadSortedList(item: Int, sortEfficiently: Boolean, fg: Boolean ) {
        val context = context ?: return

        // update records
        if (MyUpdatesFragment.isNewSession(mainViewModel.timestamp)) {
            viewModel.maintainRecords(context)
        }

        if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_SEARCH) {
            val textExtra = launchMethod.textExtra
            if (textExtra != null) {
                // load as ApiUnit.ALL_APPS,
                // because loadedItems is used to check loading and avoid repeated loading
                if (!loadedItems.shouldLoad(ApiUnit.ALL_APPS)) return
                loadedItems.startLoad(ApiUnit.ALL_APPS) {
                    displaySearch(context, textExtra)
                }
            }
            return
        } else if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK) {
            val dataStreamExtra = launchMethod.dataStreamExtra
            if (dataStreamExtra != null) {
                apkRetriever.resolvePackage(dataStreamExtra) { it?.let(viewModel::addArchiveApp) }
                viewModel.sortApps(sortItem)
                refreshList()
                ceaseRefresh(ApiUnit.NON)
            }
            return
        }

        if (!viewModel.loadedItems.shouldLoad(item)) {
            if (!sortEfficiently && fg) {
                viewModel.sortApps(dataConfig.sortItem)
                listFragment.updateList(viewModel.apps4Cache, viewBinding.apiSwipeRefresh)
            }
            return
        }

        when (item) {
            ApiUnit.APK -> {
                loadedItems.startLoad(item) {
                    // user selected primary storage to scan APKs
                    accessiblePrimaryExternal(context) { treeUri ->
                        apkRetriever.fromUri(treeUri, ::displayApk)
                    }
                    ceaseRefresh(ApiUnit.APK)
                }
                return
            }
            ApiUnit.SELECTED -> {
                getContentLauncher.launch("application/vnd.android.package-archive")
                return
            }
            ApiUnit.VOLUME -> {
                openVolumeLauncher.launch(null)
                return
            }
        }

        viewModel.loadAppItemList(context, item)
        if (fg) viewModel.sortApps(dataConfig.sortItem)
        viewModel.loadAppItems(context)
    }

    fun accessiblePrimaryExternal(context: Context, task: ((uri: Uri) -> Unit)?): Boolean{
        val uriExternalPrimary = settingsPreferences.getString(P.URI_SCANNING_FOLDER, "") ?: ""
        for (uriPermission in context.contentResolver.persistedUriPermissions) {
            val uri = uriPermission.uri
            if (uri.toString() != uriExternalPrimary) continue
            task?.invoke(uri)
            return true
        }
        return false
    }

    fun needPermission(request: (permission: String) -> Unit): Boolean {
        val context = context ?: return false
        if (displayItem != MainStatus.DISPLAY_APPS_APK) return false
        if (OsUtils.satisfy(OsUtils.Q)) {
            var flagGranted = false
            accessiblePrimaryExternal(context){ flagGranted = true }
            if (flagGranted) return false
            openDirectoryLauncher.launch(null)
            X.toast(context, MyR.string.av_apks_select_folder, Toast.LENGTH_LONG)
        } else {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (PermissionUtil.check(context, permissions).isNotEmpty()) {
                if (OsUtils.satisfy(OsUtils.M)) {
                    request(permissions[0])
                } else {
                    refreshLayout.isRefreshing = false
                    notify(R.string.toast_permission_storage_denied)
                }
            }
        }
        return true
    }

    private fun dragDropAction(context: Context, clipData: ClipData?) {
        val cd: ClipData? = clipData
        if (cd == null) {
            notifyBriefly(R.string.text_no_content)
            return
        }
        val description = cd.description
        if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            loadItem = ApiUnit.DISPLAY
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                statusMan.selectListSrcItem(MainStatus.DISPLAY_APPS_SELECT)
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
                        displayApk(itemUri)
                        continue@items
                    }
                    apkRetriever.fromDocumentUri(itemUri, ::displayApk)
                }
            }
        }
        ceaseRefresh(ApiUnit.DISPLAY)
    }

    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()) register@{ uriList ->
        if (uriList.isEmpty()) {
            notify(R.string.text_no_content)
            return@register
        }
        val context = context ?: return@register
        lifecycleScope.launch(Dispatchers.Default) {
            uriList.forEach(::displayApk)
            ceaseRefresh(if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK) ApiUnit.NON else ApiUnit.SELECTED)
        }
    }

    private val openDirectoryLauncher = operationMan.registerDirectoryOpening(this) register@{ uri: Uri? ->
        if (uri == null) {
            notifyBriefly(R.string.text_no_content)
            ceaseRefresh(ApiUnit.APK)
            return@register
        }
        val context = context ?: return@register
        lifecycleScope.launch(Dispatchers.Default) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            settingsPreferences.edit {
                putString(P.URI_SCANNING_FOLDER, uri.toString())
            }
            statusMan.joinDisplay()
        }
    }

    private val openVolumeLauncher = operationMan.registerVolumeOpening(this) register@{ uri ->
        if (uri == null) {
            notifyBriefly(R.string.text_no_content)
            ceaseRefresh(ApiUnit.VOLUME)
            return@register
        }
        // todo
        lifecycleScope.launch(Dispatchers.Default) {
            apkRetriever.fromUri(uri, ::displayApk)
            ceaseRefresh(ApiUnit.VOLUME)
        }
    }
}

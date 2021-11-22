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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.database.RecordMaintainer
import com.madness.collision.unit.api_viewing.databinding.FragmentApiBinding
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.list.ApiInfoPop
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.main.MainDataConfig
import com.madness.collision.unit.api_viewing.main.MainListLoader
import com.madness.collision.unit.api_viewing.main.MainStatus
import com.madness.collision.unit.api_viewing.main.MainToolbar
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.*
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.*
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
        var dataStreamExtra: PackageInfo? = null
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
    private val dataConfig = MainDataConfig()
    private var loadItem: Int by dataConfig::loadItem
    private var sortItem: Int by dataConfig::sortItem
    private var displayItem: Int by dataConfig::displayItem
    private val loadedItems: ApiUnit
        get() = viewModel.loadedItems
    private var floatTop: Int = 0
    private var tbDrawable: Drawable? = null
    private lateinit var apkRetriever: ApkRetriever
    private lateinit var settingsPreferences: SharedPreferences

    // managers
    private lateinit var toolbarMan: MainToolbar
    private lateinit var statusMan: MainStatus
    private lateinit var listLoadingMan: MainListLoader

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
        toolbar.background.mutate().constantState?.newDrawable()?.let { tbDrawable = it }
        return true
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
        val colorTb = ThemeUtil.getColor(context, R.attr.colorTb)
        if (tbDrawable == null) tbDrawable = ColorDrawable(colorTb)

        settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        EasyAccess.init(context, settingsPreferences)

        loadedItems.apkPreload = settingsPreferences.getBoolean(
            PrefUtil.API_APK_PRELOAD, PrefUtil.API_APK_PRELOAD_DEFAULT)
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
        listLoadingMan = MainListLoader(this, dataConfig).apply {
            this.context = context
            this.viewModel = this@MyUnit.viewModel
            this.listFragment = this@MyUnit.listFragment
            this.apkRetriever = this@MyUnit.apkRetriever
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentApiBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onStop() {
        toolbarMan.removeSearchBackCallback()
        ApiTaskManager.cancelAll()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        childFragmentManager.saveFragment(outState, STATE_KEY_LIST, listFragment)
        super.onSaveInstanceState(outState)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            // set as null to fix anchor problem with toolbar
                toolbarMan.removeSortPopup()
            // remove this callback otherwise app cannot go back
            toolbarMan.removeSearchBackCallback()
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
            checkRefresh()
        }
        super.onHiddenChanged(hidden)
    }

    private fun displayApk(doNow: Boolean, block: () -> Unit) {
        if (doNow) {
            block.invoke()
            viewModel.sortApps(sortItem)
            updateList(viewModel.screen4Display(loadItem))
        } else lifecycleScope.launch(Dispatchers.Default) {
            block.invoke()
        }
    }

    private fun <T> displayApk(resolve: ApkRetriever.(T, (ApiViewingApp?) -> Unit) -> Unit,
                               arg: T, doNow: Boolean) {
        displayApk(doNow) {
            apkRetriever.resolve(arg) {
                it ?: return@resolve
                viewModel.addArchiveApp(it)
            }
        }
    }

    private fun <T> displayApk(context: Context, arg: T): Unit = when (arg) {
        is Uri -> {
            val doNow = when (loadItem) {
                ApiUnit.APK, ApiUnit.SELECTED, ApiUnit.VOLUME, ApiUnit.DISPLAY -> true
                else -> launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK
            }
            displayApk(ApkRetriever::resolveUri, arg, doNow)
        }
        is String -> {
            val doNow = loadItem == ApiUnit.APK
            displayApk(ApkRetriever::resolvePath, arg, doNow)
        }
        else -> Unit
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
            dao.selectNameAlike(text).let {
                viewModel.addApps(it)
            }
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
        listLoadingMan.viewBinding = this@MyUnit.viewBinding
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
        toolbarMan.setupSortRunnable()
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

        if (!isSpecial) {
            displayItem = settingsPreferences.getInt(PrefUtil.AV_LIST_SRC_ITEM, PrefUtil.AV_LIST_SRC_ITEM_DEFAULT)
            statusMan.createListSrcPopup(context, viewBinding.apiSpinnerDisplayBack)
            viewBinding.avListSrc.setOnClickListener { statusMan.showListSrcPopup() }
            statusMan.setupDisplayRunnable()
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

        if (launchMethod.mode != LaunchMethod.LAUNCH_MODE_SEARCH
                && launchMethod.mode != LaunchMethod.LAUNCH_MODE_LINK) {
            // fix refreshing animation not shown
            lifecycleScope.launch(Dispatchers.Default) {
                delay(1)
                withContext(Dispatchers.Main) {
                    if (!isRestore) {
                        statusMan.selectListSrcItem(displayItem)
                    } else {
                        statusMan.loadListSrcItem(displayItem)
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
    fun reloadList() = lifecycleScope.launch(Dispatchers.Default) {
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
            RecordMaintainer.pack(context, this).all
        }

        if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_SEARCH) {
            val textExtra = launchMethod.textExtra
            if (textExtra != null) {
                // load as ApiUnit.ALL_APPS,
                // because loadedItems is used to check loading and avoid repeated loading
                if (!loadedItems.shouldLoad(ApiUnit.ALL_APPS)) return
                loadedItems.loading(ApiUnit.ALL_APPS)
                displaySearch(context, textExtra)
                loadedItems.finish(ApiUnit.ALL_APPS)
            }
            return
        } else if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK) {
            val dataStreamExtra = launchMethod.dataStreamExtra
            if (dataStreamExtra != null) {
                lifecycleScope.launch(Dispatchers.Default) {
                    displayPkgInfo(dataStreamExtra)
                    ceaseRefresh(ApiUnit.NON)
                }
            }
            return
        }

        var loadingItem: Int = item
        var isForegroundLoading = fg
        var isEfficientSort = sortEfficiently
        while (true) {
            loadingItem = listLoadingMan.loadSortedStandardList(loadingItem, isEfficientSort, isForegroundLoading) {
                when (loadingItem) {
                    ApiUnit.USER, ApiUnit.SYS, ApiUnit.ALL_APPS -> listLoadingMan.loadAppItemList(context, loadingItem)
                    ApiUnit.APK, ApiUnit.SELECTED, ApiUnit.VOLUME -> loadApkItemList(context, loadingItem)
                }
            } ?: break
            isForegroundLoading = false  // next item will be loaded in background
            isEfficientSort = true  // next item will sort efficiently
            if (loadingItem != ApiUnit.APK) continue
            // check APK item to load in background for permission
            if (OsUtils.satisfy(OsUtils.Q)) {
//                if (!accessiblePrimaryExternal(context, null)) break
                break // prohibit background loading due to storage consumption issue
            } else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (PermissionUtil.check(context, permissions).isNotEmpty()) break
            }
        }
    }

    private fun loadApkItemList(context: Context, item: Int) {
        loadedItems.loading(item)
        when (item){
            ApiUnit.APK -> {
                lifecycleScope.launch(Dispatchers.Default) {
                    listLoadingMan.loadDeviceApks(context) { displayApk(context, it) }
                    ceaseRefresh(ApiUnit.APK)
                    loadedItems.finish(item)
                }
            }
            ApiUnit.SELECTED -> {
                getContentLauncher.launch("application/vnd.android.package-archive")
            }
            ApiUnit.VOLUME -> {
                openVolumeLauncher.launch(null)
            }
        }
    }

    private fun displayPkgInfo(info: PackageInfo) {
        displayApk(true) {
            apkRetriever.resolvePackage(info) {
                it ?: return@resolvePackage
                viewModel.addArchiveApp(it)
            }
        }
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
            ApiTaskManager.now(Dispatchers.Main) {
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
                        displayApk(context, itemUri)
                        continue@items
                    }
                    apkRetriever.fromDocumentUri(itemUri) {
                        displayApk(context, it)
                    }
                }
            }
        }
        ceaseRefresh(ApiUnit.DISPLAY)
    }

    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uriList ->
        if (uriList.isEmpty()) {
            notify(R.string.text_no_content)
            return@registerForActivityResult
        }
        val context = context ?: return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.Default) {
            uriList.forEach { displayApk(context, it) }
            ceaseRefresh(if (launchMethod.mode == LaunchMethod.LAUNCH_MODE_LINK) ApiUnit.NON else ApiUnit.SELECTED)
        }
    }

    private val openDirectoryLauncher = registerForActivityResult(
        object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                return super.createIntent(context, input).apply {
                    // make it possible to persist
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    // make it possible to access children
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    ) { uri: Uri? ->
        if (uri == null) {
            notifyBriefly(R.string.text_no_content)
            ceaseRefresh(ApiUnit.APK)
            return@registerForActivityResult
        }
        val context = context ?: return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.Default) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            settingsPreferences.edit {
                putString(P.URI_SCANNING_FOLDER, uri.toString())
            }
            statusMan.joinDisplay()
        }
    }

    private val openVolumeLauncher = registerForActivityResult(
        object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                return super.createIntent(context, input).apply {
                    // make it possible to access children
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    ) { uri ->
        if (uri == null) {
            notifyBriefly(R.string.text_no_content)
            ceaseRefresh(ApiUnit.VOLUME)
            return@registerForActivityResult
        }
        val context = context ?: return@registerForActivityResult
        // todo
        lifecycleScope.launch(Dispatchers.Default) {
            apkRetriever.fromUri(uri) { displayApk(context, it) }
            ceaseRefresh(ApiUnit.VOLUME)
        }
    }
}

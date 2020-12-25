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

package com.madness.collision.unit.api_viewing.list

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AvListBinding
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import com.madness.collision.unit.api_viewing.R as RAv

internal class AppListFragment : TaggedFragment(), Filterable {

    override val category: String = "AV"
    override val id: String = "AppList"

    companion object {
        private const val ARG_IS_SCROLLBAR_ENABLED = "isScrollbarEnabled"
        private const val ARG_IS_FADING_EDGE_ENABLED = "isFadingEdgeEnabled"
        private const val ARG_IS_NESTED_SCROLLING_ENABLED = "isNestedScrollingEnabled"

        fun newInstance(): AppListFragment {
            return AppListFragment()
        }

        fun newInstance(isScrollbarEnabled: Boolean, isFadingEdgeEnabled: Boolean, isNestedScrollingEnabled: Boolean): AppListFragment {
            val args = Bundle().apply {
                putBoolean(ARG_IS_SCROLLBAR_ENABLED, isScrollbarEnabled)
                putBoolean(ARG_IS_FADING_EDGE_ENABLED, isFadingEdgeEnabled)
                putBoolean(ARG_IS_NESTED_SCROLLING_ENABLED, isNestedScrollingEnabled)
            }
            return AppListFragment().apply { arguments = args }
        }
    }

    private lateinit var mContext: Context
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: APIAdapter
    private lateinit var mManager: RecyclerView.LayoutManager
    private lateinit var viewBinding: AvListBinding
    private val service = AppListService()
    private val viewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        val context = mContext
        mAdapter = APIAdapter(mContext, object : APIAdapter.Listener {
            override val click: (ApiViewingApp) -> Unit = {
                ApiInfoPop.newInstance(it).show(childFragmentManager, ApiInfoPop.TAG)
            }
            override val longClick: (ApiViewingApp) -> Boolean = {
                showOptions(context, it)
                true
            }
        })

        mAdapter.resolveSpanCount(this, 450f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = AvListBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    fun showAppOptions(app: ApiViewingApp) {
        showOptions(mContext, app)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mRecyclerView = viewBinding.avListRecyclerView

        arguments?.run {
            mRecyclerView.isVerticalScrollBarEnabled = getBoolean(ARG_IS_SCROLLBAR_ENABLED, mRecyclerView.isVerticalScrollBarEnabled)
            mRecyclerView.isVerticalFadingEdgeEnabled = getBoolean(ARG_IS_FADING_EDGE_ENABLED, mRecyclerView.isVerticalFadingEdgeEnabled)
            mRecyclerView.isNestedScrollingEnabled = getBoolean(ARG_IS_NESTED_SCROLLING_ENABLED, mRecyclerView.isNestedScrollingEnabled)
        }

        mManager = mAdapter.suggestLayoutManager()
        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter

        viewModel.apps4Display.observe(viewLifecycleOwner) {
            mAdapter.apps = it
        }
    }

    fun scrollToTop() {
        mManager.scrollToPosition(0)
    }

    fun getAdapter(): APIAdapter {
        return mAdapter
    }

    fun getRecyclerView(): RecyclerView {
        return mRecyclerView
    }

    private fun updateCacheSize() {
        val manager = mManager as LinearLayoutManager
        val unitSize = manager.findLastVisibleItemPosition() - manager.findFirstVisibleItemPosition()
        updateCacheSize(unitSize)
    }

    private fun updateCacheSize(unitSize: Int) {
        val cacheSize = if (unitSize < 20) (30 + unitSize * 10) else (100 + unitSize * 7)
        EasyAccess.loadLimitHalf = cacheSize
        EasyAccess.loadAmount = unitSize
        EasyAccess.preloadLimit = EasyAccess.loadLimitHalf - EasyAccess.loadAmount
    }

    fun updateList(list: List<ApiViewingApp>, refreshLayout: SwipeRefreshLayout? = null) {
        if (list.isEmpty() && viewModel.apps4DisplayValue.isEmpty()) return
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.updateApps4Display(list)
        }
        // use launchWhenStarted to avoid mRecyclerView not initialized bug when linking from app store
        lifecycleScope.launchWhenStarted {
            mRecyclerView.post {
                updateCacheSize()
                loadAppIcons(refreshLayout)
            }
        }
    }

    fun loadAppIcons(refreshLayout: SwipeRefreshLayout? = null) = lifecycleScope.launch(Dispatchers.Default) {
        try {
            for (index in viewModel.apps4DisplayValue.indices) {
                if (index >= EasyAccess.preloadLimit) break
                if (index >= viewModel.apps4DisplayValue.size) break
                if (refreshLayout == null) {
                    mAdapter.ensureItem(index)
                    continue
                }
                val shouldCeaseRefresh = (index >= EasyAccess.loadAmount - 1)
                        || (index >= mAdapter.listCount - 1)
                val doCeaseRefresh = shouldCeaseRefresh && refreshLayout.isRefreshing
                if (doCeaseRefresh) withContext(Dispatchers.Main) {
                    refreshLayout.isRefreshing = false
                }
                val callback: (() -> Unit)? = if (doCeaseRefresh) {
                    {
                        refreshLayout.isRefreshing = false
                    }
                } else null
                mAdapter.ensureItem(index, callback)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearBottomAppIcons() = lifecycleScope.launch(Dispatchers.Default) {
        try {
            var index = viewModel.apps4DisplayValue.size - 1
            val cacheSize = EasyAccess.loadLimitHalf * 2 + 10
            while (index >= cacheSize) {
                val app = viewModel.apps4DisplayValue[index]
                if (!app.preload) app.clearIcons()
                index--
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        childFragmentManager.run {
            (findFragmentByTag(ApiInfoPop.TAG) as BottomSheetDialogFragment?)?.dismiss()
        }
        super.onPause()
    }

    abstract class Filter: android.widget.Filter() {
        var isAddition: Boolean = false
        abstract fun onCancel()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val appList = if (isAddition) viewModel.apps4DisplayValue
                else (viewModel.reservedApps ?: emptyList())
                val filterResults = FilterResults()
                if (appList.isEmpty()) {
                    filterResults.count = 0
                    return filterResults
                }
                val filtered: MutableList<ApiViewingApp> = mutableListOf()
                val filterText = charSequence.toString()
                val iterator: Iterator<ApiViewingApp> = appList.iterator()
                // Check store links
                val appFromStore = Utils.checkStoreLink(filterText)
                if (appFromStore != null) {
                    while (iterator.hasNext()) {
                        val info = iterator.next()
                        if (info.packageName == appFromStore) {
                            filtered.add(info)
                            break
                        }
                    }
                    filterResults.values = filtered
                    filterResults.count = filtered.size
                    return filterResults
                }
                val locale = SystemUtil.getLocaleApp()
                val input4Comparision: String = filterText.toLowerCase(locale)
                while (iterator.hasNext()) {
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
                // reserve app list
                viewModel.reserveApps()
                updateList(re)
            }

            override fun onCancel() {
                viewModel.clearReserved()
            }
        }
    }

    private fun showOptions(context: Context, app: ApiViewingApp) {
        val popActions = CollisionDialog(context, R.string.text_cancel).apply {
            setTitleCollision(0, 0, 0)
            setContent(0)
            setCustomContent(RAv.layout.av_adapter_actions)
            setListener { dismiss() }
            show()
        }
        popActions.findViewById<View>(RAv.id.avAdapterActionsDetails).setOnClickListener {
            popActions.dismiss()
            actionDetails(context, app)
        }
        val vActionOpen = popActions.findViewById<View>(RAv.id.avAdapterActionsOpen)
        if (app.isLaunchable) {
            val launchIntent = service.getLaunchIntent(context, app)
            val activityName = launchIntent?.component?.className ?: ""
            val vOpenActivity = popActions.findViewById<TextView>(RAv.id.avAdapterActionsOpenActivity)
            vOpenActivity.text = activityName
            vActionOpen.setOnClickListener {
                popActions.dismiss()
                if (launchIntent == null) {
                    notifyBriefly(R.string.text_error)
                } else {
                    startActivity(launchIntent)
                }
            }
            vActionOpen.setOnLongClickListener {
                X.copyText2Clipboard(context, activityName, R.string.text_copy_content)
                true
            }
        } else {
            vActionOpen.visibility = View.GONE
        }
        popActions.findViewById<View>(RAv.id.avAdapterActionsIcon).setOnClickListener {
            popActions.dismiss()
            actionIcon(context, app)
        }
        popActions.findViewById<View>(RAv.id.avAdapterActionsApk).setOnClickListener {
            popActions.dismiss()
            actionApk(context, app)
        }
    }

    private fun actionDetails(context: Context, appInfo: ApiViewingApp) = lifecycleScope.launch(Dispatchers.Default) {
        val details = service.getAppDetails(context, appInfo)
        if (details.isEmpty()) return@launch
        val contentView = TextView(context)
        contentView.text = details
        contentView.textSize = 10f
        val padding = X.size(context, 20f, X.DP).toInt()
        contentView.setPadding(padding, padding, padding, 0)
        withContext(Dispatchers.Main) {
            CollisionDialog(context, R.string.text_alright).run {
                setContent(0)
                setTitleCollision(appInfo.name, 0, 0)
                setCustomContent(contentView)
                decentHeight()
                setListener { dismiss() }
                show()
            }
        }
    }

    private fun actionIcon(context: Context, app: ApiViewingApp) {
        val path = F.createPath(F.cachePublicPath(context), "App", "Logo", "${app.name}.png")
        val image = File(path)
        app.getOriginalIcon(context)?.let {
            if (F.prepare4(image)) X.savePNG(it, path)
        }
        val uri: Uri = image.getProviderUri(context)
//        val previewTitle = app.name // todo set preview title
        childFragmentManager.let {
            FilePop.by(context, uri, "image/png", R.string.textShareImage, uri, app.name).show(it, FilePop.TAG)
        }
    }

    // todo split APKs
    private fun actionApk(context: Context, app: ApiViewingApp) {
        val path = F.createPath(F.cachePublicPath(context), "App", "APK", "${app.name}-${app.verName}.apk")
        val apk = File(path)
        if (F.prepare4(apk)) {
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    X.copyFileLessTwoGB(File(app.appPackage.basePath), apk)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        val uri: Uri = apk.getProviderUri(context)
        val previewTitle = "${app.name} ${app.verName}"
//        val flag = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val previewPath = F.createPath(F.cachePublicPath(context), "App", "Logo", "${app.name}.png")
        val image = File(previewPath)
        val appIcon = app.icon
        if (appIcon != null && F.prepare4(image)) X.savePNG(appIcon, previewPath)
        val imageUri = image.getProviderUri(context)
        childFragmentManager.let {
            val fileType = "application/vnd.android.package-archive"
            FilePop.by(context, uri, fileType, R.string.textShareApk, imageUri, previewTitle).show(it, FilePop.TAG)
        }
    }

}

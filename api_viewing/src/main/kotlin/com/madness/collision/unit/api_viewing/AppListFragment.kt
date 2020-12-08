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

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.databinding.AvListBinding
import com.madness.collision.unit.api_viewing.list.AppListService
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt
import com.madness.collision.unit.api_viewing.R as MyR

internal class AppListFragment : TaggedFragment() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        val context = mContext
        mAdapter = APIAdapter(mContext, object : APIAdapter.Listener {
            override val click: (ApiViewingApp) -> Unit = {
                ApiInfoPop.newInstance(it).show(childFragmentManager, ApiInfoPop.TAG)
            }
            override val longClick: (ApiViewingApp) -> Boolean = {
                val popActions = CollisionDialog(context, R.string.text_cancel).apply {
                    setTitleCollision(0, 0, 0)
                    setContent(0)
                    setCustomContent(MyR.layout.av_adapter_actions)
                    setListener { dismiss() }
                    show()
                }
                popActions.findViewById<View>(MyR.id.avAdapterActionsDetails).setOnClickListener { _ ->
                    popActions.dismiss()
                    actionDetails(context, it)
                }
                val vActionOpen = popActions.findViewById<View>(MyR.id.avAdapterActionsOpen)
                if (it.isLaunchable) {
                    val launchIntent = service.getLaunchIntent(context, it)
                    val activityName = launchIntent?.component?.className ?: ""
                    val vOpenActivity = popActions.findViewById<TextView>(MyR.id.avAdapterActionsOpenActivity)
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
                popActions.findViewById<View>(MyR.id.avAdapterActionsIcon).setOnClickListener { _ ->
                    popActions.dismiss()
                    actionIcon(context, it)
                }
                popActions.findViewById<View>(MyR.id.avAdapterActionsApk).setOnClickListener { _ ->
                    popActions.dismiss()
                    actionApk(context, it)
                }
                true
            }
        })

        val unitWidth = X.size(mContext, 450f, X.DP)
        val spanCount = (availableWidth / unitWidth).roundToInt().run {
            if (this < 2) 1 else this
        }
        mAdapter.spanCount = spanCount
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = AvListBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mRecyclerView = viewBinding.avListRecyclerView

        arguments?.run {
            mRecyclerView.isVerticalScrollBarEnabled = getBoolean(ARG_IS_SCROLLBAR_ENABLED, mRecyclerView.isVerticalScrollBarEnabled)
            mRecyclerView.isVerticalFadingEdgeEnabled = getBoolean(ARG_IS_FADING_EDGE_ENABLED, mRecyclerView.isVerticalFadingEdgeEnabled)
            mRecyclerView.isNestedScrollingEnabled = getBoolean(ARG_IS_NESTED_SCROLLING_ENABLED, mRecyclerView.isNestedScrollingEnabled)
        }

        mManager = mAdapter.suggestLayoutManager(mContext)
        mRecyclerView.layoutManager = mManager
        mRecyclerView.adapter = mAdapter
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

    fun getLayoutManager(): RecyclerView.LayoutManager {
        return mManager
    }

    override fun onPause() {
        childFragmentManager.run {
            (findFragmentByTag(ApiInfoPop.TAG) as BottomSheetDialogFragment?)?.dismiss()
        }
        super.onPause()
    }

    private fun actionDetails(context: Context, appInfo: ApiViewingApp) = lifecycleScope.launch(Dispatchers.Default) {
        val details = service.getAppDetails(context, appInfo)
        if (details.isEmpty()) return@launch
        val contentView = TextView(context)
        contentView.text = details
        contentView.textSize = 10f
        val padding = X.size(context, 20f, X.DP).toInt()
        contentView.setPadding(padding, padding, padding, 0)
        launch(Dispatchers.Main) {
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

    private fun actionIcon(context: Context, app: ApiViewingApp){
        val path = F.createPath(F.cachePublicPath(context), "App", "Logo", "${app.name}.png")
        val image = File(path)
        app.getOriginalIcon(context)?.let { if (F.prepare4(image)) X.savePNG(it, path) }
        val uri: Uri = image.getProviderUri(context)
//        val previewTitle = app.name // todo set preview title
        childFragmentManager.let {
            FilePop.by(context, uri, "image/png", R.string.textShareImage, uri, app.name).show(it, FilePop.TAG)
        }
    }

    // todo split APKs
    private fun actionApk(context: Context, app: ApiViewingApp){
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

/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.list

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.os.PreviewBuild
import com.madness.collision.main.MainViewModel
import com.madness.collision.main.showPage
import com.madness.collision.unit.api_viewing.MyBridge
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.data.codenameOrNull
import com.madness.collision.unit.api_viewing.data.verNameOrNull
import com.madness.collision.unit.api_viewing.stats.StatisticsFragment
import com.madness.collision.unit.api_viewing.ui.os.SystemModulesFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Stable
class CompOptionsEventHandlerImpl(
    private val viewModel: AppListViewModel,
    private val contentsLauncher: ActivityResultLauncher<String>,
    private val volumeLauncher: ActivityResultLauncher<Uri?>,
    private val context: Context,
) : CompositeOptionsEventHandler {
    override fun shareList() = viewModel.exportAppList(context)
    override fun showSettings() = context.showPage(MyBridge.getSettings())
    override fun updateTags(id: String, state: Boolean?) = viewModel.updateTagFilter(id, state)
    override fun toggleSrc(src: AppListSrc) = viewModel.toggleListSrc(src)

    override fun toggleApks() {
        when (viewModel.containsListSrc(AppListSrc.SelectApks)) {
            true -> viewModel.removeListSrc(AppListSrc.SelectApks)
            else -> contentsLauncher.launch("application/vnd.android.package-archive")
        }
    }

    override fun toggleFolder() {
        when (viewModel.containsListSrc(AppListSrc.SelectVolume)) {
            true -> viewModel.removeListSrc(AppListSrc.SelectVolume)
            else -> volumeLauncher.launch(null)
        }
    }

    override fun setOrder(order: AppListOrder) = viewModel.setListOrder(order)
    override fun setApiMode(apiMode: AppApiMode) = viewModel.setApiMode(apiMode)
}

private typealias GetContentsContract = ActivityResultContracts.GetMultipleContents

private class OpenVolumeContract : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return super.createIntent(context, input).apply {
            // make it possible to access children
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

@Composable
fun rememberCompOptionsEventHandler(viewModel: AppListViewModel): CompositeOptionsEventHandler {
    val conLauncher = rememberLauncherForActivityResult(GetContentsContract()) { uriList ->
        when {
            uriList.isEmpty() -> Unit  // ignore invalid selection silently
            else -> viewModel.toggleListSrc(AppListSrc.SelectApks(uriList))
        }
    }
    val volLauncher = rememberLauncherForActivityResult(OpenVolumeContract()) { uri ->
        when {
            uri == null -> Unit  // ignore invalid selection silently
            else -> viewModel.toggleListSrc(AppListSrc.SelectVolume(uri))
        }
    }
    val context = LocalContext.current
    return remember { CompOptionsEventHandlerImpl(viewModel, conLauncher, volLauncher, context) }
}


@Stable
class ListHeaderStateImpl(
    override val devInfoLabel: String,
    override val devInfoDesc: String,
    initListCat: ListSrcCat,
    private val viewModel: AppListViewModel,
    private val context: Context,
) : ListHeaderState {
    private var scrollY by mutableIntStateOf(0)
    private var mutListCat by mutableStateOf(initListCat)
    override var headerHeight: Int by mutableIntStateOf(0)
    override val headerOffsetY: Int by derivedStateOf { -(scrollY.coerceIn(0, headerHeight)) }
    override var statsSize: Int by mutableIntStateOf(0)
    override var listCat: ListSrcCat
        get() = mutListCat
        set(value) {
            mutListCat = value
            viewModel.setListSrcCat(value)
        }

    override fun updateOffsetY(scrollY: Int) { this.scrollY = scrollY }
    override fun showSystemModules() = context.showPage<SystemModulesFragment>()
    override fun showStats(options: AppListOptions) {
        getStatsFragment(options)?.let(context::showPage)
    }
    override fun onQueryChange(query: String) = viewModel.setQueryFilter(query)
}

private fun getStatsFragment(options: AppListOptions): StatisticsFragment? {
    val platformSrc = listOf(AppListSrc.SystemApps, AppListSrc.UserApps)
        .mapNotNull { src -> options.srcSet.find { it.key == src } }
    val platformApiUnit = when {
        platformSrc.isEmpty() -> return null
        platformSrc.size >= 2 -> ApiUnit.ALL_APPS
        platformSrc[0] == AppListSrc.SystemApps -> ApiUnit.SYS
        platformSrc[0] == AppListSrc.UserApps -> ApiUnit.USER
        else -> return null
    }
    return StatisticsFragment.newInstance(platformApiUnit)
}

private fun getAndroidVer(context: Context): Pair<String?, String> {
    val andVer = VerInfo(Build.VERSION.SDK_INT)
    val andPreview = PreviewBuild.codenameOrNull?.let { "$it Preview" }
    val version = andPreview ?: andVer.verNameOrNull?.let { "Android $it" }
    val apiDesc = listOfNotNull(
        "API ${andVer.apiText}", andVer.codenameOrNull(context)).joinToString()
    return version to apiDesc
}

@Composable
fun rememberListHeaderState(initListCat: ListSrcCat, viewModel: AppListViewModel): ListHeaderState {
    val context = LocalContext.current
    return remember {
        val (ver, desc) = getAndroidVer(context)
        ListHeaderStateImpl(ver.orEmpty(), desc, initListCat, viewModel, context)
    }
}


@Stable
class AppListStateImpl(
    initIsRefreshing: Boolean,
    initSrcCats: Set<ListSrcCat>,
    initOpUiState: AppListOpUiState
) : AppListState {
    override var isRefreshing: Boolean by mutableStateOf(initIsRefreshing)
    override var loadedCats: Set<ListSrcCat> by mutableStateOf(initSrcCats)
    override var opUiState: AppListOpUiState by mutableStateOf(initOpUiState)
}

@Composable
fun rememberAppListState(viewModel: AppListViewModel): AppListState {
    val mainViewModel = viewModel<MainViewModel>()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = remember {
        // set refreshing to true when initializing
        AppListStateImpl(
            initIsRefreshing = true,
            initSrcCats = setOf(ListSrcCat.Platform),
            initOpUiState = viewModel.opUiState.value
        )
    }
    LaunchedEffect(Unit) {
        viewModel.init(context, lifecycleOwner, mainViewModel.timestamp)
        viewModel.isLoadingSrc.onEach(listState::isRefreshing::set).launchIn(this)
        viewModel.loadedSrcCats.onEach(listState::loadedCats::set).launchIn(this)
        viewModel.opUiState.onEach(listState::opUiState::set).launchIn(this)
    }
    return listState
}

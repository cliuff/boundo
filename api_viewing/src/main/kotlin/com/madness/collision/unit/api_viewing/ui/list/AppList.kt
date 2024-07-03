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

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.chief.lang.mapIf
import com.madness.collision.unit.api_viewing.ComposeUnit
import com.madness.collision.util.FilePop
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.notifyBriefly
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class AppListFragment : ComposeUnit(), Democratic {
    override val id: String = "AV"

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
//        mainViewModel.configNavigation(toolbar, iconColor)
//        toolbar.setTitle(com.madness.collision.R.string.apiViewer)
        toolbar.isVisible = false
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
        if (OsUtils.satisfy(OsUtils.N)) {
            composeViewOwner.getView()?.setOnDragListener { _, event ->
                handleDragEvent(event)
                true
            }
        }
        val viewModel by viewModels<AppListViewModel>()
        viewModel.events
            .filterIsInstance<AppListEvent.ShareAppList>()
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach ev@{ event ->
                val context = context ?: return@ev
                val title = com.madness.collision.R.string.fileActionsShare
                FilePop.by(context, event.file, "text/csv", title, imageLabel = "App List")
                    .show(childFragmentManager, FilePop.TAG)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        LaunchMethod(arguments).run {
            if (mode == LaunchMethod.LAUNCH_MODE_SEARCH && textExtra != null) {
                viewModel.toggleListSrc(AppListSrc.DataSourceQuery(null, textExtra))
            } else if (mode == LaunchMethod.LAUNCH_MODE_LINK && dataStreamExtra != null) {
                viewModel.toggleListSrc(AppListSrc.SharedApk(dataStreamExtra))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (lifecycleEventTime.compareValues(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_CREATE) > 0) {
            val viewModel by viewModels<AppListViewModel>()
            context?.let(viewModel::checkListPrefs)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun handleDragEvent(event: DragEvent) {
        fun ClipData.toQueryOrUris() = run {
            if (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                if (itemCount > 0 && getItemAt(0).text.isNullOrEmpty().not()) {
                    return@run getItemAt(0).text to null
                }
            }
            null to (0..<itemCount).mapNotNull { i -> getItemAt(i).uri }
        }
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> notifyBriefly(R.string.apiDragDropHint)
            DragEvent.ACTION_DROP -> {
                val viewModel by viewModels<AppListViewModel>()
                event.clipData?.toQueryOrUris().let clip@{ pair ->
                    val (query, uriList) = pair ?: return@clip
                    if (query != null) {
                        viewModel.setQueryFilter(query)
                    } else if (uriList != null) {
                        val permissions = activity?.requestDragAndDropPermissions(event)
                        viewModel.toggleListSrc(AppListSrc.DragAndDrop(uriList)) {
                            permissions?.release()
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            AppList(paddingValues = rememberContentPadding())
        }
    }
}

@Composable
fun AppList(paddingValues: PaddingValues) {
    val viewModel = viewModel<AppListViewModel>()
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val appListPrefs by viewModel.appListId.collectAsStateWithLifecycle()
    val opUiState by viewModel.opUiState.collectAsStateWithLifecycle()
    val headerState = rememberListHeaderState(ListSrcCat.Platform, viewModel)
    AppListScaffold(
        listState = rememberAppListState(viewModel),
        eventHandler = rememberCompOptionsEventHandler(viewModel),
        paddingValues = paddingValues,
        contentOffsetProgress = -headerState.headerOffsetY,
    ) { loadedCats, contentPadding ->
        LegacyAppList(
            appList = appList,
            appListPrefs = appListPrefs,
            options = opUiState.options,
            loadedCats = loadedCats,
            headerState = headerState,
            paddingValues = contentPadding,
        )
    }
}

@Composable
private fun AppListV2(paddingValues: PaddingValues) {
    val viewModel = viewModel<AppListViewModel>()
    val artList by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))) {
        LazyColumn(contentPadding = paddingValues) {
            items(items = artList, key = { p -> p.packageName }) { art ->
                AppListItem(art = art)
            }
        }
    }
}

@Stable
interface AppListState {
    val isRefreshing: Boolean
    val loadedCats: Set<ListSrcCat>
    val opUiState: AppListOpUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListScaffold(
    listState: AppListState,
    eventHandler: CompositeOptionsEventHandler,
    paddingValues: PaddingValues,
    contentOffsetProgress: Int,
    content: @Composable (Set<ListSrcCat>, PaddingValues) -> Unit
) {
    var showListOptions by remember { mutableIntStateOf(0) }
    val toolbarHeight = with(LocalDensity.current) { 100.dp.toPx() }
    val toolbarOpacity by remember(contentOffsetProgress) {
        derivedStateOf {
            (contentOffsetProgress / toolbarHeight)
                .coerceIn(0f, 0.96f).mapIf({ it <= 0.06f }, { 0f })
        }
    }

    Scaffold(
        topBar = {
            AppListBar(
                isRefreshing = listState.isRefreshing,
                windowInsets = WindowInsets(top = 28.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarOpacity))
            ) {
                AppListBarAction(
                    icon = Icons.Outlined.CheckCircle,
                    label = listState.opUiState.options.srcSet.size.takeIf { it > 0 }?.toString(),
                    onClick = { showListOptions++ }
                )
            }
        },
        content = { contentPadding ->
            Box() {
                content(listState.loadedCats, contentPadding)
                ListOptionsDialog(
                    isShown = showListOptions,
                    options = listState.opUiState.options,
                    eventHandler = eventHandler
                )
            }
        }
    )
}

@PreviewCombinedColorLayout
@Composable
private fun AppListPreview() {
    val listState = remember {
        val options = AppListOptions(
            listOf(AppListSrc.SystemApps), AppListOrder.UpdateTime, AppApiMode.Target)
        AppListStateImpl(
            initIsRefreshing = false,
            initSrcCats = emptySet(),
            initOpUiState = AppListOpUiState(options)
        )
    }
    BoundoTheme {
        AppListScaffold(
            listState = listState,
            eventHandler = remember { PseudoCompOptionsEventHandler() },
            paddingValues = PaddingValues(),
            contentOffsetProgress = 50,
            content = { _, _ -> Box(Modifier.fillMaxSize().background(Color.DarkGray)) }
        )
    }
}

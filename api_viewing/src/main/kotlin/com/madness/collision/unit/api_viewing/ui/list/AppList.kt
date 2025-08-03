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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.madness.collision.chief.app.asInsets
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.chief.lang.mapIf
import com.madness.collision.diy.SpanAdapter
import com.madness.collision.unit.api_viewing.ComposeUnit
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.ui.home.AppHomeNavPage
import com.madness.collision.unit.api_viewing.ui.home.AppHomeNavPageImpl
import com.madness.collision.unit.api_viewing.ui.info.AppInfoEventHandler
import com.madness.collision.unit.api_viewing.ui.info.AppInfoFragment
import com.madness.collision.unit.api_viewing.ui.info.AppInfoSheet
import com.madness.collision.unit.api_viewing.ui.info.ListStateAppOwner
import com.madness.collision.unit.api_viewing.ui.info.LocalAppInfoCallback
import com.madness.collision.unit.api_viewing.ui.info.rememberAppInfoEventHandler
import com.madness.collision.unit.api_viewing.ui.info.rememberAppInfoState
import com.madness.collision.util.F
import com.madness.collision.util.FilePop
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.mainApplication
import com.madness.collision.util.notifyBriefly
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

open class AppListFragment : ComposeUnit(), Democratic, AppHomeNavPage by AppHomeNavPageImpl() {
    override val id: String = "AV"

    private var listHeaderDarkIcon: Boolean = false

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
//        mainViewModel.configNavigation(toolbar, iconColor)
//        toolbar.setTitle(com.madness.collision.R.string.apiViewer)
        toolbar.isVisible = false
        return true
    }

    private fun getListHeaderDarkIcon(context: Context): Boolean {
        val f = F.createFile(F.valFilePubExterior(context), "Art_ListHeader.jpg")
        // light status bar icons for custom header images
        if (f.exists()) return false
        val sealIndex = Utils.getDevCodenameLetter()
            ?: Utils.getAndroidLetterByAPI(Build.VERSION.SDK_INT)
        return when (sealIndex) {
            'm', 'n', 'q', 'u' -> true
            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { ctx ->
            val isDark = getListHeaderDarkIcon(ctx).also { listHeaderDarkIcon = it }
            setStatusBarDarkIcon(isDark)
        }
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
            val src = if (mode == LaunchMethod.LAUNCH_MODE_SEARCH && textExtra != null) {
                AppListSrc.DataSourceQuery(null, textExtra)
            } else if (mode == LaunchMethod.LAUNCH_MODE_LINK && dataStreamExtra != null) {
                AppListSrc.SharedApk(dataStreamExtra)
            } else {
                return@run
            }
            // launch in default context and wait for view model init completed
            lifecycleScope.launch { viewModel.toggleListSrc(src) }
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
            val appInfoEventHandler = rememberAppInfoEventHandler(this)
            AppList(
                eventHandler = rememberAppListEventHandler(appInfoEventHandler),
                paddingValues = navContentPadding,
            )
        }
    }

    @Composable
    private fun rememberAppListEventHandler(appInfoEventHandler: AppInfoEventHandler) =
        remember<AppListEventHandler> {
            object : AppListEventHandler, AppInfoEventHandler by appInfoEventHandler {
                private val host = this@AppListFragment

                override fun getMaxSpan(): Int {
                    return SpanAdapter.getSpanCount(host, 290f)
                }

                override fun onAppBarOpacityChange(opacity: Float) {
                    val isDark = when {
                        opacity < 0.4f -> listHeaderDarkIcon
                        else -> mainApplication.isPaleTheme
                    }
                    setStatusBarDarkIcon(isDark)
                }
            }
        }
}

@Stable
interface AppListEventHandler : AppInfoEventHandler {
    fun getMaxSpan(): Int
    fun onAppBarOpacityChange(opacity: Float)
}

@Composable
fun AppList(eventHandler: AppListEventHandler, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val viewModel = viewModel<AppListViewModel>()
    val appInfoState = rememberAppInfoState()
    val appInfoCallback = remember(viewModel) {
        object : AppInfoFragment.Callback {
            override fun getAppOwner(): AppInfoFragment.AppOwner {
                return ListStateAppOwner(viewModel.appList::value) { pkgName ->
                    viewModel.getApp(context, pkgName)
                }
            }
        }
    }

    Box {
        AppListPrimary(
            eventHandler = eventHandler,
            showAppInfo = { appInfoState.app = it },
            paddingValues = paddingValues,
        )

        CompositionLocalProvider(LocalAppInfoCallback provides appInfoCallback) {
            AppInfoSheet(
                state = appInfoState,
                onDismissRequest = { appInfoState.app = null },
                eventHandler = eventHandler,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListPrimary(
    eventHandler: AppListEventHandler,
    showAppInfo: (ApiViewingApp) -> Unit,
    paddingValues: PaddingValues,
) {
    val viewModel = viewModel<AppListViewModel>()
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val appListConfig by viewModel.appListConfig.collectAsStateWithLifecycle()
    val appSrcState by viewModel.appSrcState.collectAsStateWithLifecycle()
    val opUiState by viewModel.opUiState.collectAsStateWithLifecycle()
    val headerState = rememberListHeaderState(viewModel)
    val appBarState = rememberTopAppBarState()
    val scrollState = rememberLazyGridState()
    AppListScaffold(
        listState = rememberAppListState(viewModel),
        eventHandler = rememberCompOptionsEventHandler(viewModel),
        appBarState = appBarState,
        listScrollState = scrollState,
        paddingValues = paddingValues,
        onAppBarOpacityChange = eventHandler::onAppBarOpacityChange,
    ) { contentPadding ->
        AppListGrid(
            appList = appList,
            onClickApp = showAppInfo,
            getMaxSpan = eventHandler::getMaxSpan,
            listConfig = appListConfig,
            options = opUiState.options,
            appSrcState = appSrcState,
            headerState = headerState,
            appBarState = appBarState,
            scrollState = scrollState,
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
    val opUiState: AppListOpUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListScaffold(
    listState: AppListState,
    eventHandler: CompositeOptionsEventHandler,
    appBarState: TopAppBarState = rememberTopAppBarState(),
    listScrollState: LazyGridState = rememberLazyGridState(),
    paddingValues: PaddingValues = PaddingValues(),
    onAppBarOpacityChange: (Float) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    var showListOptions by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val density = LocalDensity.current
            val toolbarOpacity by remember(density, appBarState) {
                val toolbarHeight = with(density) { 100.dp.toPx() }
                val headerHeight = (toolbarHeight * 1.8f).roundToInt()
                derivedStateOf {
                    val fraction = if (listScrollState.firstVisibleItemIndex == 0) {
                        // use first item offset when scrolled to top
                        (listScrollState.firstVisibleItemScrollOffset - headerHeight) / toolbarHeight
                    } else {
                        (-appBarState.contentOffset - headerHeight) / toolbarHeight
                    }
                    fraction.coerceIn(0f, 0.96f).mapIf({ it <= 0.06f }, { 0f })
                }
            }
            LaunchedEffect(toolbarOpacity) {
                onAppBarOpacityChange(toolbarOpacity)
            }

            val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarOpacity)
            AppListBar(
                isRefreshing = listState.isRefreshing,
                windowInsets = paddingValues.asInsets()
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor, scrolledContainerColor = containerColor),
                scrollBehavior = scrollBehavior,
            ) {
                AppListBarAction(
                    icon = Icons.Outlined.CheckCircle,
                    label = listState.opUiState.options.srcSet.size.takeIf { it > 0 }?.toString(),
                    onClick = { showListOptions++ }
                )
            }
        },
        containerColor = Color.Transparent,
        content = { contentPadding ->
            Box() {
                content(PaddingValues(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    top = contentPadding.calculateTopPadding() + 5.dp,
                    bottom = paddingValues.calculateBottomPadding() + 20.dp))
                ListOptionsDialog(
                    isShown = showListOptions,
                    options = listState.opUiState.options,
                    eventHandler = eventHandler,
                    windowInsets = paddingValues.asInsets()
                        .only(WindowInsetsSides.Bottom),
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewCombinedColorLayout
@Composable
private fun AppListPreview() {
    val listState = remember {
        val options = AppListOptions(
            listOf(AppListSrc.SystemApps), AppListOrder.UpdateTime, AppApiMode.Target)
        AppListStateImpl(
            initIsRefreshing = false,
            initOpUiState = AppListOpUiState(options)
        )
    }
    BoundoTheme {
        AppListScaffold(
            listState = listState,
            eventHandler = remember { PseudoCompOptionsEventHandler() },
            content = { _ -> Box(Modifier.fillMaxSize().background(Color.DarkGray)) }
        )
    }
}

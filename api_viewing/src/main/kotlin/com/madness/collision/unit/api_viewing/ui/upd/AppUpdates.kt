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

package com.madness.collision.unit.api_viewing.ui.upd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.UpdatedApp
import com.madness.collision.unit.api_viewing.ui.upd.item.ApiUpdGuiArt
import com.madness.collision.unit.api_viewing.ui.upd.item.UpdGuiArt
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.unit.api_viewing.upgrade.new
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.mainApplication

@Stable
interface AppUpdatesEventHandler {
    fun hasUsageAccess(): Boolean
    fun refreshUpdates()
    fun showAppInfo(app: ApiViewingApp)
    fun showAppListPage()
    fun showUsageAccessSettings()
    fun requestAllPkgsQuery(permission: String?)
    fun showAppSettings()
    @Composable
    fun UnitBar(width: Dp)
}

@Composable
fun AppUpdatesPage(paddingValues: PaddingValues, eventHandler: AppUpdatesEventHandler) {
    val viewModel: AppUpdatesViewModel = viewModel()
    val updatesUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appListPrefs by viewModel.appListId.collectAsStateWithLifecycle()
    val (isLoading, sections, permState) = updatesUiState
    Scaffold(
        topBar = {
            BoxWithConstraints {
                UpdatesAppBar(
                    refreshing = isLoading,
                    onClickRefresh = eventHandler::refreshUpdates,
                    onClickSettings = eventHandler::showAppSettings,
                    windowInsets = WindowInsets(
                        top = paddingValues.calculateTopPadding(),
                        left = paddingValues.calculateLeftPadding(LocalLayoutDirection.current),
                        right = paddingValues.calculateRightPadding(LocalLayoutDirection.current)),
                ) {
                    val horizontalPadding = LocalLayoutDirection.current.let { di ->
                        paddingValues.run { calculateLeftPadding(di) + calculateRightPadding(di) }
                    }
                    val maxContentWidth = maxWidth - horizontalPadding
                    val barWidth = when {
                        maxContentWidth >= 360.dp -> (maxContentWidth - 40.dp).coerceAtMost(400.dp)
                        else -> maxContentWidth - 20.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            // scrollable in small split screen
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        content = {
                            with(eventHandler) { UnitBar(width = barWidth) }
                        }
                    )
                }
            }
        },
        containerColor = if (mainApplication.isDarkTheme) Color(0xFF050505) else Color(0xFFFCFCFC),
        content = { contentPadding ->
            val hasUsageAccess = eventHandler.hasUsageAccess()
            BoxWithConstraints {
                val horizontalPadding = LocalLayoutDirection.current.let { di ->
                    paddingValues.run { calculateLeftPadding(di) + calculateRightPadding(di) }
                }
                val appItemStyle = when {
                    viewModel.columnCount > 1 -> DefaultAppItemStyle
                    maxWidth - horizontalPadding >= 360.dp -> DefaultAppItemStyle
                    else -> CompactAppItemStyle
                }
                CompositionLocalProvider(
                    LocalAppItemStyle provides appItemStyle,
                    LocalAppItemPrefs provides appListPrefs,
                ) {
                    // always show list to animate appear/disappear
                    // if (sections.isNotEmpty() || !hasUsageAccess)
                    UpdatesList(
                        sections = sections,
                        columnCount = viewModel.columnCount,
                        paddingValues = PaddingValues(
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            top = contentPadding.calculateTopPadding() + 5.dp,
                            bottom = paddingValues.calculateBottomPadding() + 20.dp
                        ),
                        onClickApp = eventHandler::showAppInfo,
                        onClickViewMore = eventHandler::showAppListPage,
                        onClickUsageAccess = eventHandler::showUsageAccessSettings
                            // hide usage access when all pkgs query not granted yet
                            .takeIf { !hasUsageAccess && permState.canQueryAllPkgs },
                        onClickInstalledAppsQuery =
                            { eventHandler.requestAllPkgsQuery(permState.queryPermission) }
                            .takeIf { !permState.canQueryAllPkgs },
                    )
                }
            }
            // skip the default not loading state before loading updates
            var init by remember { mutableIntStateOf(0) }; SideEffect { init++ }
            val isNothing = sections.isEmpty() && hasUsageAccess && !isLoading && init > 0
            AnimatedVisibility(visible = isNothing, enter = fadeIn(), exit = fadeOut()) {
                UpdateNothing(modifier = Modifier.fillMaxWidth().padding(PaddingValues(
                    top = contentPadding.calculateTopPadding() + 13.dp,
                    bottom = paddingValues.calculateBottomPadding() + 20.dp
                )))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatesAppBar(
    refreshing: Boolean,
    onClickRefresh: () -> Unit,
    onClickSettings: () -> Unit,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    unitBarContent: @Composable () -> Unit,
) {
    val appBarColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xff0c0c0c) else Color.White
        false -> if (mainApplication.isDarkTheme) Color(0xff0c0c0c) else Color.White
    }
    val appBarDividerColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFF505050) else Color(0xFFC0C0C0)
        false -> if (mainApplication.isDarkTheme) Color(0xFF505050) else Color(0xFFC0C0C0)
    }
    val appBarIconColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFFD0D6DB) else Color(0xFF353535)
        false -> if (mainApplication.isDarkTheme) Color(0xFFD0D6DB) else Color(0xFF353535)
    }
    Column {
        TopAppBar(
            title = {},
            actions = {
                val rotation by rememberOvershootRotation(refreshing)
                var showUnitBar by remember { mutableStateOf(false) }
                IconButton(modifier = Modifier.rotate(rotation), onClick = onClickRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = appBarIconColor,
                    )
                }
                IconButton(onClick = { showUnitBar = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = appBarIconColor,
                    )
                }
                if (showUnitBar) {
                    Popup(
                        offset = with(LocalDensity.current) { IntOffset(0, 40.dp.roundToPx()) },
                        onDismissRequest = { showUnitBar = false },
                    ) {
                        // reserve 8dp padding for all 4 sides
                        PopupCard(modifier = Modifier.padding(8.dp)) { unitBarContent() }
                    }
                }
                val endPadding = windowInsets.asPaddingValues()
                    .calculateEndPadding(LocalLayoutDirection.current)
                    .let { if (it >= 5.dp) 2.dp else 5.dp }
                Spacer(modifier = Modifier.width(endPadding))
            },
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = appBarColor.copy(alpha = 0.87f)),
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = appBarDividerColor.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun PopupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = content,
    )
}

@Composable
private fun UpdatesList(
    sections: AppUpdatesSections,
    columnCount: Int,
    paddingValues: PaddingValues,
    onClickApp: (ApiViewingApp) -> Unit,
    onClickViewMore: () -> Unit,
    onClickUsageAccess: (() -> Unit)?,
    onClickInstalledAppsQuery: (() -> Unit)?,
) {
    var lastSectionCount by remember { mutableIntStateOf(0) }
    val contentPadding = paddingValues.run {
        PaddingValues(
            top = calculateTopPadding(),
            bottom = calculateBottomPadding(),
            start = 3.dp + calculateStartPadding(LocalLayoutDirection.current).coerceAtLeast(7.dp),
            end = 3.dp + calculateEndPadding(LocalLayoutDirection.current).coerceAtLeast(7.dp),
        )
    }
    if (columnCount <= 1) {
        // use lazy column for simpler (thus faster) impl
        val listState = rememberLazyListState()
        LazyColumn(state = listState, contentPadding = contentPadding) {
            if (onClickInstalledAppsQuery != null) {
                item(key = "@upd.btn0") {
                    UpdatesRedirectButton(type = 3, onClickInstalledAppsQuery, Modifier.animateItem())
                }
            }
            for ((secIndex, secList) in sections) {
                if (secList.isNotEmpty()) {
                    item(key = "@upd.sec${secIndex.ordinal}") {
                        UpdatesSectionTitle(index = secIndex, Modifier.animateItem())
                    }
                    sectionItems(secIndex, secList, onClickApp) { list, key, content ->
                        items(list, key) { item -> content(item, Modifier.animateItem()) }
                    }
                }
            }
            if (onClickUsageAccess != null) {
                item(key = "@upd.btn2") {
                    UpdatesRedirectButton(type = 2, onClickUsageAccess, Modifier.animateItem())
                }
            }
        }
        LaunchedEffect(sections) {
            // scroll to the top of the new list from existing usage access button
            if (lastSectionCount == 0 && sections.isNotEmpty()) listState.scrollToItem(0)
            lastSectionCount = sections.size
        }
    } else {
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            state = gridState, contentPadding = contentPadding) {
            if (onClickInstalledAppsQuery != null) {
                // placeholder to make installed apps query button be positioned in a new row's cell
                item(key = "@upd.row0", span = MaxLineSpan) {
                    Spacer(modifier = Modifier.animateItem())
                }
                item(key = "@upd.btn0") {
                    UpdatesRedirectButton(type = 3, onClickInstalledAppsQuery, Modifier.animateItem())
                }
            }
            for ((secIndex, secList) in sections) {
                if (secList.isNotEmpty()) {
                    item(key = "@upd.sec${secIndex.ordinal}", span = MaxLineSpan) {
                        UpdatesSectionTitle(index = secIndex, Modifier.animateItem())
                    }
                    sectionItems(secIndex, secList, onClickApp) { list, key, content ->
                        items(list, key) { item -> content(item, Modifier.animateItem()) }
                    }
                }
            }
            if (onClickUsageAccess != null) {
                // placeholder to make usage access button be positioned in a new row's cell
                item(key = "@upd.row1", span = MaxLineSpan) {
                    Spacer(modifier = Modifier.animateItem())
                }
                item(key = "@upd.btn2") {
                    UpdatesRedirectButton(type = 2, onClickUsageAccess, Modifier.animateItem())
                }
            }
        }
        LaunchedEffect(sections) {
            // scroll to the top of the new list from existing usage access button
            if (lastSectionCount == 0 && sections.isNotEmpty()) gridState.scrollToItem(0)
            lastSectionCount = sections.size
        }
    }
}

private val MaxLineSpan: LazyGridItemSpanScope.() -> GridItemSpan
        = { GridItemSpan(maxLineSpan) }

@Composable
private fun UpdatesSectionTitle(index: AppUpdatesIndex, modifier: Modifier = Modifier) {
    UpdateSectionTitle(
        modifier = modifier
            .padding(top = 5.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        text = updateIndexLabel(index)
    )
}

@Composable
private fun UpdatesRedirectButton(type: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    when (type) {
        1 -> {
            MoreUpdatesButton(
                modifier = modifier
                    .padding(top = 5.dp)
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                onClick = onClick,
            )
        }
        2 -> {
            UsageAccessRequest(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp)
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                onClick = onClick,
            )
        }
        3 -> {
            QueryInstalledAppsRequest(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                onClick = onClick,
            )
        }
    }
}

private inline fun <T : UpdatedApp> sectionItems(
    secIndex: AppUpdatesIndex,
    secList: List<T>,
    noinline onClickApp: (ApiViewingApp) -> Unit,
    items: (items: List<T>, key: (T) -> Any, itemContent: @Composable (T, Modifier) -> Unit) -> Unit,
) {
    if (secIndex == AppUpdatesIndex.UPG) {
        items(secList, { upd -> (upd as Upgrade).new.packageName + secIndex.ordinal }
        ) { upd, modifier ->
            if (upd is Upgrade) {
                val context = LocalContext.current
                val itemPrefs = LocalAppItemPrefs.current
                var lastArt: ApiUpdGuiArt? by remember { mutableStateOf(null) }
                val art = remember(upd, itemPrefs) {
                    val art = lastArt
                    val updatedArt = when {
                        itemPrefs > 0 && art != null -> art.withUpdatedTags(upd.new, context)
                        else -> upd.toGuiArt(context) { onClickApp(upd.new) }
                    }
                    updatedArt.also { lastArt = it }
                }
                AppUpdateItem(
                    modifier = modifier.padding(horizontal = 5.dp, vertical = 5.dp),
                    art = art,
                )
            }
        }
    } else {
        items(secList, { upd -> upd.app.packageName + secIndex.ordinal }
        ) { upd, modifier ->
            val app = upd.app
            if (true) {
                val context = LocalContext.current
                val itemPrefs = LocalAppItemPrefs.current
                var lastArt: UpdGuiArt? by remember { mutableStateOf(null) }
                val art = remember(app, itemPrefs) {
                    val art = lastArt
                    val updatedArt = when {
                        itemPrefs > 0 && art != null -> art.withUpdatedTags(app, context)
                        else -> app.toGuiArt(context) { onClickApp(app) }
                    }
                    updatedArt.also { lastArt = it }
                }
                AppItem(
                    modifier = modifier.padding(horizontal = 5.dp, vertical = 5.dp),
                    art = art,
                )
            }
        }
    }
}

@Composable
@PreviewCombinedColorLayout
private fun AppUpdatesPreview() {
    BoundoTheme {
        Scaffold(
            topBar = {
                UpdatesAppBar(refreshing = true, onClickRefresh = {}, onClickSettings = {}) {}
            },
            content = { contentPadding ->
                UpdatesList(
                    sections = emptyMap(),
                    columnCount = 1,
                    paddingValues = PaddingValues(top = contentPadding.calculateTopPadding() + 5.dp),
                    onClickApp = {},
                    onClickViewMore = {},
                    onClickUsageAccess = {},
                    onClickInstalledAppsQuery = {},
                )
            }
        )
    }
}

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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.util.dev.PreviewCombinedColorLayout

@Stable
interface AppUpdatesEventHandler {
    fun hasUsageAccess(): Boolean
    fun refreshUpdates()
    fun showAppInfo(app: ApiViewingApp)
    fun showAppListPage()
    fun showUsageAccessSettings()
    fun showAppSettings()
    @Composable
    fun UnitBar(width: Dp)
}

@Composable
fun AppUpdatesPage(paddingValues: PaddingValues, eventHandler: AppUpdatesEventHandler) {
    val viewModel: AppUpdatesViewModel = viewModel()
    val updatesUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (isLoading, sections) = updatesUiState
    Scaffold(
        topBar = {
            BoxWithConstraints {
                UpdatesAppBar(
                    refreshing = isLoading,
                    onClickRefresh = eventHandler::refreshUpdates,
                    onClickSettings = eventHandler::showAppSettings,
                    windowInsets = WindowInsets(top = paddingValues.calculateTopPadding()),
                ) {
                    val width = maxWidth - 40.dp
                    Box(modifier = Modifier.widthIn(max = width)) {
                        with(eventHandler) { UnitBar(width = width) }
                    }
                }
            }
        },
        content = { contentPadding ->
            val hasUsageAccess = eventHandler.hasUsageAccess()
            if (sections.isNotEmpty() || !hasUsageAccess) {
                UpdatesList(
                    sections = sections,
                    columnCount = viewModel.columnCount,
                    paddingValues = PaddingValues(
                        top = contentPadding.calculateTopPadding() + 5.dp,
                        bottom = paddingValues.calculateBottomPadding() + 20.dp
                    ),
                    onClickApp = eventHandler::showAppInfo,
                    onClickViewMore = eventHandler::showAppListPage,
                    onClickUsageAccess = eventHandler::showUsageAccessSettings
                        .takeIf { !hasUsageAccess },
                )
            } else {
                Text("Nada")
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
    windowInsets: WindowInsets = WindowInsets(0),
    unitBarContent: @Composable () -> Unit,
) {
    TopAppBar(
        title = {},
        actions = {
            val rotation by rememberOvershootRotation(refreshing)
            var showUnitBar by remember { mutableStateOf(false) }
            IconButton(modifier = Modifier.rotate(rotation), onClick = onClickRefresh) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            IconButton(onClick = { showUnitBar = true }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            DropdownMenu(expanded = showUnitBar, onDismissRequest = { showUnitBar = false }) {
                unitBarContent()
            }
            Spacer(modifier = Modifier.width(5.dp))
        },
        windowInsets = windowInsets,
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
) {
    LazyVerticalGrid(columns = GridCells.Fixed(columnCount), contentPadding = paddingValues) {
        for ((secIndex, secList) in sections) {
            if (secList.isNotEmpty()) {
                item(key = secIndex, span = { GridItemSpan(maxLineSpan) }) {
                    UpdateSectionTitle(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        text = updateIndexLabel(secIndex)
                    )
                }
                sectionItems(secIndex, secList, onClickApp)
            }
        }
        if (sections.any { (_, list) -> list.isNotEmpty() }) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MoreUpdatesButton(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    onClick = onClickViewMore,
                )
            }
        }
        if (onClickUsageAccess != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UsageAccessRequest(
                    modifier = Modifier
                        .widthIn(max = 450.dp)
                        .fillMaxWidth()
                        .clickable(onClick = onClickUsageAccess)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun LazyGridScope.sectionItems(
    secIndex: AppUpdatesIndex,
    secList: List<*>,
    onClickApp: (ApiViewingApp) -> Unit,
) {
    if (secIndex == AppUpdatesIndex.UPG) {
        items(secList) { upd ->
            if (upd is Upgrade) {
                val context = LocalContext.current
                AppUpdateItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    art = remember(upd) { upd.toGuiArt(context) { onClickApp(upd.new) } },
                )
            }
        }
    } else {
        items(secList) { app ->
            if (app is ApiViewingApp) {
                val context = LocalContext.current
                AppItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    art = remember(app) { app.toGuiArt(context) { onClickApp(app) } },
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
                    paddingValues = PaddingValues(top = contentPadding.calculateTopPadding()),
                    onClickApp = {},
                    onClickViewMore = {},
                    onClickUsageAccess = {},
                )
            }
        )
    }
}

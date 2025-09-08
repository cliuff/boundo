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

package com.madness.collision.unit.api_viewing.ui.org.coll

import android.content.pm.PackageInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.chief.app.asInsets
import com.madness.collision.chief.app.LocalPageNavController
import com.madness.collision.chief.layout.share
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.ui.org.OrgRouteId
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.mainApplication
import com.madness.collision.util.ui.AppIconPackageInfo
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import io.cliuff.boundo.org.model.CompColl
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgCollPage(contentPadding: PaddingValues = PaddingValues()) {
    val viewModel = viewModel<OrgCollViewModel>()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.init(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (isLoading, coll, collList, installedPkgsSummary) = uiState
    val navController = LocalPageNavController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    OrgCollScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            OrgCollAppBar(
                collList = collList,
                selectedColl = coll,
                onClickColl = { coll -> viewModel.selectColl(coll, context) },
                onActionDelete = { coll?.let(viewModel::deleteColl) },
                onActionImport = { viewModel.importColl(context) },
                onActionExport = { coll?.let { viewModel.exportColl(it, context) } },
                windowInsets = contentPadding.asInsets()
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .share(WindowInsets(top = 5.dp)),
                scrollBehavior = scrollBehavior,
            )
        },
        onClickAdd = {
            // add group for coll, or create a new coll then add
            val route = OrgRouteId.NewGroup(coll?.id ?: -1)
            navController.navigateTo(route.asRoute())
        },
        contentWindowInsets = contentPadding.asInsets()
            .add(WindowInsets(top = 10.dp, bottom = 20.dp)),
    ) { innerPadding ->
        if (coll != null) {
            OrgCollContent(
                modifier = Modifier.fillMaxSize(),
                coll = coll,
                onClickGroup = clickGroup@{ i, id ->
                    if (i !in coll.groups.indices) return@clickGroup
                    val route = OrgRouteId.GroupInfo(coll.groups[i], coll.id, id)
                    navController.navigateTo(route.asRoute())
                },
                // keep null values (uninstalled) to show in grid cells
                getPkgsForGroup = { pkgs -> pkgs.map(viewModel::getPkg) },
                installedAppsSummary = installedPkgsSummary?.let { (a, b) -> "$a/$b" } ?: "N/A",
                contentPadding = innerPadding,
            )
        } else if (!isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = stringResource(com.madness.collision.R.string.text_no_content),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OrgCollScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    onClickAdd: () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier
                    .windowInsetsPadding(contentWindowInsets.only(WindowInsetsSides.End)),
                onClick = onClickAdd,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

@Composable
private fun OrgCollContent(
    coll: CompColl,
    onClickGroup: (i: Int, id: Int) -> Unit,
    modifier: Modifier = Modifier,
    installedAppsSummary: String = "N/A",
    getPkgsForGroup: ((List<String>) -> List<PackageInfo?>) =
        { pkgs -> arrayOfNulls<PackageInfo>(pkgs.size).toList() },
    contentPadding: PaddingValues = PaddingValues(),
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val hazeStyle = remember(surfaceColor) {
        HazeStyle(
            backgroundColor = if (mainApplication.isDarkTheme) Color(0xFF191919) else Color.White,
            tint = HazeTint(surfaceColor.copy(if (mainApplication.isDarkTheme) 0.5f else 0.3f)),
            blurRadius = 35.dp,
            noiseFactor = 0.08f,
            fallbackTint = HazeTint(surfaceColor),
        )
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val cellWidth = remember {
        when {
            maxWidth >= 840.dp -> 200.dp
            maxWidth >= 600.dp -> 170.dp
            maxWidth >= 360.dp -> 170.dp
            else -> 160.dp
        }
    }
    LazyVerticalGrid(
        // fillMaxSize to intercept scroll gestures in empty areas
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(cellWidth),
        contentPadding = contentPadding.run {
            PaddingValues(
                top = calculateTopPadding(),
                bottom = calculateBottomPadding(),
                start = 3.dp + calculateStartPadding(LocalLayoutDirection.current).coerceAtLeast(4.dp),
                end = 3.dp + calculateEndPadding(LocalLayoutDirection.current).coerceAtLeast(4.dp),
            )
        },
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "AppsOverview") {
            val navController = LocalPageNavController.current
            CollAppsSummary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                text = stringResource(R.string.org_coll_installed_sum, installedAppsSummary),
                onClick = {
                    val route = OrgRouteId.CollAppList(coll)
                    navController.navigateTo(route.asRoute())
                },
            )
        }
        itemsIndexed(coll.groups, key = { _, g -> g.id }, contentType = { _, _ -> "Group" }) { i, group ->
            CollGroup(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                name = group.name,
                appCount = group.apps.size,
                apps = getPkgsForGroup(group.apps.take(3).map(OrgApp::pkg)),
                onClick = { onClickGroup(i, group.id) },
                appHazeState = remember { HazeState() },
                appHazeStyle = hazeStyle,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "Space") {
            Spacer(modifier = Modifier.height((maxHeight / 4).coerceIn(80.dp, 120.dp)))
        }
    }
    }
}

@Composable
private fun CollAppsSummary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            modifier = Modifier,
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun CollGroup(
    name: String,
    appCount: Int,
    apps: List<PackageInfo?>,
    onClick: () -> Unit,
    appHazeState: HazeState,
    modifier: Modifier = Modifier,
    appHazeStyle: HazeStyle = HazeStyle.Unspecified,
) {
    SubcomposeLayers(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        lowerLayer = { size ->
            Box(modifier = Modifier
                .size(with(LocalDensity.current) { DpSize(size.width.toDp(), size.height.toDp()) })
                .hazeEffect(appHazeState, style = appHazeStyle) {
                    inputScale = HazeInputScale.Fixed(0.8f)
                })
        },
    ) {
    Column {
        Row(modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 12.dp)) {
            Text(
                modifier = Modifier.alignByBaseline().weight(1f, fill = false),
                text = "$name ",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                modifier = Modifier.alignByBaseline().widthIn(max = 50.dp),
                text = appCount.toString(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontSize = 16.sp,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hazeSource(appHazeState)
                .padding(start = 20.dp, top = 20.dp, end = 4.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // placeholder to match height with neighboring cells
            if (apps.isEmpty()) {
                Box(modifier = Modifier.size(28.dp))
            }
            for (i in apps.indices) {
                val app = apps[i]
                val icPkg = remember { app?.applicationInfo?.let { AppIconPackageInfo(app, it) } }
                if (i > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (icPkg != null) {
                    AsyncImage(
                        modifier = Modifier.width(28.dp).heightIn(max = 28.dp),
                        model = icPkg,
                        contentDescription = null,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), shape = CircleShape)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@PreviewCombinedColorLayout
private fun OrgCollPreview() {
    val anApp = OrgApp(pkg = "", label = "", labelLocale = "", createTime = 0L, modifyTime = 0L)
    val groups = listOf(
        OrgGroup(id = 0, name = "Group 1", createTime = 0L, modifyTime = 0L, apps = listOf(anApp)),
        OrgGroup(id = 1, name = "Group 2", createTime = 0L, modifyTime = 0L, apps = listOf(anApp)),
        OrgGroup(id = 2, name = "Group 3", createTime = 0L, modifyTime = 0L, apps = listOf(anApp)),
        OrgGroup(id = 3, name = "Group with a Looooooooooooog Name", createTime = 0L, modifyTime = 0L, apps = listOf(anApp)),
    )
    val coll = CompColl(id = 0, name = "Collection 1", createTime = 0L, modifyTime = 0L, groups = groups)
    val coll1 = CompColl(id = 1, name = "Collection 2", createTime = 0L, modifyTime = 0L, groups = emptyList())
    BoundoTheme {
        OrgCollScaffold(
            topBar = { OrgCollAppBar(collList = listOf(coll, coll1), selectedColl = coll) },
        ) { innerPadding ->
            OrgCollContent(
                coll = coll,
                onClickGroup = { _, _ -> },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = innerPadding,
            )
        }
    }
}

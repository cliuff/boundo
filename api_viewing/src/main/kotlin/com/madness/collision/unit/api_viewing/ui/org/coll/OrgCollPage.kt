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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.chief.app.asInsets
import com.madness.collision.chief.app.LocalPageNavController
import com.madness.collision.unit.api_viewing.ui.org.OrgRouteId
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.ui.AppIconPackageInfo
import io.cliuff.boundo.org.model.CompColl
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup

@Composable
fun OrgCollPage(contentPadding: PaddingValues = PaddingValues()) {
    val viewModel = viewModel<OrgCollViewModel>()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.init(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (isLoading, coll, collList, installedPkgsSummary) = uiState
    val navController = LocalPageNavController.current
    OrgCollScaffold(
        topBar = {
            OrgCollAppBar(
                collList = collList,
                selectedColl = coll,
                onClickColl = { coll -> viewModel.selectColl(coll, context) },
                onActionDelete = { coll?.let(viewModel::deleteColl) },
                onActionImport = { viewModel.importColl(context) },
                onActionExport = { coll?.let { viewModel.exportColl(it, context) } },
                windowInsets = contentPadding.asInsets()
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
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
                modifier = Modifier.fillMaxWidth(),
                coll = coll,
                onClickGroup = clickGroup@{ i, id ->
                    if (i !in coll.groups.indices) return@clickGroup
                    val route = OrgRouteId.GroupInfo(coll.groups[i], coll.id, id)
                    navController.navigateTo(route.asRoute())
                },
                installedAppsSummary = installedPkgsSummary?.let { (a, b) -> "$a/$b" } ?: "N/A",
                contentPadding = innerPadding,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
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
    topBar: @Composable () -> Unit = {},
    onClickAdd: () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
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
    contentPadding: PaddingValues = PaddingValues(),
) {
    val viewModel = viewModel<OrgCollViewModel>()
    val groupPkgs = remember(coll.groups) {
        coll.groups.map { group ->
            group.apps.take(3).mapNotNull { app -> viewModel.getPkg(app.pkg) }
        }
    }
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item {
            val navController = LocalPageNavController.current
            CollAppsSummary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                text = "Installed apps summary ($installedAppsSummary)",
                onClick = {
                    val route = OrgRouteId.CollAppList(coll)
                    navController.navigateTo(route.asRoute())
                },
            )
        }
        itemsIndexed(coll.groups, key = { _, g -> g.id }) { i, group ->
            CollGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                name = "${group.name} (${group.apps.size})",
                apps = groupPkgs[i],
                onClick = { onClickGroup(i, group.id) },
            )
        }
    }
}

@Composable
private fun CollAppsSummary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
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

@Composable
private fun CollGroup(name: String, apps: List<PackageInfo>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in apps.indices) {
                val app = apps[i]
                val icPkg = remember { app.applicationInfo?.let { AppIconPackageInfo(app, it) } }
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )
                }
            }
        }
    }
}

@Composable
@PreviewCombinedColorLayout
private fun OrgCollPreview() {
    val anApp = OrgApp(pkg = "", label = "", labelLocale = "", createTime = 0L, modifyTime = 0L)
    val groups = listOf(
        OrgGroup(id = 0, name = "Group 1", createTime = 0L, modifyTime = 0L, apps = listOf(anApp)),
        OrgGroup(id = 1, name = "Group 2", createTime = 0L, modifyTime = 0L, apps = listOf(anApp)),
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

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

package com.madness.collision.unit.api_viewing.ui.org.group

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.chief.app.LocalPageNavController
import com.madness.collision.chief.layout.scaffoldWindowInsets
import com.madness.collision.ui.comp.ClassicTopAppBar
import com.madness.collision.unit.api_viewing.ui.org.OrgRouteId
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppHeading
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppItem
import com.madness.collision.unit.api_viewing.ui.org.coll.CompactCollAppItemStyle
import com.madness.collision.unit.api_viewing.ui.org.coll.DetailedCollAppItemStyle
import com.madness.collision.unit.api_viewing.ui.org.coll.LocalCollAppItemStyle
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.mainApplication
import com.madness.collision.util.ui.AppIconPackageInfo
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup

@Stable
interface GroupInfoEventHandler {
    fun getAppLabel(pkgName: String): String
    fun launchApp(pkgName: String)
    fun showAppInOwner(pkgName: String, owner: String)
}

@Composable
fun GroupInfoPage(
    group: OrgGroup? = null,
    modCollId: Int = -1,
    modGroupId: Int = -1,
) {
    val viewModel = viewModel<GroupInfoViewModel>()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.init(context, modCollId, modGroupId) }
    val eventHandler = rememberGroupInfoEventHandler(viewModel, context)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (groupName, selPkgs, installedApps, isLoading, isSubmitOk, ownerApps) = uiState
    val groupPkgs = remember(group) { group?.apps?.map(OrgApp::pkg)?.toSet() }

    val navController = LocalPageNavController.current
    LaunchedEffect(isSubmitOk) { if (isSubmitOk) navController.navigateBack() }

    GroupScaffold(
        title = group?.name ?: groupName,
        onActionEdit = {
            val route = OrgRouteId.GroupEditor(modCollId, modGroupId)
            navController.navigateTo(route.asRoute())
        },
        onActionDelete = { group?.let(viewModel::remove) },
    ) { innerPadding ->
        GroupContent(
            modifier = Modifier.fillMaxWidth(),
            groupName = group?.name ?: groupName,
            eventHandler = eventHandler,
            selectedPkgs = groupPkgs ?: selPkgs,
            installedApps = installedApps,
            appOwnerApps = ownerApps,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun rememberGroupInfoEventHandler(viewModel: GroupInfoViewModel, context: Context) =
    remember<GroupInfoEventHandler> {
        object : GroupInfoEventHandler {
            override fun getAppLabel(pkgName: String) =
                viewModel.getPkgLabel(pkgName)
            override fun launchApp(pkgName: String) {
                context.packageManager.getLaunchIntentForPackage(pkgName)
                    ?.let(context::startActivity)
            }
            override fun showAppInOwner(pkgName: String, owner: String) {
                viewModel.getAppOwner(owner)?.showAppInfo(pkgName, context)
            }
        }
    }

@Composable
private fun GroupScaffold(
    title: String,
    onActionEdit: () -> Unit,
    onActionDelete: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val appBarIconColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFFD0D6DB) else Color(0xFF353535)
        false -> if (mainApplication.isDarkTheme) Color(0xFFD0D6DB) else Color(0xFF353535)
    }
    val (topBarInsets, _, contentInsets) =
        scaffoldWindowInsets(shareCutout = 12.dp, shareStatusBar = 8.dp, shareWaterfall = 8.dp)
    Scaffold(
        topBar = {
            ClassicTopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = onActionEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = appBarIconColor,
                        )
                    }
                    IconButton(onClick = onActionDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = appBarIconColor,
                        )
                    }
                },
                windowInsets = topBarInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = contentInsets,
        content = content,
    )
}

@Composable
private fun GroupContent(
    groupName: String,
    eventHandler: GroupInfoEventHandler,
    modifier: Modifier = Modifier,
    selectedPkgs: Set<String> = emptySet(),
    installedApps: List<PackageInfo> = emptyList(),
    appOwnerApps: List<PackageInfo> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    var detailed by remember { mutableStateOf(false) }
    val selectedApps = remember(selectedPkgs, installedApps) {
        when (selectedPkgs.size) {
            0 -> emptyList()
            1 -> installedApps.find { it.packageName in selectedPkgs }?.let(::listOf).orEmpty()
            else -> installedApps.filter { it.packageName in selectedPkgs }
        }
    }
    val uninstalledPkgs = remember(selectedPkgs, selectedApps) {
        if (installedApps.isEmpty() || selectedApps.size >= selectedPkgs.size) return@remember emptyList()
        val un = selectedPkgs - selectedApps.mapTo(HashSet(selectedApps.size)) { it.packageName }
        un.toList()
    }
    val context = LocalContext.current
    val defAppIcon = remember { context.packageManager.defaultActivityIcon }

    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        if (selectedApps.isNotEmpty()) {
            item(key = "@group.sec.sel", contentType = "AppHeading") {
                CollAppHeading(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 0.dp),
                    name = "Group apps (${selectedApps.size})",
                    changeViewText = if (detailed) "Detailed view" else "Compact view",
                    onChangeView = { detailed = !detailed },
                )
            }
        }
        items(selectedApps, key = { app -> app.packageName + "$" }, contentType = { "App" }) { app ->
            val icPkg = app.applicationInfo?.let { AppIconPackageInfo(app, it) }
            val itemStyle = if (detailed) DetailedCollAppItemStyle else CompactCollAppItemStyle
            CompositionLocalProvider(LocalCollAppItemStyle provides itemStyle) {
            GroupItem(
                modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 8.dp),
                name = eventHandler.getAppLabel(app.packageName),
                pkgName = app.packageName,
                iconModel = icPkg,
                onLaunch = { eventHandler.launchApp(app.packageName) },
                onExternal = null,
            )
            }
        }

        if (uninstalledPkgs.isNotEmpty()) {
            item(key = "@group.sec.uninstall", contentType = "AppHeading") {
                CollAppHeading(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 0.dp),
                    name = "Uninstalled apps (${uninstalledPkgs.size})",
                )
            }
        }
        items(uninstalledPkgs, key = { app -> app }, contentType = { "App" }) { app ->
            var showAppOwners by remember { mutableStateOf(false) }
            val itemStyle = DetailedCollAppItemStyle
            CompositionLocalProvider(LocalCollAppItemStyle provides itemStyle) {
            GroupItem(
                modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 8.dp),
                name = eventHandler.getAppLabel(app),
                pkgName = app,
                iconModel = defAppIcon,
                onLaunch = null,
                onExternal = { showAppOwners = true },
                externalContent = {
                    DropdownMenu(
                        expanded = showAppOwners,
                        onDismissRequest = { showAppOwners = false },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        AppOwners(
                            appOwnerApps = appOwnerApps,
                            getOwnerLabel = eventHandler::getAppLabel,
                            onSelectOwner = { owner ->
                                eventHandler.showAppInOwner(app, owner)
                                showAppOwners = false
                            },
                        )
                    }
                }
            )
            }
        }
    }
}

@Composable
private fun AppOwners(
    appOwnerApps: List<PackageInfo>,
    getOwnerLabel: (String) -> String,
    onSelectOwner: (String) -> Unit,
) {
    for (owner in appOwnerApps) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 160.dp, max = 260.dp)
                .clickable { onSelectOwner(owner.packageName) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                modifier = Modifier.width(32.dp).heightIn(max = 32.dp),
                model = owner.applicationInfo?.let { AppIconPackageInfo(owner, it) },
                contentDescription = null,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = getOwnerLabel(owner.packageName),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            // extra space for optical balance
            Spacer(Modifier.width(15.dp))
        }
    }
}

@Composable
private fun GroupItem(
    name: String,
    pkgName: String,
    iconModel: Any?,
    onLaunch: (() -> Unit)?,
    onExternal: (() -> Unit)?,
    modifier: Modifier = Modifier,
    externalContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollAppItem(
            modifier = Modifier.weight(1f),
            name = name,
            iconModel = iconModel,
            secondaryText = pkgName,
        )
        if (onLaunch != null || onExternal != null) {
            Box {
                OutlinedButton(
                    modifier = Modifier.sizeIn(minWidth = 36.dp, minHeight = 20.dp),
                    onClick = onLaunch ?: onExternal ?: {},
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 5.dp),
                ) {
                    Text(text = if (onLaunch != null) "Open" else "Install...", fontSize = 12.sp)
                }
                externalContent()
            }
        }
    }
}

internal fun PseudoGroupInfoEventHandler() =
    object : GroupInfoEventHandler {
        override fun getAppLabel(pkgName: String) = ""
        override fun launchApp(pkgName: String) {}
        override fun showAppInOwner(pkgName: String, owner: String) {}
    }

@Composable
@PreviewCombinedColorLayout
private fun GroupInfoPreview() {
    BoundoTheme {
        Surface {
            GroupContent(
                modifier = Modifier.fillMaxWidth(),
                groupName = "Preview Group",
                eventHandler = remember { PseudoGroupInfoEventHandler() },
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
            )
        }
    }
}

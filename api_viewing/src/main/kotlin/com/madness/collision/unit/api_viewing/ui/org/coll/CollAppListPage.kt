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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.ui.org.group.GroupEditorViewModel
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.ui.AppIconPackageInfo
import io.cliuff.boundo.org.model.CompColl
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup

@Stable
interface GroupInfoEventHandler {
    fun getAppLabel(pkgName: String): String
    fun getAppGroups(pkgName: String): List<String>
}

@Composable
fun CollAppListPage(
    coll: CompColl? = null,
    modCollId: Int = -1,
    modGroupId: Int = -1,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val viewModel = viewModel<GroupEditorViewModel>()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.init(context, modCollId, modGroupId) }
    val eventHandler = rememberGroupInfoEventHandler(viewModel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (groupName, selPkgs, installedApps, installedAppsGrouping, isLoading) = uiState
    val collPkgs = remember(coll) { coll?.groups?.flatMap { it.apps.map(OrgApp::pkg) }?.toSet() }
    GroupContent(
        modifier = Modifier.fillMaxWidth(),
        groupName = coll?.name ?: groupName,
        eventHandler = eventHandler,
        selectedPkgs = collPkgs ?: selPkgs,
        installedApps = installedApps,
        installedAppsGrouping = installedAppsGrouping,
        contentPadding = contentPadding,
    )
}

@Composable
private fun rememberGroupInfoEventHandler(viewModel: GroupEditorViewModel) =
    remember<GroupInfoEventHandler> {
        object : GroupInfoEventHandler {
            override fun getAppLabel(pkgName: String) =
                viewModel.getPkgLabel(pkgName)
            override fun getAppGroups(pkgName: String) =
                viewModel.getPkgGroups(pkgName).map(OrgGroup::name)
        }
    }

@Composable
private fun GroupContent(
    groupName: String,
    eventHandler: GroupInfoEventHandler,
    modifier: Modifier = Modifier,
    selectedPkgs: Set<String> = emptySet(),
    installedApps: List<PackageInfo> = emptyList(),
    installedAppsGrouping: List<Int> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        for (sectionIndex in installedAppsGrouping.indices) {
            val sectionApps = installedApps.getGroup(installedAppsGrouping, sectionIndex)
            val selectedSecPkgSize = sectionApps.count { it.packageName in selectedPkgs }
            if (sectionApps.isNotEmpty()) {
                item(key = "@group.sec.apps$sectionIndex") {
                    val heading = collAppGroupHeading(sectionIndex)
                    CollAppHeading(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = 12.dp),
                        name = "$heading ($selectedSecPkgSize/${sectionApps.size})",
                    )
                }
            }
            items(sectionApps, key = { app -> app.packageName }) { app ->
                val icPkg = app.applicationInfo?.let { AppIconPackageInfo(app, it) }
                GroupItem(
                    modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 8.dp),
                    name = eventHandler.getAppLabel(app.packageName),
                    selected = app.packageName in selectedPkgs,
                    iconModel = icPkg,
                    includedGroups = eventHandler.getAppGroups(app.packageName),
                )
            }
        }
    }
}

@Composable
private fun GroupItem(
    name: String,
    selected: Boolean,
    iconModel: Any?,
    modifier: Modifier = Modifier,
    includedGroups: List<String> = emptyList(),
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollAppItem(
            modifier = Modifier.weight(1f),
            name = name,
            iconModel = iconModel,
            desc = { CollAppGroupRow(names = includedGroups) },
        )
        Checkbox(
            modifier = Modifier.minimumInteractiveComponentSize(),
            checked = selected,
            onCheckedChange = null,
            enabled = false,
        )
    }
}

internal fun PseudoGroupInfoEventHandler() =
    object : GroupInfoEventHandler {
        override fun getAppLabel(pkgName: String) = ""
        override fun getAppGroups(pkgName: String) = emptyList<String>()
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

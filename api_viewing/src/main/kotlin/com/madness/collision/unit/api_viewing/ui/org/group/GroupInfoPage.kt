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

import android.content.pm.PackageInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppHeading
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppItem
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.ui.AppIconPackageInfo
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup

@Stable
interface GroupInfoEventHandler {
    fun getAppLabel(pkgName: String): String
}

@Composable
fun GroupInfoPage(group: OrgGroup? = null, modCollId: Int = -1, modGroupId: Int = -1) {
    val viewModel = viewModel<GroupEditorViewModel>()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.init(context, modCollId, modGroupId) }
    val eventHandler = rememberGroupInfoEventHandler(viewModel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (groupName, selPkgs, installedApps, _, isLoading) = uiState
    val groupPkgs = remember(group) { group?.apps?.map(OrgApp::pkg)?.toSet() }
    GroupContent(
        modifier = Modifier.fillMaxWidth(),
        groupName = group?.name ?: groupName,
        eventHandler = eventHandler,
        selectedPkgs = groupPkgs ?: selPkgs,
        installedApps = installedApps,
    )
}

@Composable
private fun rememberGroupInfoEventHandler(viewModel: GroupEditorViewModel) =
    remember<GroupInfoEventHandler> {
        object : GroupInfoEventHandler {
            override fun getAppLabel(pkgName: String) =
                viewModel.getPkgLabel(pkgName)
        }
    }

@Composable
private fun GroupContent(
    groupName: String,
    eventHandler: GroupInfoEventHandler,
    modifier: Modifier = Modifier,
    selectedPkgs: Set<String> = emptySet(),
    installedApps: List<PackageInfo> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val selectedApps = remember(selectedPkgs, installedApps) {
        when (selectedPkgs.size) {
            0 -> emptyList()
            1 -> installedApps.find { it.packageName in selectedPkgs }?.let(::listOf).orEmpty()
            else -> installedApps.filter { it.packageName in selectedPkgs }
        }
    }
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "@group.header") {
            GroupHeader(
                groupName = groupName,
                onSubmit = {},
            )
        }

        if (selectedApps.isNotEmpty()) {
            item(key = "@group.sec.sel") {
                CollAppHeading(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 12.dp),
                    name = "Group apps (${selectedApps.size})",
                )
            }
        }
        items(selectedApps, key = { app -> app.packageName + "$" }) { app ->
            val icPkg = app.applicationInfo?.let { AppIconPackageInfo(app, it) }
            CollAppItem(
                modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 8.dp),
                name = eventHandler.getAppLabel(app.packageName),
                iconModel = icPkg,
            )
        }
    }
}

@Composable
private fun GroupHeader(groupName: String, onSubmit: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 20.dp, vertical = 5.dp),
            onClick = onSubmit,
        ) {
            Text(
                modifier = Modifier,
                text = "Add group",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 15.sp,
            )
        }
        GroupName(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            name = groupName,
        )
    }
}

@Composable
private fun GroupName(name: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = name,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 15.sp,
        lineHeight = 17.sp,
    )
}

internal fun PseudoGroupInfoEventHandler() =
    object : GroupInfoEventHandler {
        override fun getAppLabel(pkgName: String) = ""
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

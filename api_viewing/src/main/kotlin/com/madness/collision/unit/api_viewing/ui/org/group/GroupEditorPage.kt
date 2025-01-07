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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.chief.app.LocalPageNavController
import com.madness.collision.chief.layout.scaffoldWindowInsets
import com.madness.collision.ui.comp.ClassicTopAppBar
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppGroupRow
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppHeading
import com.madness.collision.unit.api_viewing.ui.org.coll.CollAppItem
import com.madness.collision.unit.api_viewing.ui.org.coll.collAppGroupHeading
import com.madness.collision.unit.api_viewing.ui.org.coll.getGroup
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.ui.AppIconPackageInfo
import io.cliuff.boundo.org.model.OrgGroup

@Stable
interface GroupEditorEventHandler {
    fun getAppLabel(pkgName: String): String
    fun getAppGroups(pkgName: String): List<String>
    fun setGroupName(name: String)
    fun setAppSelected(pkgName: String, selected: Boolean)
    fun submitEdits()
}

@Composable
fun GroupEditorPage(
    modCollId: Int = -1,
    modGroupId: Int = -1,
) {
    val viewModel = viewModel<GroupEditorViewModel>()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.init(context, modCollId, modGroupId) }
    val eventHandler = rememberGroupEditorEventHandler(viewModel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (groupName, selPkgs, installedApps, installedAppsGrouping, isLoading, isSubmitOk) = uiState

    val navController = LocalPageNavController.current
    LaunchedEffect(isSubmitOk) { if (isSubmitOk) navController.navigateBack() }

    GroupScaffold(
        title = if (modGroupId > 0) "Edit Group" else "Create New Group",
        submitText = if (modGroupId > 0) "Update group" else "Add group",
        eventHandler = eventHandler,
    ) { innerPadding ->
        GroupContent(
            modifier = Modifier.fillMaxWidth(),
            groupName = groupName,
            eventHandler = eventHandler,
            selectedPkgs = selPkgs,
            installedApps = installedApps,
            installedAppsGrouping = installedAppsGrouping,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun rememberGroupEditorEventHandler(viewModel: GroupEditorViewModel) =
    remember<GroupEditorEventHandler> {
        object : GroupEditorEventHandler {
            override fun getAppLabel(pkgName: String) =
                viewModel.getPkgLabel(pkgName)
            override fun getAppGroups(pkgName: String) =
                viewModel.getPkgGroups(pkgName).map(OrgGroup::name)
            override fun setGroupName(name: String) =
                viewModel.setGroupName(name)
            override fun setAppSelected(pkgName: String, selected: Boolean) =
                viewModel.setPkgSelected(pkgName, selected)
            override fun submitEdits() =
                viewModel.submitEdits()
        }
    }

@Composable
private fun GroupScaffold(
    title: String,
    submitText: String,
    eventHandler: GroupEditorEventHandler,
    content: @Composable (PaddingValues) -> Unit,
) {
    val (topBarInsets, bottomBarInsets, contentInsets) =
        scaffoldWindowInsets(shareCutout = 12.dp, shareStatusBar = 8.dp, shareWaterfall = 8.dp)
    Scaffold(
        topBar = {
            ClassicTopAppBar(
                title = { Text(title) },
                windowInsets = topBarInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        bottomBar = {
            val (isEnabled, setEnabled) = remember { mutableIntStateOf(1) }
            GroupFooter(
                submitText = submitText,
                onSubmit = { setEnabled(0); eventHandler.submitEdits() },
                enabled = isEnabled == 1,
                windowInsets = bottomBarInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .add(WindowInsets(top = 10.dp, bottom = 20.dp)),
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
    eventHandler: GroupEditorEventHandler,
    modifier: Modifier = Modifier,
    selectedPkgs: Set<String> = emptySet(),
    installedApps: List<PackageInfo> = emptyList(),
    installedAppsGrouping: List<Int> = emptyList(),
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
            GroupName(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                name = groupName,
                onNameChange = eventHandler::setGroupName,
            )
        }

        if (selectedApps.isNotEmpty()) {
            item(key = "@group.sec.sel") {
                CollAppHeading(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 12.dp),
                    name = "Selected apps (${selectedApps.size})",
                )
            }
        }
        items(selectedApps, key = { app -> app.packageName + "$" }) { app ->
            val icPkg = app.applicationInfo?.let { AppIconPackageInfo(app, it) }
            GroupItem(
                modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 8.dp),
                name = eventHandler.getAppLabel(app.packageName),
                selected = app.packageName in selectedPkgs,
                iconModel = icPkg,
                onCheckedChange = { chk -> eventHandler.setAppSelected(app.packageName, chk) },
                includedGroups = eventHandler.getAppGroups(app.packageName),
            )
        }

        for (sectionIndex in installedAppsGrouping.indices) {
            val sectionApps = installedApps.getGroup(installedAppsGrouping, sectionIndex)
            if (sectionApps.isNotEmpty()) {
                item(key = "@group.sec.apps$sectionIndex") {
                    val heading = collAppGroupHeading(sectionIndex)
                    CollAppHeading(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = 12.dp),
                        name = "$heading (${sectionApps.size})",
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
                    onCheckedChange = { chk -> eventHandler.setAppSelected(app.packageName, chk) },
                    includedGroups = eventHandler.getAppGroups(app.packageName),
                )
            }
        }
    }
}

@Composable
private fun GroupFooter(
    submitText: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    Surface(modifier = modifier) {
        Box(modifier = Modifier.windowInsetsPadding(windowInsets)) {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp),
                onClick = onSubmit,
                enabled = enabled,
            ) {
                Text(
                    text = submitText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun GroupName(name: String, onNameChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Group name",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(18.dp))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = onNameChange,
            placeholder = {
                Text(
                    text = "Enter a name for the new group",
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions.Default,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun GroupItem(
    name: String,
    selected: Boolean,
    iconModel: Any?,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    includedGroups: List<String> = emptyList(),
) {
    Row(
        modifier = Modifier.clickable { onCheckedChange(!selected) }.then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollAppItem(
            modifier = Modifier.weight(1f),
            name = name,
            iconModel = iconModel,
            desc = { CollAppGroupRow(names = includedGroups) },
        )
        Checkbox(
            checked = selected,
            onCheckedChange = onCheckedChange,
        )
    }
}

internal fun PseudoGroupEditorEventHandler() =
    object : GroupEditorEventHandler {
        override fun getAppLabel(pkgName: String) = ""
        override fun getAppGroups(pkgName: String) = emptyList<String>()
        override fun setGroupName(name: String) {}
        override fun setAppSelected(pkgName: String, selected: Boolean) {}
        override fun submitEdits() {}
    }

@Composable
@PreviewCombinedColorLayout
private fun GroupEditorPreview() {
    BoundoTheme {
        val eventHandler = remember { PseudoGroupEditorEventHandler() }
        Surface(color = MaterialTheme.colorScheme.background) {
            GroupScaffold(
                title = "Unnamed Group",
                submitText = "Add group",
                eventHandler = eventHandler,
            ) { innerPadding ->
                GroupContent(
                    modifier = Modifier.fillMaxWidth(),
                    groupName = "Preview Group",
                    eventHandler = eventHandler,
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

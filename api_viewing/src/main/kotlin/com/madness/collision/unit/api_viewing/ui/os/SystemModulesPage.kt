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

package com.madness.collision.unit.api_viewing.ui.os

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.lang.runIf
import com.madness.collision.unit.api_viewing.data.ModuleInfo
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.dev.DarkPreview
import com.madness.collision.util.dev.LayoutDirectionPreviews
import com.madness.collision.util.ui.autoMirrored
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.madness.collision.R as MainR

@Composable
fun SystemModulesPage(paddingValues: PaddingValues, showPkgModule: (ModuleInfo) -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel<SystemModulesViewModel>()
    val (modules, appPkgs) = viewModel.uiState.value
    LaunchedEffect(Unit) { viewModel.init(context) }
    if (modules.isNotEmpty()) {
        SystemModules(
            moduleList = modules,
            appPkgSet = appPkgs,
            onClickItem = showPkgModule,
            paddingValues = paddingValues,
        )
    } else {
        Box(
            modifier = Modifier.padding(paddingValues).padding(top = 15.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(MainR.string.text_no_content),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun SystemModules(
    moduleList: List<ModuleInfo>,
    appPkgSet: Map<String, ApkInfo>,
    onClickItem: (ModuleInfo) -> Unit,
    paddingValues: PaddingValues,
) {
    LazyColumn(contentPadding = paddingValues) {
        itemsIndexed(moduleList) { index, moduleInfo ->
            val apkInfo = appPkgSet[moduleInfo.pkgName]
            if (index > 0) {
                Divider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }
            val hasAppPkg = apkInfo?.isApex == false
            val textAlpha = if (apkInfo == null) 0.5f else 1f
            Row(
                modifier = (Modifier as Modifier)
                    .runIf({ hasAppPkg }, { clickable { onClickItem(moduleInfo) } })
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = moduleInfo.name.orEmpty(),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha),
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                        )
                        if (apkInfo != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            ApkType(apkInfo = apkInfo)
                        }
                    }
                    Text(
                        text = moduleInfo.pkgName.orEmpty(),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f * textAlpha),
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                    )
                }
                if (hasAppPkg) {
                    Icon(
                        modifier = Modifier.size(20.dp).autoMirrored(),
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ApkType(apkInfo: ApkInfo) {
    val context = LocalContext.current
    val contentColor = when (apkInfo.isApex) {
        true -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        false -> Color(ThemeUtil.getColor(context, MainR.attr.colorActionPass))
    }
    Text(
        modifier = Modifier
            .clip(AbsoluteSmoothCornerShape(3.dp, 80))
            .background(contentColor.copy(alpha = 0.075f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        text = if (apkInfo.isApex) "APEX" else "APK",
        color = contentColor,
        fontSize = 8.sp,
        lineHeight = 9.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun SystemModulesPreviewContent() {
    val list = remember {
        listOf(
            ModuleInfo("Module 0", "com.android.mod0", false),
            ModuleInfo("Module 1", "com.android.mod1", false),
            ModuleInfo("Module 2", "com.android.mod2", false),
            ModuleInfo("Module 3", "com.android.mod3", false),
            ModuleInfo("Module 4", "com.android.mod4", false),
        )
    }
    val apkInfoMap = remember {
        buildMap {
            listOf(0, 3).mapNotNull { i -> list[i].pkgName?.let { ApkInfo(it, false) } }
                .associateByTo(this) { it.packageName }
            listOf(2, 4).mapNotNull { i -> list[i].pkgName?.let { ApkInfo(it, true) } }
                .associateByTo(this) { it.packageName }
        }
    }
    SystemModules(list, apkInfoMap, { }, PaddingValues())
}

@LayoutDirectionPreviews
@Composable
private fun SystemModulesPreview() {
    MaterialTheme { SystemModulesPreviewContent() }
}

@DarkPreview
@Composable
private fun SystemModulesDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SystemModulesPreviewContent()
    }
}

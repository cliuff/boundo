/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.info

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.unit.api_viewing.data.ModuleInfo
import com.madness.collision.unit.api_viewing.info.AppType
import com.madness.collision.unit.api_viewing.info.ExpressedTag
import com.madness.collision.unit.api_viewing.list.LocalAppSwitcherHandler
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.ui.info.tag.PkgArchDetails
import com.madness.collision.util.ui.autoMirrored
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
internal fun TagDetailsList(
    app: ApiViewingApp,
    tags: List<ExpressedTag>,
    getClick: (AppTagInfo) -> (() -> Unit)?,
    splitApks: List<Pair<String, String?>>,
) {
    for (index in tags.indices) {
        val expressed = tags[index]
        val key = run {
            if (index == 0) return@run expressed.intrinsic.id
            expressed.intrinsic.id + tags[index - 1].intrinsic.id
        }
        key(key) {
            val info = expressed.info
            val activated = expressed.activated
            val isAi = expressed.intrinsic.id == AppTagInfo.ID_APP_ADAPTIVE_ICON
            val is64Bit = expressed.intrinsic.id == AppTagInfo.ID_PKG_64BIT
            val showDivider = remember show@{
                if (index == 0) return@show false
                if (activated || isAi || is64Bit) return@show true
                val lastTag = tags[index - 1]
                lastTag.activated || lastTag.intrinsic.id == AppTagInfo.ID_APP_ADAPTIVE_ICON
            }
            if (showDivider) Divider(
                modifier = Modifier.padding(start = 20.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                thickness = 0.5.dp,
            )
            if (activated || isAi || is64Bit) {
                var showTagDetails by remember(app.packageName) { mutableStateOf(false) }
                val hasTagDetails = when (expressed.intrinsic.id) {
                    AppTagInfo.ID_PKG_64BIT,
                    AppTagInfo.ID_PKG_AAB, AppTagInfo.ID_APP_SYSTEM_MODULE, AppTagInfo.ID_TYPE_OVERLAY -> true
                    else -> false
                }
                val chevron = when (expressed.intrinsic.id) {
                    AppTagInfo.ID_APP_INSTALLER_PLAY -> Icons.Outlined.Launch
                    AppTagInfo.ID_APP_ADAPTIVE_ICON -> Icons.Outlined.ChevronRight
                    AppTagInfo.ID_PKG_64BIT,
                    AppTagInfo.ID_PKG_AAB, AppTagInfo.ID_APP_SYSTEM_MODULE, AppTagInfo.ID_TYPE_OVERLAY -> {
                        if (showTagDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore
                    }
                    else -> null
                }
                val itemColor = MaterialTheme.colorScheme.onSurface
                    .copy(alpha = if (activated) 0.75f else 0.35f)
                val onClick = run {
                    if (!hasTagDetails) return@run expressed.intrinsic.let(getClick);
                    { showTagDetails = !showTagDetails }
                }
                val shrinkTop = index > 0 && !showDivider
                TagItem(
                    title = expressed.label,
                    desc = expressed.desc,
                    icon = info.icon?.bitmap,
                    chevron = chevron,
                    itemColor = itemColor,
                    isIconActivated = activated,
                    onClick = onClick,
                    modifier = Modifier
                        .let { if (index == 0) it.padding(top = 8.dp) else it }
                        .let { if (index == tags.lastIndex) it.padding(bottom = 8.dp) else it }
                        .padding(top = if (shrinkTop) 0.dp else 12.dp, bottom = 12.dp),
                )
                if (hasTagDetails) {
                    AnimatedVisibility(
                        visible = showTagDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        when (expressed.intrinsic.id) {
                            AppTagInfo.ID_PKG_AAB -> AppBundleDetails(splitApks)
                            AppTagInfo.ID_PKG_64BIT -> PkgArchDetails(app)
                            AppTagInfo.ID_APP_SYSTEM_MODULE -> {
                                val module = app.moduleInfo
                                if (module != null) {
                                    ModuleDetails(moduleInfo = module)
                                }
                            }
                            AppTagInfo.ID_TYPE_OVERLAY -> {
                                val overlay = app.appType as? AppType.Overlay
                                if (overlay != null) {
                                    OverlayDetails(overlay.target)
                                }
                            }
                        }
                    }
                }
            } else {
                val shrinkTop = index > 0 && !showDivider
                TagItemDeactivated(
                    title = expressed.label,
                    desc = expressed.desc,
                    icon = info.icon?.bitmap,
                    modifier = Modifier
                        .let { if (index == 0) it.padding(top = 8.dp) else it }
                        .let { if (index == tags.lastIndex) it.padding(bottom = 8.dp) else it }
                        .padding(top = if (shrinkTop) 0.dp else 6.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun TagItem(
    title: String,
    desc: String?,
    icon: Bitmap?,
    chevron: ImageVector?,
    itemColor: Color,
    isIconActivated: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagItemIcon(icon = icon, activated = isIconActivated)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = itemColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp,
                )
            }
            if (desc != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    color = itemColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 11.sp,
                )
            }
        }
        if (chevron != null) {
            Icon(
                modifier = Modifier.size(16.dp).autoMirrored(),
                imageVector = chevron,
                contentDescription = null,
                tint = itemColor,
            )
        }
    }
}

@Composable
private fun TagItemDeactivated(
    title: String, desc: String?, icon: Bitmap?, modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).then(modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagItemIcon(icon = icon, activated = false)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            if (desc != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = desc,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TagItemIcon(icon: Bitmap?, activated: Boolean) {
    if (icon != null) {
        val colorFilter = remember {
            if (activated) return@remember null
            val matrix = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(matrix)
        }
        Image(
            modifier = Modifier.size(16.dp),
            bitmap = icon.asImageBitmap(),
            contentDescription = null,
            colorFilter = colorFilter,
        )
    } else {
        Icon(
            modifier = Modifier.size(16.dp).autoMirrored(),
            imageVector = Icons.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (activated) 0.75f else 0.4f),
        )
    }
}

@Composable
private fun ModuleDetails(moduleInfo: ModuleInfo) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(start = 3.dp, bottom = 12.dp)
            .clip(AbsoluteSmoothCornerShape(cornerRadius = 8.dp, smoothnessAsPercent = 60))
            .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column() {
            Text(
                text = moduleInfo.name.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = moduleInfo.pkgName.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OverlayDetails(target: String) {
    val context = LocalContext.current
    val switcherHandler = LocalAppSwitcherHandler.current
    val appData by produceState(null as ApiViewingApp? to false) {
        val app = withContext(Dispatchers.IO) { switcherHandler.getApp(target) }
        // offer result after some delay for visibility animation to finish
        delay(50)
        value = app to true
    }
    val app = remember(appData) { appData.first }
    val isRetrieved = remember(appData) { appData.second }
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(start = 3.dp, bottom = 12.dp)
            .clip(AbsoluteSmoothCornerShape(cornerRadius = 8.dp, smoothnessAsPercent = 60))
            .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .clickable(enabled = app != null) clc@{ if (app != null) switcherHandler.loadApp(app) }
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app != null) {
            AsyncImage(
                modifier = Modifier.height(24.dp).widthIn(max = 24.dp),
                model = remember { AppPackageInfo(context, app) },
                contentDescription = null,
            )
        } else if (!isRetrieved) {
            // leave space for icon unconditionally when app is not retrieved yet,
            // this space will be removed afterwards if app is retrieved unsuccessfully (low probability)
            Spacer(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column() {
            if (app != null) {
                Text(
                    text = app.name,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (!isRetrieved) {
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = target,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
    }
}

@Composable
private fun AppBundleDetails(list: List<Pair<String, String?>>) {
    Column(modifier = Modifier
        .padding(horizontal = 20.dp)
        .padding(bottom = 12.dp)) {
        val totalSize = list[0].second
        Text(
            text = stringResource(R.string.av_list_info_aab_total_size, totalSize ?: "0"),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        list.subList(1, list.size).forEach {
            AppBundleApkItem(it.first, it.second)
        }
    }
}

@Composable
private fun AppBundleApkItem(label: String, label1: String?) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (label1 != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

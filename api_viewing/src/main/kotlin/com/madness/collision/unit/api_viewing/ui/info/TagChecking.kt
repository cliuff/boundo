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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.info.ExpressedTag
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo

@Composable
internal fun TagDetailsList(
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
            val showDivider = remember show@{
                if (index == 0) return@show false
                if (activated || isAi) return@show true
                val lastTag = tags[index - 1]
                lastTag.activated || lastTag.intrinsic.id == AppTagInfo.ID_APP_ADAPTIVE_ICON
            }
            if (showDivider) Divider(
                modifier = Modifier.padding(start = 20.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                thickness = 0.5.dp,
            )
            if (activated || isAi) {
                var showAabDetails by remember { mutableStateOf(false) }
                val isAab = expressed.intrinsic.id == AppTagInfo.ID_PKG_AAB
                val direction = LocalLayoutDirection.current
                val chevron by remember(showAabDetails) {
                    derivedStateOf {
                        when (expressed.intrinsic.id) {
                            AppTagInfo.ID_APP_INSTALLER_PLAY -> Icons.Outlined.Launch
                            AppTagInfo.ID_APP_ADAPTIVE_ICON -> {
                                if (direction == LayoutDirection.Rtl) Icons.Outlined.ChevronLeft
                                else Icons.Outlined.ChevronRight
                            }
                            AppTagInfo.ID_PKG_AAB -> {
                                if (showAabDetails) Icons.Outlined.ExpandLess
                                else Icons.Outlined.ExpandMore
                            }
                            else -> null
                        }
                    }
                }
                val itemColor = MaterialTheme.colorScheme.onSurface
                    .copy(alpha = if (activated) 0.75f else 0.35f)
                val onClick = run {
                    if (!isAab) return@run expressed.intrinsic.let(getClick);
                    { showAabDetails = !showAabDetails }
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
                if (isAab) {
                    AnimatedVisibility(
                        visible = showAabDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        AppBundleDetails(splitApks)
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
                modifier = Modifier.size(16.dp),
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
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (activated) 0.75f else 0.4f),
        )
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

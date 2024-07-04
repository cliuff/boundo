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

package com.madness.collision.unit.api_viewing.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.madness.collision.R as MainR

@Stable
interface ListOptionsEventHandler {
    fun toggleSrc(src: AppListSrc)
    fun toggleApks()
    fun toggleFolder()
    fun setOrder(order: AppListOrder)
    fun setApiMode(apiMode: AppApiMode)
}

@Composable
fun AppListOptions(
    modifier: Modifier = Modifier,
    options: AppListOptions,
    eventHandler: ListOptionsEventHandler
) {
    val horizontalPadding = 20.dp
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 13.dp),
            text = "Choose app sources",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            lineHeight = 17.sp,
        )
        Row(modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)) {
            val srcChoices = listOf(
                AppListSrc.SystemApps to stringResource(MainR.string.apiDisplaySys),
                AppListSrc.UserApps to stringResource(R.string.sdkcheck_displayspinner_user),
//                StorageAppSrc.DeviceApks to stringResource(MainR.string.apiDisplayAPK),
            )
            val selSrcSet = remember(options.srcSet) { options.srcSet.toSet() }
            for (i in srcChoices.indices) {
                val (listSrc, srcLabel) = srcChoices[i]
                if (i > 0) Spacer(modifier = Modifier.width(15.dp))
                AppSrcItem(
                    selected = listSrc in selSrcSet,
                    label = srcLabel,
                    category = "Installed app",
                    onClick = { eventHandler.toggleSrc(listSrc) },
                )
            }

            Spacer(modifier = Modifier.width(15.dp))
            AppSrcItem(
                selected = selSrcSet.any { it is AppListSrc.SelectApks },
                label = stringResource(MainR.string.apiDisplayFile),
                category = "APK files",
                onClick = eventHandler::toggleApks,
            )

            Spacer(modifier = Modifier.width(15.dp))
            AppSrcItem(
                selected = selSrcSet.any { it is AppListSrc.SelectVolume },
                label = stringResource(MainR.string.apiDisplayVolume),
                category = "APK files",
                onClick = eventHandler::toggleFolder,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 13.dp),
            text = "List ordering",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            lineHeight = 17.sp,
        )
        Row(modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)) {
            for ((i, order) in AppListOrder.entries.withIndex()) {
                val orderLabel = when (order) {
                    AppListOrder.LowerApi -> stringResource(R.string.sdkcheck_sortspinner_sortbylowersdk) // "Lower API"
                    AppListOrder.HigherApi -> stringResource(R.string.sdkcheck_sortspinner_sortbyhighersdk) // "Higher API"
                    AppListOrder.AppName -> stringResource(R.string.sdkcheck_sortspinner_sortbyname) // "App name"
                    AppListOrder.UpdateTime -> stringResource(MainR.string.menuApiSortApiTime) // "Update time"
                }
                if (i > 0) Spacer(modifier = Modifier.width(12.dp))
                OrderItem(
                    selected = options.listOrder == order,
                    label = orderLabel,
                    onClick = { eventHandler.setOrder(order) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 13.dp),
            text = "List API mode",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            lineHeight = 17.sp,
        )
        Row(modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)) {
            for ((i, mode) in AppApiMode.entries.withIndex()) {
                val modeLabel = when (mode) {
                    AppApiMode.Compile -> "Compile API"
                    AppApiMode.Target -> "Target API"
                    AppApiMode.Minimum -> "Min. API"
                }
                if (i > 0) Spacer(modifier = Modifier.width(12.dp))
                OrderItem(
                    selected = options.apiMode == mode,
                    label = modeLabel,
                    onClick = { eventHandler.setApiMode(mode) }
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
    }
}

@Composable
private fun AppSrcItem(selected: Boolean, label: String, category: String, onClick: () -> Unit) {
    val (containerColor, textColor) = MaterialTheme.colorScheme.run {
        if (selected) (tertiary to onTertiary) else (tertiaryContainer to onTertiaryContainer)
    }
    Box(
        modifier = Modifier
            .heightIn(min = 80.dp)
            .widthIn(min = 50.dp)
            .clip(AbsoluteSmoothCornerShape(14.dp, 60))
            .clickable(onClick = onClick)
            .background(color = containerColor)
            .padding(4.dp),
    ) {
        Column(modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 10.dp, vertical = 20.dp)) {
            Text(
                text = category,
                color = textColor.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                modifier = Modifier.align(Alignment.TopEnd).padding(1.dp).size(18.dp),
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun OrderItem(selected: Boolean, label: String, onClick: () -> Unit) {
    val (containerColor, textColor) = MaterialTheme.colorScheme.run {
        if (selected) (primary to onPrimary) else (primaryContainer to onPrimaryContainer)
    }
    Column(
        modifier = Modifier
            .widthIn(min = 50.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(color = containerColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun PseudoListOptionsEventHandler() =
    object : ListOptionsEventHandler {
        override fun toggleSrc(src: AppListSrc) {}
        override fun toggleApks() {}
        override fun toggleFolder() {}
        override fun setOrder(order: AppListOrder) {}
        override fun setApiMode(apiMode: AppApiMode) {}
    }

@PreviewCombinedColorLayout
@Composable
private fun AppListOptionsPreview() {
    val options = remember {
        val src = listOf(AppListSrc.SystemApps, AppListSrc.UserApps)
        AppListOptions(src, AppListOrder.HigherApi, AppApiMode.Target)
    }
    BoundoTheme {
        AppListOptions(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            options = options,
            eventHandler = remember { PseudoListOptionsEventHandler() }
        )
    }
}

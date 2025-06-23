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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.util.dev.PreviewCombinedColorLayout

@Stable
interface CompositeOptionsEventHandler : ListOptionsEventHandler {
    fun shareList()
    fun showSettings()
    fun updateTags(id: String, state: Boolean?)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListOptionsDialog(
    isShown: Int,
    options: AppListOptions,
    eventHandler: CompositeOptionsEventHandler,
    windowInsets: WindowInsets = WindowInsets(0),
) {
    var showBottomSheet by remember(isShown) { mutableStateOf(isShown > 0) }
    if (showBottomSheet) {
        BottomSheet(
            onDismiss = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            content = { SheetContent(options, eventHandler, windowInsets) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(onDismiss: () -> Unit, sheetState: SheetState, content: @Composable () -> Unit) {
    // todo set contentWindowInsets to zero to disable nav bar scrim
    // todo horizontal insets as sheet's margin, bottom insets as content padding
    // avoid horizontal insets for waterfall in portrait, and system bars and cutout in split screen
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        content = { content() },
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
                content = { BottomSheetDefaults.DragHandle() }
            )
        }
    )
}

@Composable
private fun SheetContent(
    options: AppListOptions,
    eventHandler: CompositeOptionsEventHandler,
    windowInsets: WindowInsets = WindowInsets(0),
) {
    val tagState = remember {
        val filter = options.srcSet.filterIsInstance<AppListSrc.TagFilter>().firstOrNull()
        ListTagState(filter?.checkedTags.orEmpty())
    }
    ListOptionsPager(
        actions = {
            PagerAction(icon = Icons.Outlined.Share, onClick = eventHandler::shareList)
            Spacer(modifier = Modifier.width(8.dp))
            PagerAction(icon = Icons.Outlined.Settings, onClick = eventHandler::showSettings)
        }) { tabIndex ->
        when (tabIndex) {
            0 -> {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(windowInsets.asPaddingValues()),
                ) {
                    AppListOptions(options = options, eventHandler = eventHandler)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        text = stringResource(R.string.avManual),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            1 -> {
                AppListTags(
                    tagState = tagState,
                    onStateChanged = eventHandler::updateTags,
                    contentPadding = windowInsets.asPaddingValues(),
                )
            }
        }
    }
}

@Composable
private fun ListOptionsPager(
    actions: @Composable RowScope.() -> Unit,
    page: @Composable (Int) -> Unit
) {
    Column() {
        var tabIndex by remember { mutableIntStateOf(0) }
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                val labels = remember {
                    arrayOf(R.string.av_options_tab_options, R.string.av_options_tab_tags)
                }
                for (i in labels.indices) {
                    key(i) {
                        PagerTab(
                            isSelected = i == tabIndex,
                            label = stringResource(labels[i]),
                            onClick = { tabIndex = i }
                        )
                    }
                }
            }
            actions()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            page(tabIndex)
        }
    }
}

@Composable
private fun PagerTab(isSelected: Boolean, label: String, onClick: () -> Unit) {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        text = label,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 0.95f else 0.7f),
        fontSize = 13.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun PagerAction(icon: ImageVector, onClick: () -> Unit) {
    Icon(
        modifier = Modifier
            .border(Dp.Hairline, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), CircleShape)
            .clickable(
                onClick = onClick,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 15.dp),
            )
            .padding(7.dp)
            .size(16.dp),
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
    )
}

internal fun PseudoCompOptionsEventHandler(): CompositeOptionsEventHandler =
    object : CompositeOptionsEventHandler,
        ListOptionsEventHandler by PseudoListOptionsEventHandler() {
        override fun shareList() {}
        override fun showSettings() {}
        override fun updateTags(id: String, state: Boolean?) {}
    }

@OptIn(ExperimentalMaterial3Api::class)
@PreviewCombinedColorLayout
@Composable
private fun ListOptionsPreview() {
    val options = remember {
        val src = listOf(AppListSrc.SystemApps, AppListSrc.UserApps)
        AppListOptions(src, AppListOrder.HigherApi, AppApiMode.Target)
    }
    BoundoTheme {
        BottomSheet(
            onDismiss = {},
            content = {
                SheetContent(
                    options = options,
                    eventHandler = remember { PseudoCompOptionsEventHandler() }
                )
            },
            sheetState = rememberStandardBottomSheetState(SheetValue.Expanded)
        )
    }
}

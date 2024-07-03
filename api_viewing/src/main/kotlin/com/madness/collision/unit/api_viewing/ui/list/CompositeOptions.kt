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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
fun ListOptionsDialog(isShown: Int, options: AppListOptions, eventHandler: CompositeOptionsEventHandler) {
    var showBottomSheet by remember(isShown) { mutableStateOf(isShown > 0) }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            content = { SheetContent(options, eventHandler) }
        )
    }
}

@Composable
private fun SheetContent(options: AppListOptions, eventHandler: CompositeOptionsEventHandler) {
    val tagState = remember {
        val filter = options.srcSet.filterIsInstance<AppListSrc.TagFilter>().firstOrNull()
        ListTagState(filter?.checkedTags.orEmpty())
    }
    ListOptionsPager(
        actions = {
            PagerAction(icon = Icons.Outlined.Share, onClick = eventHandler::shareList)
            Spacer(modifier = Modifier.width(5.dp))
            PagerAction(icon = Icons.Outlined.Settings, onClick = eventHandler::showSettings)
        }) { tabIndex ->
        when (tabIndex) {
            0 -> {
                Column() {
                    AppListOptions(options = options, eventHandler = eventHandler)
                    Text(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        text = stringResource(R.string.avManual),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                    )
                }
            }
            1 -> {
                AppListTags(tagState = tagState, onStateChanged = eventHandler::updateTags)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            val labels = arrayOf("List options", "Tag filter")
            for (i in labels.indices) {
                key(i) {
                    Text(
                        modifier = Modifier
                            .clickable { tabIndex = i }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        text = labels[i],
                        fontSize = 15.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            actions()
            Spacer(modifier = Modifier.width(8.dp))
        }
        page(tabIndex)
    }
}

@Composable
private fun PagerAction(icon: ImageVector, onClick: () -> Unit) {
    Icon(
        modifier = Modifier
            .border(Dp.Hairline, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), CircleShape)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 15.dp),
            )
            .padding(7.dp)
            .size(16.dp),
        imageVector = icon,
        contentDescription = null,
    )
}

internal fun PseudoCompOptionsEventHandler(): CompositeOptionsEventHandler =
    object : CompositeOptionsEventHandler,
        ListOptionsEventHandler by PseudoListOptionsEventHandler() {
        override fun shareList() {}
        override fun showSettings() {}
        override fun updateTags(id: String, state: Boolean?) {}
    }

@PreviewCombinedColorLayout
@Composable
private fun ListOptionsPreview() {
    val options = remember {
        val src = listOf(AppListSrc.SystemApps, AppListSrc.UserApps)
        AppListOptions(src, AppListOrder.HigherApi, AppApiMode.Target)
    }
    BoundoTheme {
        SheetContent(
            options = options,
            eventHandler = remember { PseudoCompOptionsEventHandler() }
        )
    }
}

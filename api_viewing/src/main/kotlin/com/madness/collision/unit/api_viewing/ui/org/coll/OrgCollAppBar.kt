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

import android.text.format.DateUtils
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.chief.app.stateOf
import com.madness.collision.ui.comp.ClassicTopAppBarDefaults
import com.madness.collision.unit.api_viewing.R
import io.cliuff.boundo.org.model.CompColl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgCollAppBar(
    collList: List<CompColl>,
    selectedColl: CompColl?,
    onClickColl: (CompColl) -> Unit = {},
    onActionDelete: () -> Unit = {},
    onActionImport: () -> Unit = {},
    onActionExport: () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val barContentAlpha by remember(scrollBehavior) {
            val state = scrollBehavior?.state ?: return@remember stateOf(1f)
            derivedStateOf { (1 - 2 * state.collapsedFraction).coerceIn(0f, 1f) }
        }
        CenterAlignedTopAppBar(
            title = {
                Text(
                    modifier = Modifier.alpha(barContentAlpha),
                    text = stringResource(R.string.org_title),
                    fontWeight = FontWeight.Medium,
                    fontFamily = ClassicTopAppBarDefaults.FontFamily,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            actions = {
                val context = LocalContext.current
                OverflowIconButton(
                    modifier = Modifier.alpha(barContentAlpha),
                    collName = selectedColl?.name,
                    collCreateTime = remember(selectedColl) {
                        selectedColl ?: return@remember ""
                        val time = DateUtils.getRelativeTimeSpanString(
                            selectedColl.createTime,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS)
                        context.getString(R.string.org_coll_create_time, time)
                    },
                    onActionDelete = onActionDelete,
                    onActionImport = onActionImport,
                    onActionExport = onActionExport,
                )
            },
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
            scrollBehavior = scrollBehavior,
        )
        val selIndex = selectedColl?.let(collList::indexOf) ?: -1
        // show non-empty coll list, even if selIndex < 0
        if (collList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                for (i in collList.indices) {
                    val coll = collList[i]
                    CollectionTab(
                        selected = i == selIndex,
                        name = coll.name,
                        onClick = { onClickColl(coll) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverflowIconButton(
    collName: String?,
    collCreateTime: String,
    onActionDelete: () -> Unit,
    onActionImport: () -> Unit,
    onActionExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (collName != null) {
        var showDelDialog by remember { mutableStateOf(false) }
        if (showDelDialog) {
            DeleteConfirmationDialog(
                title = stringResource(R.string.org_coll_del_title),
                onConfirm = { showDelDialog = false; onActionDelete() },
                onDismiss = { showDelDialog = false },
            )
        }

        var showOptions by remember { mutableStateOf(false) }
        IconButton(modifier = modifier, onClick = { showOptions = true }) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
            )
        }

        DropdownMenu(
            modifier = Modifier.widthIn(max = 260.dp),
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            shape = RoundedCornerShape(10.dp),
        ) {
            CollectionInfo(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 160.dp)
                    .padding(top = 10.dp, bottom = 4.dp),
                name = collName,
                createTime = collCreateTime,
            )

            DropdownMenuDeleteItem(
                modifier = Modifier.widthIn(min = 160.dp),
                text = stringResource(R.string.org_coll_option_del),
                onClick = {
                    showDelDialog = true
                    showOptions = false
                },
            )

            val context = LocalContext.current
            val impFile = remember { context.externalCacheDir?.resolve("Import.org.txt") }
            if (impFile?.exists() == true) {
                DropdownMenuItem(
                    modifier = Modifier.widthIn(min = 160.dp),
                    text = { Text(text = "Import collection") },
                    onClick = onActionImport,
                )
                DropdownMenuItem(
                    modifier = Modifier.widthIn(min = 160.dp),
                    text = { Text(text = "Export collection") },
                    onClick = onActionExport,
                )
            }
        }
    }
}

@Composable
private fun CollectionInfo(name: String, createTime: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
        )
        Text(
            text = createTime,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontSize = 11.sp,
            lineHeight = 12.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun CollectionTab(
    selected: Boolean,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.background),
        label = {
            Text(
                modifier = Modifier
                    .widthIn(min = 25.dp, max = 200.dp)
                    .padding(vertical = 10.dp),
                text = name,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    )
}

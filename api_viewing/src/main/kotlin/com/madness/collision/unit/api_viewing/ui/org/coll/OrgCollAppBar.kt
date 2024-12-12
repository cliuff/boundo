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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.cliuff.boundo.org.model.CompColl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgCollAppBar(
    collList: List<CompColl>,
    selectedColl: CompColl?,
    onClickColl: (CompColl) -> Unit = {},
    onActionDelete: () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {},
            actions = {
                IconButton(onClick = onActionDelete) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                    )
                }
            },
            windowInsets = windowInsets,
        )
        val selIndex = selectedColl?.let(collList::indexOf) ?: -1
        if (collList.isNotEmpty() && selIndex >= 0) {
            // PrimaryScrollableTabRow does not fill max width, use our own surface instead
            Surface() {
                Column() {
                    PrimaryScrollableTabRow(selectedTabIndex = selIndex, edgePadding = 20.dp, divider = {}) {
                        for (i in collList.indices) {
                            val coll = collList[i]
                            CollectionTab(
                                selected = coll == selectedColl,
                                name = coll.name,
                                onClick = { onClickColl(coll) },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CollectionTab(
    selected: Boolean,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tab(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                modifier = Modifier.widthIn(max = 200.dp),
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    )
}

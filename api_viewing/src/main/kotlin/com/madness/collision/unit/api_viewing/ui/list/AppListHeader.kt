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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListHeader(
    modifier: Modifier = Modifier,
    devInfoLabel: String,
    devInfoDesc: String,
    statsSizeLabel: String,
    onClickDevInfo: () -> Unit,
    onClickStats: () -> Unit,
) {
    Column(modifier = modifier) {
        var query by remember { mutableStateOf("") }
        var isActive by remember { mutableStateOf(false) }
        SearchBar(
            modifier = Modifier.fillMaxWidth(),
            query = query,
            onQueryChange = { query = it },
            onSearch = {},
            active = isActive,
            onActiveChange = { isActive = it },
            placeholder = {
                Text(
                    text = stringResource(com.madness.collision.R.string.sdk_search_hint),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            },
            content = {},
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            DeviceApiInfo(
                modifier = Modifier.weight(3f).fillMaxHeight(),
                label = devInfoLabel,
                desc = devInfoDesc,
                onClick = onClickDevInfo,
            )
            Spacer(modifier = Modifier.width(10.dp))
            ListStats(
                modifier = Modifier.weight(2f).fillMaxHeight(),
                sizeLabel = statsSizeLabel,
                onClick = onClickStats
            )
        }
    }
}

@Composable
private fun DeviceApiInfo(modifier: Modifier = Modifier, label: String, desc: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(AbsoluteSmoothCornerShape(15.dp, 60))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .size(width = 30.dp, height = 50.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(5.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.padding(vertical = 1.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 14.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ListStats(modifier: Modifier = Modifier, sizeLabel: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(AbsoluteSmoothCornerShape(15.dp, 60))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp).height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PieChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Stats",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontSize = 13.sp,
            lineHeight = 14.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = sizeLabel,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 11.sp,
            lineHeight = 12.sp,
        )
    }
}

@PreviewCombinedColorLayout
@Composable
private fun HeaderPreview() {
    BoundoTheme {
        AppListHeader(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            devInfoLabel = "Android 15",
            devInfoDesc = "API 35, Vanilla Ice Cream",
            statsSizeLabel = "231",
            onClickDevInfo = { },
            onClickStats = { },
        )
    }
}
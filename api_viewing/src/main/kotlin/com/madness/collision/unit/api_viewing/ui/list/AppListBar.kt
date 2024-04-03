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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.util.dev.PreviewCombinedColorLayout

@Composable
fun AppListBar(paddingValues: PaddingValues, toolsContent: @Composable RowScope.() -> Unit) {
    val windowInsets = WindowInsets.systemBars
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface)
//            .windowInsetsPadding(windowInsets)
            .padding(paddingValues)
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            var appSrcType by remember { mutableStateOf(ListSrcCat.Platform) }
            AppSrcTypeSwitcher(selType = appSrcType, onSelType = { appSrcType = it })
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, content = toolsContent)
    }
}

@Composable
private fun AppSrcTypeSwitcher(selType: ListSrcCat, onSelType: (ListSrcCat) -> Unit) {
    val types = remember {
        listOf(
            ListSrcCat.Platform to "Platform",
//            ListSrcCat.Storage to "",
            ListSrcCat.Temporary to "Temp",
        )
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(width = 1.dp, Color.DarkGray, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((srcType, typeLabel) in types) {
            if (srcType == selType) {
                Text(
                    modifier = Modifier
                        .clickable { onSelType(srcType) }
                        .padding(horizontal = 2.dp),
                    text = typeLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    modifier = Modifier
                        .clickable { onSelType(srcType) }
                        .padding(horizontal = 2.dp),
                    text = typeLabel,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun AppBarTool(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .border(width = Dp.Hairline, Color.LightGray, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(1.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewCombinedColorLayout
@Composable
private fun AppListBarPreview() {
    BoundoTheme {
        AppListBar(paddingValues = PaddingValues(0.dp)) {
            AppBarTool(icon = Icons.Outlined.CheckCircle, label = "Options", onClick = { })
            AppBarTool(icon = Icons.Outlined.FilterAlt, label = "Filter", onClick = { })
            AppBarTool(icon = Icons.Outlined.PieChart, label = "Stats", onClick = { })
            AppBarTool(icon = Icons.Outlined.MoreVert, label = "", onClick = { })
        }
    }
}

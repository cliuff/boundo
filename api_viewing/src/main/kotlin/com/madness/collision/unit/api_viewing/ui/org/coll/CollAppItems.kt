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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.madness.collision.R as MainR

internal fun <T> List<T>.getGroup(grouping: List<Int>, groupIndex: Int): List<T> {
    // end index should be greater than 0
    if (grouping[groupIndex] <= 0) return emptyList()
    // iterate backwards to find the first end index > 0
    val startIndex = (groupIndex - 1 downTo 0).find { i -> grouping[i] > 0 }
    return subList(startIndex?.let(grouping::get) ?: 0, grouping[groupIndex])
}

@Composable
@ReadOnlyComposable
fun collAppGroupHeading(groupIndex: Int): String =
    when (groupIndex) {
        0 -> "Installed launcher apps"
        1 -> "Installed service apps"
        2 -> "Installed misc apps"
        3 -> "Installed overlay apps"
        else -> "Installed apps"
    }

@Composable
fun CollAppHeading(name: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = name,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 13.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun CollAppItem(
    name: String,
    iconModel: Any?,
    modifier: Modifier = Modifier,
    typeText: String? = null,
    typeIcon: ImageVector? = null,
    secondaryText: String? = null,
    desc: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconModel != null) {
            AsyncImage(
                modifier = Modifier.width(36.dp).heightIn(max = 36.dp),
                model = iconModel,
                contentDescription = null,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                if (typeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AppType(text = typeText, icon = typeIcon)
                }
            }
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                )
            }
            desc()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollAppGroupRow(names: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (name in names) {
            AppGroup(name = name)
        }
    }
}

@Composable
private fun AppGroup(name: String) {
    Box(
        Modifier
            .clip(AbsoluteSmoothCornerShape(3.dp, 80))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.075f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppType(text: String, icon: ImageVector? = null) {
    Row(
        Modifier
            .border(Dp.Hairline, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape = CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(12.dp),
                imageVector = icon,
                contentDescription = null,
                tint = when (text) {
                    "/system", "/system_ext", "/vendor", "/odm", "/apex" ->
                        colorResource(MainR.color.androidRobotGreen).copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                },
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

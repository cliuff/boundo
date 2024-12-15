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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

internal fun <T> List<T>.getGroup(grouping: List<Int>, groupIndex: Int): List<T> {
    // end index should be greater than 0
    if (grouping[groupIndex] <= 0) return emptyList()
    // iterate backwards to find the first end index > 0
    val startIndex = (groupIndex - 1 downTo 0).indexOfFirst { i -> grouping[i] > 0 }
    return subList(grouping.getOrNull(startIndex) ?: 0, grouping[groupIndex])
}

@Composable
@ReadOnlyComposable
fun collAppGroupHeading(groupIndex: Int): String =
    when (groupIndex) {
        0 -> "Installed launcher apps"
        1 -> "Installed service apps"
        2 -> "Installed overlay apps"
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
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

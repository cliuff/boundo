/*
 * Copyright 2025 Clifford Liu
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SharedLibItem(
    label: String,
    type: String?,
    desc: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = if (desc != null) 1.dp else 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                modifier = Modifier.alignByBaseline().weight(1f, fill = false),
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
            if (type != null) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = type,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 8.sp,
                    lineHeight = 8.sp,
                )
            }
        }
        if (desc != null) {
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 8.sp,
                lineHeight = 9.sp,
            )
        }
    }
}

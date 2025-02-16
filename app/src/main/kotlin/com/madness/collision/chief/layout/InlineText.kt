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

package com.madness.collision.chief.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull

@Composable
fun SubcomposeTargetSize(
    modifier: Modifier = Modifier,
    target: @Composable () -> Unit,
    content: @Composable (size: IntSize) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val placeables = subcompose("target", target)
            .fastMap { it.measure(Constraints()) }
        val tw = placeables.fastMaxOfOrNull { it.measuredWidth } ?: 0
        val th = placeables.fastMaxOfOrNull { it.measuredHeight } ?: 0
        val size = IntSize(tw,th)

        val contentPlaceables = subcompose("content") { content(size) }
            .fastMap { it.measure(constraints) }
        val layoutWidth = contentPlaceables.fastMaxOfOrNull { it.width } ?: 0
        val layoutHeight = contentPlaceables.fastMaxOfOrNull { it.height } ?: 0
        layout(layoutWidth, layoutHeight) {
            contentPlaceables.fastForEach { it.placeRelative(0, 0) }
        }
    }
}

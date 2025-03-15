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

package com.madness.collision.unit.api_viewing.ui.org.coll

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull

@Composable
fun SubcomposeLayers(
    modifier: Modifier = Modifier,
    lowerLayer: @Composable (size: IntSize) -> Unit,
    upperLayer: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val placeables = subcompose("upper", upperLayer)
            .fastMap { it.measure(constraints) }
        val layerWidth = placeables.fastMaxOfOrNull { it.width } ?: 0
        val layerHeight = placeables.fastMaxOfOrNull { it.height } ?: 0
        val size = IntSize(layerWidth, layerHeight)

        val lowerPlaceables = subcompose("lower") { lowerLayer(size) }
            .fastMap { it.measure(constraints) }
        layout(layerWidth, layerHeight) {
            lowerPlaceables.fastForEach { it.placeRelative(0, 0) }
            placeables.fastForEach { it.placeRelative(0, 0) }
        }
    }
}

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

package com.madness.collision.unit.api_viewing.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull

@Composable
fun AppHomeScaffold(
    modifier: Modifier = Modifier,
    sideBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = WindowInsets.systemBars,
    content: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val sideBarPlaceables = subcompose(HomeLayoutSlot.SideBar, sideBar)
            .fastMap { it.measure(looseConstraints) }
        val sideBarWidth = sideBarPlaceables.fastMaxOfOrNull { it.width } ?: 0

        val bottomBarPlaceables = subcompose(HomeLayoutSlot.BottomBar, bottomBar)
            .fastMap { it.measure(looseConstraints) }
        val bottomBarHeight = bottomBarPlaceables.fastMaxOfOrNull { it.height } ?: 0

        val contentMeasurables = subcompose(HomeLayoutSlot.MainContent) {
            val insets = contentWindowInsets.asPaddingValues(this)
            val innerPadding = PaddingValues(
                top = insets.calculateTopPadding(),
                bottom = when {
                    bottomBarPlaceables.isNotEmpty() -> bottomBarHeight.toDp()
                    else -> insets.calculateBottomPadding()
                },
                start = when {
                    sideBarPlaceables.isNotEmpty() -> 0.dp
                    else -> insets.calculateStartPadding(layoutDirection)
                },
                end = insets.calculateEndPadding(layoutDirection),
            )
            content(innerPadding)
        }
        val contentPlaceables = contentMeasurables
            .fastMap { it.measure(looseConstraints.copy(maxWidth = layoutWidth - sideBarWidth)) }

        layout(layoutWidth, layoutHeight) {
            sideBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            contentPlaceables.fastForEach { it.placeRelative(sideBarWidth, 0) }
            bottomBarPlaceables.fastForEach {
                it.placeRelative(sideBarWidth, layoutHeight - bottomBarHeight)
            }
        }
    }
}

private enum class HomeLayoutSlot { SideBar, BottomBar, MainContent }

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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.waterfall
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.madness.collision.chief.lang.runIf

@Composable
fun scaffoldWindowInsets(
    shareCutout: Dp = 0.dp,
    shareStatusBar: Dp = 0.dp,
    shareWaterfall: Dp = 0.dp,
    shareSideCutout: Dp = Dp.Unspecified,
): List<WindowInsets> {
    // share waterfall insets with content padding
    val pageInsets = WindowInsets.systemBars
        .union(WindowInsets.waterfall.runIf({ shareWaterfall.value > 0f }) {
            share(WindowInsets(left = shareWaterfall, right = shareWaterfall))
        })
    // union cutout insets separately
    val (remainingCutoutInsets, cornerCutout) = rememberHorizontalCornerCutout()
    // union upper cutout insets for top app bar
    val topBarCornerCutoutInsets = WindowInsets(
        left = cornerCutout.getUpperInsetSpaceOrZero(-1),
        right = cornerCutout.getUpperInsetSpaceOrZero(1))
    // union upper cutout insets for bottom app bar
    val bottomBarCornerCutoutInsets = WindowInsets(
        left = cornerCutout.getLowerInsetSpaceOrZero(-1),
        right = cornerCutout.getLowerInsetSpaceOrZero(1))
    // add lower cutout height to content padding
    val contentCutoutHeight = intArrayOf(-1, 1).maxOf(cornerCutout::getLowerHeightOrZero)

    val topBarInsets = pageInsets
        .union(remainingCutoutInsets)
        .union(topBarCornerCutoutInsets)
        .runIf({ shareStatusBar.value > 0f }) {
            share(WindowInsets(top = shareStatusBar))
        }
    val bottomBarInsets = pageInsets
        .union(remainingCutoutInsets)
        .union(bottomBarCornerCutoutInsets)
    val contentInsets = pageInsets
        // apply IME insets to content only,
        // so bottom bar does not appear on top of the IME
        .union(WindowInsets.ime)
        .union(remainingCutoutInsets.runIf({ shareCutout.value > 0f }) {
            share(WindowInsets(left = shareCutout, right = shareCutout))
        })
        // this height is only added to the content when bottom bar is not present
        .add(WindowInsets(bottom = contentCutoutHeight))
        // add cutout insets to content when side bar is present (top bar is not)
        .runIf({ shareSideCutout.isSpecified }) { union(topBarCornerCutoutInsets) }
    val sideBarInsets =
        if (shareSideCutout.isSpecified) {
            // union upper cutout height for side bar at the start side
            val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
            val sideUpperCutoutHeight = cornerCutout.getUpperHeightOrZero(if (isLtr) -1 else 1)
            pageInsets
                .union(remainingCutoutInsets.runIf({ shareSideCutout.value > 0f }) {
                    share(WindowInsets(left = shareSideCutout, right = shareSideCutout))
                })
                .union(WindowInsets(top = sideUpperCutoutHeight, bottom = contentCutoutHeight))
        } else {
            WindowInsets(0)
        }
    return listOf(topBarInsets, bottomBarInsets, contentInsets, sideBarInsets)
}

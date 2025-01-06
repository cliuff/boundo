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

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import com.madness.collision.util.SystemUtil
import kotlin.math.absoluteValue

@Composable
fun rememberHorizontalCornerCutout(): Pair<WindowInsets, HorizontalCornerCutout> {
    val context = LocalContext.current
    val insetsCompat = LocalWindowInsets.current
    return remember(insetsCompat) { insetsCompat.resolveHorizontalCornerCutout(context) }
}

@JvmInline
value class HorizontalCornerCutout(private val values: List<Int>) {

    /**
     * @param side negative for the left side, positive for the right side.
     * @return inset space of the cutout if it's present on the [side], otherwise returns 0.
     */
    fun getInsetSpaceOrZero(side: Int): Int {
        return values[side.coerceIn(0, 1)]
    }

    /**
     * @return inset space of the cutout if it's on the upper part, otherwise returns 0.
     */
    fun getUpperInsetSpaceOrZero(side: Int): Int {
        return if (getUpperHeightOrZero(side) > 0) getInsetSpaceOrZero(side) else 0
    }

    /**
     * @return inset space of the cutout if it's on the lower part, otherwise returns 0.
     */
    fun getLowerInsetSpaceOrZero(side: Int): Int {
        return if (getLowerHeightOrZero(side) > 0) getInsetSpaceOrZero(side) else 0
    }

    /**
     * @return height of the cutout if it's on the upper part, otherwise returns 0.
     */
    fun getUpperHeightOrZero(side: Int): Int {
        return values[2 + side.coerceIn(0, 1)].coerceAtLeast(0)
    }

    /**
     * @return height of the cutout if it's on the lower part, otherwise returns 0.
     */
    fun getLowerHeightOrZero(side: Int): Int {
        return values[2 + side.coerceIn(0, 1)].coerceAtMost(0)
            .absoluteValue.coerceAtLeast(0)
    }

    // todo getInsetTop() and getInsetBottom(), in addition to upper/lower heights
}

// todo app bar hide out space
// whether horizontal cutout is contained in the upper/lower part along the edges
fun WindowInsetsCompat.resolveHorizontalCornerCutout(
    context: Context): Pair<WindowInsets, HorizontalCornerCutout> {

    val displayCutoutCompat = displayCutout
    // boundingRects had empty values removed, calculate positioned values manually
    val displayCutoutRects = displayCutoutCompat?.boundingRects.orEmpty()
    val cutoutRects = when {
        displayCutoutCompat == null || displayCutoutRects.isEmpty() -> emptyList()
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> emptyList()
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && displayCutoutRects.size != 1 -> emptyList()
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> when {
            displayCutoutCompat.safeInsetLeft > 0 -> listOf(displayCutoutRects[0], null)
            displayCutoutCompat.safeInsetRight > 0 -> listOf(null, displayCutoutRects[0])
            else -> emptyList()
        }
        else -> toWindowInsets()?.displayCutout?.run { listOf(boundingRectLeft, boundingRectRight) }
    }

    val cutoutInsets = getInsets(WindowInsetsCompat.Type.displayCutout())
    if (cutoutRects.isNullOrEmpty()) {
        val windowInsets = cutoutInsets.run { WindowInsets(left, top, right, bottom) }
        val cornerCutout = HorizontalCornerCutout(listOf(cutoutInsets.left, cutoutInsets.right, 0, 0))
        return windowInsets to cornerCutout
    }
    val windowSize = SystemUtil.getRuntimeWindowSize(context)
    val heights = cutoutRects.map { rect ->
        when {
            rect == null || rect.isEmpty -> 0
            // cutout is contained in the upper 1/3 part
            rect.bottom < windowSize.y / 3f -> rect.height()
            // cutout is contained in the lower 1/3 part
            rect.top > windowSize.y * 2 / 3f -> -rect.height()
            // cutout is contained in the center 1/3 part
            // todo small cutout height at the center
            else -> 0
        }
    }
    // exclude from display cutout insets the consumed ones
    val remainingInsets = cutoutInsets.run {
        WindowInsets(if (heights[0] == 0) left else 0, top, if (heights[1] == 0) right else 0, bottom)
    }
    val cornerCutout = listOf(cutoutInsets.left, cutoutInsets.right, heights[0], heights[1])
        .let(::HorizontalCornerCutout)
    return remainingInsets to cornerCutout
}

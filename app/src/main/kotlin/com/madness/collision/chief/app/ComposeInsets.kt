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

package com.madness.collision.chief.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Internal code from [WindowInsets.kt][WindowInsets]. */

/**
 * Convert a [PaddingValues] to a [WindowInsets].
 */
fun PaddingValues.asInsets(): WindowInsets = PaddingValuesInsets(this)

/**
 * An [WindowInsets] calculated from [paddingValues].
 */
@Stable
private class PaddingValuesInsets(private val paddingValues: PaddingValues) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    override fun getTop(density: Density) = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    override fun getRight(density: Density, layoutDirection: LayoutDirection) = with(density) {
        paddingValues.calculateRightPadding(layoutDirection).roundToPx()
    }

    override fun getBottom(density: Density) = with(density) {
        paddingValues.calculateBottomPadding().roundToPx()
    }

    override fun toString(): String {
        val layoutDirection = LayoutDirection.Ltr
        val start = paddingValues.calculateLeftPadding(layoutDirection)
        val top = paddingValues.calculateTopPadding()
        val end = paddingValues.calculateRightPadding(layoutDirection)
        val bottom = paddingValues.calculateBottomPadding()
        return "PaddingValues($start, $top, $end, $bottom)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PaddingValuesInsets) {
            return false
        }

        return other.paddingValues == paddingValues
    }

    override fun hashCode(): Int = paddingValues.hashCode()
}

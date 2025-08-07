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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.waterfall
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.times

object BottomSheetDefaults {
    val windowInsets: WindowInsets
        @NonRestartableComposable
        @Composable
        get() = WindowInsets.run { systemBars.union(displayCutout).union(waterfall) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun symmetricSheetMargin(maxWidth: Dp, windowInsets: WindowInsets): Pair<Dp, PaddingValues> {
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding = windowInsets.asPaddingValues()
    val (paddingStart, paddingEnd) = contentPadding.run {
        calculateStartPadding(layoutDirection) to calculateEndPadding(layoutDirection)
    }

    // apply symmetric margin to sheet's surface
    val symmetricHorizontal = max(paddingStart, paddingEnd)
    val useSymmetry = maxWidth - 2 * symmetricHorizontal >= BottomSheetDefaults.SheetMaxWidth
    val maxSheetWidth = when {
        useSymmetry -> BottomSheetDefaults.SheetMaxWidth
        else -> maxWidth - paddingStart - paddingEnd
    }
    val horizontalMargin = when {
        useSymmetry -> PaddingValues(horizontal = symmetricHorizontal)
        else -> PaddingValues(start = paddingStart, end = paddingEnd)
    }
    return maxSheetWidth to horizontalMargin
}

/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.util.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

fun Modifier.autoMirrored(): Modifier = composed {
    orWithRtl { scale(scaleX = -1f, scaleY = 1f) }
}

@Composable
inline fun <T> T.orWithRtl(rtl: () -> T) = layoutDirected({ this }, rtl)

@Composable
inline fun <T> layoutDirected(ltr: () -> T, rtl: () -> T) =
    when (LocalLayoutDirection.current) {
        LayoutDirection.Ltr -> ltr()
        LayoutDirection.Rtl -> rtl()
    }

abstract class ExpandableTextLayout(private val expandLineState: State<Int>) {
    val expandLine: Boolean get() = expandLineState.value and 0b01 == 0b01
    val setState: Boolean get() = expandLineState.value and 0b10 == 0b10
    abstract fun onTextLayout(layoutResult: TextLayoutResult)
}

@Composable
fun rememberExpandableTextLayout(resetKey: Any?, expandableWidth: Dp): ExpandableTextLayout {
    val expandableWidthPx = with(LocalDensity.current) { expandableWidth.toPx() }
    return remember(resetKey) {
        val expandLineState = mutableIntStateOf(0b10)
        object : ExpandableTextLayout(expandLineState) {
            override fun onTextLayout(layoutResult: TextLayoutResult) {
                if (!setState) return
                expandLineState.intValue = layoutResult.run {
                    if (lineCount != 2) return@run 0
                    val b = getLineRight(1) - getLineLeft(1) <= expandableWidthPx
                    b.compareTo(false)  // boolean to int
                }
            }
        }
    }
}

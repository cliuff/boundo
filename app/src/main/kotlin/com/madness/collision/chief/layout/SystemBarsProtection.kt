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

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.madness.collision.ui.theme.isAppInDarkTheme

object SystemBarsProtectionDefaults {

    val topSystemBarWindowInsets: WindowInsets
        @NonRestartableComposable
        @Composable
        get() = WindowInsets.systemBars.only(WindowInsetsSides.Top)

    /** [Tappable][tappableElement] [systemBars] window insets on the bottom (i.e. 3-btn navigation bar). */
    val tappableBottomSystemBarWindowInsets: WindowInsets
        @Composable
        get() = WindowInsets.run {
            val density = LocalDensity.current
            when {
                // gestural navigation does not have tappableElement inset
                tappableElement.getBottom(density) <= 0 -> WindowInsets()
                else -> systemBars.only(WindowInsetsSides.Bottom)
            }
        }

    @Composable
    fun derivedStatusBarColorOf(color: Color = MaterialTheme.colorScheme.background): Color {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color.copy(alpha = 0.9f)
        } else if (!isAppInDarkTheme()) {
            color.copy(alpha = 0.9f)
        } else {
            contentColorFor(color).copy(alpha = 0.27f)
        }
    }

    @Composable
    fun derivedNavBarColorOf(color: Color = MaterialTheme.colorScheme.background): Color {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            color.copy(alpha = 0.34f)
        } else if (!isAppInDarkTheme()) {
            color.copy(alpha = 0.34f)
        } else {
            contentColorFor(color).copy(alpha = 0.1f)
        }
    }
}

@Composable
fun StatusBarProtection(
    modifier: Modifier = Modifier,
    color: Color = SystemBarsProtectionDefaults.derivedStatusBarColorOf(),
    windowInsets: WindowInsets = SystemBarsProtectionDefaults.topSystemBarWindowInsets,
) {
    Canvas(modifier.fillMaxWidth().windowInsetsTopHeight(windowInsets)) {
        drawRect(color)
    }
}

@Composable
fun NavigationBarProtection(
    modifier: Modifier = Modifier,
    color: Color = SystemBarsProtectionDefaults.derivedNavBarColorOf(),
    windowInsets: WindowInsets = SystemBarsProtectionDefaults.tappableBottomSystemBarWindowInsets,
) {
    Canvas(modifier.fillMaxWidth().windowInsetsBottomHeight(windowInsets)) {
        drawRect(color)
    }
}

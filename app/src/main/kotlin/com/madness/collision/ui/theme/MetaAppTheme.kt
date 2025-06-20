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

package com.madness.collision.ui.theme

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.madness.collision.util.mainApplication

@Composable
fun PreviewAppTheme(
    isAppInDarkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme = appColorScheme(isAppInDarkTheme),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides colorScheme.onBackground,
        LocalAppInDarkTheme provides isAppInDarkTheme,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@Composable
fun MetaAppTheme(
    isAppInDarkTheme: Boolean = mainApplication.isDarkTheme,
    colorScheme: ColorScheme = appColorScheme(isAppInDarkTheme),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        // set default color for text as fallback when not set by Surface/Scaffold
        LocalContentColor provides colorScheme.onBackground,
        LocalAppInDarkTheme provides isAppInDarkTheme,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}


/** See [isAppInDarkTheme] instead for public use. */
// using compositionLocalOf following LocalConfiguration
private val LocalAppInDarkTheme = compositionLocalOf<Boolean> {
    error("LocalAppInDarkTheme not provided")
}

/**
 * Whether the app is in dark theme, determined by [LocalAppInDarkTheme].
 *
 * See also [isSystemInDarkTheme] that requires [Configuration.uiMode] correctly set,
 * either by [UiModeManager.setApplicationNightMode] or [AppCompatDelegate.setDefaultNightMode].
 */
@Composable
@ReadOnlyComposable
fun isAppInDarkTheme(): Boolean {
    return LocalAppInDarkTheme.current
}


@Composable
@ReadOnlyComposable
private fun appColorScheme(isInDarkTheme: Boolean): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (isInDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isInDarkTheme) darkColorScheme() else lightColorScheme()
    }
}

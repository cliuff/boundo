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

package com.madness.collision.unit.api_viewing.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.madness.collision.unit.api_viewing.ui.list.LaunchMethod
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun StandaloneAppList(
    query: CharSequence? = null,
    pkgInfo: Parcelable? = null,
    onStatusBarDarkIconChange: (Boolean) -> Unit,
) {
    val (homeNav, setHomeNav) = remember { mutableStateOf<AppHomeNav?>(null) }
    LaunchedEffect(homeNav) statusBar@{
        val nav = homeNav ?: return@statusBar
        nav.statusBarDarkIcon
            .onEach(onStatusBarDarkIconChange)
            .launchIn(this)
    }

    val contentInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    DisposableEffect(Unit) { onDispose { setHomeNav(null) } }
    HomeNavFragment(
        selectedPageIndex = 1,
        initArguments = Bundle().apply {
            // Intent.EXTRA_TEXT is retrieved as String
            if (query != null) putString(Intent.EXTRA_TEXT, query.toString())
            if (pkgInfo != null) putParcelable(LaunchMethod.EXTRA_DATA_STREAM, pkgInfo)
            if (pkgInfo != null) putInt(LaunchMethod.EXTRA_LAUNCH_MODE, LaunchMethod.LAUNCH_MODE_LINK)
        },
        contentPadding = contentInsets.asPaddingValues(),
        onUpdate = setHomeNav,
    )
}

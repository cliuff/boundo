/*
 * Copyright 2023 Clifford Liu
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

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils

class ComposeViewOwner {
    private var mutComposeView: ComposeView? = null
    private val composeView: ComposeView? by ::mutComposeView

    fun getView() = composeView

    fun createView(context: Context, lifecycleOwner: LifecycleOwner): ComposeView {
        val lifecycle = lifecycleOwner.lifecycle
        if (lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    owner.lifecycle.removeObserver(this)
                    mutComposeView = null
                }
            })
        }

        val view = ComposeView(context)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        mutComposeView = view
        return view
    }
}

@Composable
fun rememberColorScheme(): ColorScheme {
    return if (OsUtils.satisfy(OsUtils.S)) {
        val context = LocalContext.current
        val isDark = mainApplication.isDarkTheme
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
    }
}

@Composable
fun rememberContentPadding(mainViewModel: MainViewModel): PaddingValues {
    val insetTop by mainViewModel.contentWidthTop.observeAsState(0)
    val insetBottom by mainViewModel.contentWidthBottom.observeAsState(0)
    val density = LocalDensity.current
    return remember(insetTop, insetBottom) {
        with(density) { PaddingValues(top = insetTop.toDp(), bottom = insetBottom.toDp()) }
    }
}

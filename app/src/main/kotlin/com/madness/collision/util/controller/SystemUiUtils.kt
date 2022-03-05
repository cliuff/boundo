/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.util.controller

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import com.madness.collision.util.X
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.os.SystemBarConfig
import com.madness.collision.util.os.SystemBarUtils

interface SystemUiBarConfig {
    // null property value means stay untouched
    var color: Int?
    var isTransparentBar: Boolean?
    var isDarkIcon: Boolean?
    var isContrastEnforced: Boolean?

    companion object {
        operator fun invoke() = object : SystemUiBarConfig {
            override var color: Int? = null
            override var isTransparentBar: Boolean? = null
            override var isDarkIcon: Boolean? = null
            override var isContrastEnforced: Boolean? = null
        }
    }
}

interface NavigationBarConfig : SystemUiBarConfig {
    var dividerColor: Int?

    companion object {
        operator fun invoke() = object : NavigationBarConfig {
            override var color: Int? = null
            override var isTransparentBar: Boolean? = null
            override var isDarkIcon: Boolean? = null
            override var isContrastEnforced: Boolean? = null
            override var dividerColor: Int? = null
        }
    }
}

@DslMarker
private annotation class SystemUiConfigMarker

@SystemUiConfigMarker
abstract class SystemBarScope<C : SystemUiBarConfig>(private val xConfig: SystemUiBarConfig) {
    // used for external direct assignment
    var config: C? = null

    fun transparentBar() {
        xConfig.isTransparentBar = true
    }

    fun darkIcon() {
        xConfig.isDarkIcon = true
    }

    fun enforceContrast() {
        xConfig.isContrastEnforced = true
    }
}

class StatusBarScope(config: SystemUiBarConfig)
    : SystemBarScope<SystemUiBarConfig>(config), SystemUiBarConfig by config

class NavigationBarScope(config: NavigationBarConfig)
    : SystemBarScope<NavigationBarConfig>(config), NavigationBarConfig by config

@SystemUiConfigMarker
class SystemUiScope {
    var isFullscreen: Boolean? = false
    var statusBarConfig: SystemUiBarConfig? = null
    var navigationBarConfig: NavigationBarConfig? = null
    val statusBar: SystemUiBarConfig?
        get() = statusBarConfig
    val navigationBar: NavigationBarConfig?
        get() = navigationBarConfig

    fun fullscreen() {
        isFullscreen = true
    }

    fun statusBar(block: StatusBarScope.() -> Unit) {
        val config = statusBarConfig ?: SystemUiBarConfig()
        val scope = StatusBarScope(config).apply(block)
        // use scope's config instead if it is assigned
        statusBarConfig = scope.config ?: scope
    }

    fun navigationBar(block: NavigationBarScope.() -> Unit) {
        val config = navigationBarConfig ?: NavigationBarConfig()
        val scope = NavigationBarScope(config).apply(block)
        // use scope's config instead if it is assigned
        navigationBarConfig = scope.config ?: scope
    }
}

private fun configSystemUi(context: Context, window: Window, block: SystemUiScope.() -> Unit) {
    val systemUiConfig = SystemUiScope().apply(block)
    systemUiConfig.isFullscreen?.let { fullscreen ->
        WindowCompat.setDecorFitsSystemWindows(window, fullscreen.not())
    }
    val toConfig: SystemUiBarConfig.() -> SystemBarConfig = {
        SystemBarConfig(
            isDarkIcon = isDarkIcon ?: false,
            barColor = color ?: Color.TRANSPARENT,
            isTransparentBar = isTransparentBar ?: false,
            setDarkIcon = isDarkIcon != null
        )
    }
    systemUiConfig.statusBarConfig?.let { config ->
        SystemBarUtils.applyStatusBarConfig(context, window, toConfig(config))
        config.isContrastEnforced?.let { enforced ->
            if (OsUtils.satisfy(OsUtils.Q)) window.isStatusBarContrastEnforced = enforced
        }
    }
    systemUiConfig.navigationBarConfig?.let { config ->
        SystemBarUtils.applyNavBarConfig(context, window, toConfig(config))
        config.isContrastEnforced?.let { enforced ->
            if (OsUtils.satisfy(OsUtils.Q)) window.isNavigationBarContrastEnforced = enforced
        }
        config.dividerColor?.let { color ->
            if (OsUtils.satisfy(OsUtils.P)) window.navigationBarDividerColor = color
        }
    }
}

fun Activity.systemUi(block: SystemUiScope.() -> Unit) {
    configSystemUi(this, window, block)
}

fun Dialog.systemUi(block: SystemUiScope.() -> Unit) {
    val window = window ?: return
    configSystemUi(context, window, block)
}

fun DialogFragment.systemUi(block: SystemUiScope.() -> Unit) {
    dialog?.systemUi(block)
}

private fun edgeToEdge(context: Context, window: Window, bottomInset: Int? = null,
                       block: (SystemUiScope.() -> Unit)? = null) {
    val darkIcon = mainApplication.isPaleTheme
    val isTransparentNav: Boolean? = bottomInset?.let {
        it < X.size(context, 25f, X.DP)
    }
    configSystemUi(context, window) {
        fullscreen()
        statusBar {
            color = null
            isTransparentBar = true
            isDarkIcon = darkIcon
            isContrastEnforced = false
        }
        navigationBar {
            color = null
            isTransparentBar = isTransparentNav
            isDarkIcon = darkIcon
            isContrastEnforced = false
            dividerColor = null
        }
        block?.invoke(this)
    }
}

fun Activity.edgeToEdge(bottomInset: Int? = null, block: (SystemUiScope.() -> Unit)? = null) {
    edgeToEdge(this, window, bottomInset, block)
}

fun Dialog.edgeToEdge(bottomInset: Int? = null, block: (SystemUiScope.() -> Unit)? = null) {
    val window = window ?: return
    edgeToEdge(context, window, bottomInset, block)
}

fun DialogFragment.edgeToEdge(bottomInset: Int? = null, block: (SystemUiScope.() -> Unit)? = null) {
    dialog?.edgeToEdge(bottomInset, block)
}

private fun immersiveNavigation(context: Context, window: Window, bottomInset: Int) {
    val isTransparentNav = bottomInset < X.size(context, 25f, X.DP)
    configSystemUi(context, window) {
        fullscreen()  // this is required for unknown reason
        navigationBar { isTransparentBar = isTransparentNav }
    }
}

fun Activity.immersiveNavigation(bottomInset: Int) {
    immersiveNavigation(this, window, bottomInset)
}

fun Dialog.immersiveNavigation(bottomInset: Int) {
    val window = window ?: return
    immersiveNavigation(context, window, bottomInset)
}

fun DialogFragment.immersiveNavigation(bottomInset: Int) {
    dialog?.immersiveNavigation(bottomInset)
}

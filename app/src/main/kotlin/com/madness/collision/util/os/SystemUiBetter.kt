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

package com.madness.collision.util.os

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.Window
import android.view.WindowInsets
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.madness.collision.util.ElapsingTime
import com.madness.collision.util.mainApplication

interface SystemUiBarConfig {
    // null property value means stay untouched
    var color: Int?
    var isTransparentBar: Boolean?
    var isDarkIcon: Boolean?
    var isContrastEnforced: Boolean?
    // honored only in navigation bar
    var dividerColor: Int?

    companion object {
        operator fun invoke() = object : SystemUiBarConfig {
            override var color: Int? = null
            override var isTransparentBar: Boolean? = null
            override var isDarkIcon: Boolean? = null
            override var isContrastEnforced: Boolean? = null
            override var dividerColor: Int? = null
        }
    }

    fun copy() = object : SystemUiBarConfig {
        override var color: Int? = this@SystemUiBarConfig.color
        override var isTransparentBar: Boolean? = this@SystemUiBarConfig.isTransparentBar
        override var isDarkIcon: Boolean? = this@SystemUiBarConfig.isDarkIcon
        override var isContrastEnforced: Boolean? = this@SystemUiBarConfig.isContrastEnforced
        override var dividerColor: Int? = this@SystemUiBarConfig.dividerColor
    }

    fun string(): String {
        return "SystemUiBarConfig(color=$color, isTransparentBar=$isTransparentBar, isDarkIcon=$isDarkIcon, isContrastEnforced=$isContrastEnforced, dividerColor=$dividerColor)"
    }
}

@DslMarker
private annotation class SystemUiConfigMarker

@SystemUiConfigMarker
abstract class AbstractSystemBarScope<C : SystemUiBarConfig>(private val xConfig: SystemUiBarConfig) {
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

class SystemBarScope(config: SystemUiBarConfig)
    : AbstractSystemBarScope<SystemUiBarConfig>(config), SystemUiBarConfig by config

class SystemBarConfigInfo {
    var isFullscreen: Boolean? = false
    // 0-top, 1-bottom, 2-start, 3-end
    val systemBarConfigs: Array<SystemUiBarConfig?> = arrayOfNulls(4)

    fun copy() = SystemBarConfigInfo().apply {
        isFullscreen = this@SystemBarConfigInfo.isFullscreen
        val originalConfigs = this@SystemBarConfigInfo.systemBarConfigs
        for (i in systemBarConfigs.indices) systemBarConfigs[i] = originalConfigs[i]?.copy()
    }

    override fun toString(): String {
        val s = systemBarConfigs.joinToString(separator = ",\n") { "    " + (it?.string() ?: "null") }
        return "SystemBarConfigInfo(isFullscreen=$isFullscreen, systemBarConfigs={\n$s\n})"
    }
}

typealias SystemBarBlock = SystemBarScope.() -> Unit

@SystemUiConfigMarker
class SystemUiScope(private val configInfo: SystemBarConfigInfo) {
    var isFullscreen by configInfo::isFullscreen
    // 0-top, 1-bottom, 2-start, 3-end
    private val systemBarConfigs by configInfo::systemBarConfigs

    fun fullscreen() {
        isFullscreen = true
    }

    private fun configSide(index: Int, block: SystemBarBlock) {
        val config = systemBarConfigs[index] ?: SystemUiBarConfig()
        val scope = SystemBarScope(config).apply(block)
        // use scope's config instead if it is assigned
        systemBarConfigs[index] = scope.config ?: scope
    }

    fun top(block: SystemBarBlock) = configSide(0, block)

    fun bottom(block: SystemBarBlock) = configSide(1, block)

    fun start(block: SystemBarBlock) = configSide(2, block)

    fun end(block: SystemBarBlock) = configSide(3, block)
}

interface SystemBarMaintainer {
    val context: Context?
    val window: Window?
    // todo Layer
    val configStack: ArrayDeque<SystemBarConfigInfo>
    // Configs in effect
    val activeConfigs: Array<SystemUiBarConfig?>
    var activeInsets: WindowInsets?
    val lastInsetsTime: ElapsingTime

    fun setActiveConfigs(block: (Array<SystemUiBarConfig?>) -> Unit)
}

interface SystemBarMaintainerOwner {
    val systemBarMaintainer: SystemBarMaintainer
}

abstract class DefaultSystemBarMaintainer : SystemBarMaintainer {
    override val configStack: ArrayDeque<SystemBarConfigInfo> = ArrayDeque()
    override var activeConfigs: Array<SystemUiBarConfig?> = arrayOfNulls(4)
    override var activeInsets: WindowInsets? = null
    override val lastInsetsTime: ElapsingTime = ElapsingTime()

    override fun setActiveConfigs(block: (Array<SystemUiBarConfig?>) -> Unit) {
        activeConfigs = arrayOfNulls<SystemUiBarConfig>(4).also(block)
    }
}

class ActivitySystemBarMaintainer(private val activity: Activity) : DefaultSystemBarMaintainer() {
    override val context: Context by ::activity
    override val window: Window by activity::window
}

class DialogSystemBarMaintainer(private val dialog: Dialog) : DefaultSystemBarMaintainer() {
    override val context: Context by dialog::context
    override val window: Window? by dialog::window
}

class DialogFragmentSystemBarMaintainer(private val fragment: DialogFragment) : DefaultSystemBarMaintainer() {
    override val context: Context? by fragment::context
    override val window: Window? get() = fragment.dialog?.window
}

typealias SystemUiBlock = SystemUiScope.() -> Unit

private fun configSystemBars(maintainer: SystemBarMaintainer, insets: WindowInsets, newConfig: Boolean, block: SystemUiBlock) {
    val configStack = maintainer.configStack
    val lastConfig = configStack.lastOrNull()
    val configCopy = if (newConfig) lastConfig?.copy() else lastConfig
    val configInfo = configCopy ?: SystemBarConfigInfo()
    SystemUiScope(configInfo).apply(block)
    if (configInfo != lastConfig) configStack.add(configInfo)
    configSystemBars(maintainer, insets)
}

private fun revertSystemBars(maintainer: SystemBarMaintainer) {
    if (maintainer.configStack.isEmpty()) return
    val insets = maintainer.activeInsets ?: return
    maintainer.configStack.removeLastOrNull()
    configSystemBars(maintainer, insets)
}

private typealias InsetsType = WindowInsetsCompat.Type

private fun configSystemBars(maintainer: SystemBarMaintainer, insets: WindowInsets) {
    // Ensure 8ms minimum interval
    if (maintainer.lastInsetsTime.interval(8).not()) return
    val context = maintainer.context ?: return
    val window = maintainer.window ?: return
    val configInfo = maintainer.configStack.lastOrNull() ?: return
    configInfo.isFullscreen?.let { fullscreen ->
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
    val setStatusBar: (SystemUiBarConfig) -> Unit = { config ->
        SystemBarUtils.applyStatusBarConfig(context, window, toConfig(config))
        config.isContrastEnforced?.let { enforced ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@let
            window.isStatusBarContrastEnforced = enforced
        }
    }
    val setNavBar: (SystemUiBarConfig) -> Unit = { config ->
        SystemBarUtils.applyNavBarConfig(context, window, toConfig(config))
        config.isContrastEnforced?.let { enforced ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@let
            window.isNavigationBarContrastEnforced = enforced
        }
        config.dividerColor?.let { color ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@let
            window.navigationBarDividerColor = color
        }
    }

    val insetsCopy = WindowInsets(insets)
    val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insetsCopy)
    if (insetsCompat.hasInsets().not()) {
        maintainer.setActiveConfigs {  }
        maintainer.activeInsets = insetsCopy
        maintainer.lastInsetsTime.reset()
        return
    }
    val insetTypes = arrayOf(InsetsType.statusBars(), InsetsType.navigationBars())
    val configActions = arrayOf(setStatusBar, setNavBar)
    val insetSides = arrayOf(Insets::top, Insets::bottom, Insets::left, Insets::right)
    val sideConfigs = insetTypes.mapIndexedNotNull { typeIndex, type ->
        val typedInsets = insetsCompat.getInsets(type)
        // Only the first positive inset side (top/bottom/start/end) is configured
        val sideIndex = insetSides.indexOfFirst { it.get(typedInsets) > 0 }
        // Get corresponding system bar config by side index
        val sideConfig = configInfo.systemBarConfigs.getOrNull(sideIndex)
        // Configure system bars
        sideConfig?.let(configActions[typeIndex])
        if (sideIndex >= 0) sideIndex to sideConfig else null
    }
    maintainer.setActiveConfigs { configs ->
        sideConfigs.forEach { (side, c) -> configs[side] = c }
    }
    maintainer.activeInsets = insetsCopy
    maintainer.lastInsetsTime.reset()
}

private fun edgeToEdge(maintainer: SystemBarMaintainer, insets: WindowInsets, newConfig: Boolean, block: SystemUiBlock? = null) {
    val context = maintainer.context ?: return
    val darkIcon = mainApplication.isPaleTheme
    val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
    // tappableElement inset = 0 for gesture nav bar, and > 0 for 3-button nav bar
    val isGestureBottomNavBar = insetsCompat.getInsets(InsetsType.tappableElement()).bottom <= 0
    val effectiveInset = insetsCompat.getInsets(InsetsType.systemBars()).bottom.takeIf { it > 0 }
    val isTransparentNav: Boolean? = effectiveInset?.let { inset ->
        val metrics = context.resources.displayMetrics
        val sizeLimit = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25f, metrics)
        inset < sizeLimit
    }
    configSystemBars(maintainer, insets, newConfig) {
        fullscreen()
        top {
            color = null
            isTransparentBar = true
            isDarkIcon = darkIcon
            isContrastEnforced = false
        }
        bottom {
            color = null
            isTransparentBar = isGestureBottomNavBar || isTransparentNav != false
            isDarkIcon = darkIcon
            isContrastEnforced = false
            dividerColor = null
        }
        start {
            color = null
            isTransparentBar = true
            isDarkIcon = darkIcon
            isContrastEnforced = false
        }
        end {
            color = null
            isTransparentBar = true
            isDarkIcon = darkIcon
            isContrastEnforced = false
        }
        block?.invoke(this) // todo right to left layout support
    }
}

fun SystemBarMaintainerOwner.systemBars(insets: WindowInsets, newConfig: Boolean, block: SystemUiBlock) {
    configSystemBars(systemBarMaintainer, insets, newConfig, block = block)
}

fun SystemBarMaintainerOwner.revertSystemBars() {
    revertSystemBars(systemBarMaintainer)
}

fun SystemBarMaintainerOwner.enableEdgeToEdge() {
    val window = systemBarMaintainer.window ?: return
    WindowCompat.setDecorFitsSystemWindows(window, false)
}

fun SystemBarMaintainerOwner.edgeToEdge(insets: WindowInsets, newConfig: Boolean, block: SystemUiBlock? = null) {
    edgeToEdge(systemBarMaintainer, insets, newConfig, block)
}

fun SystemBarMaintainerOwner.checkInsets(insets: WindowInsets): Boolean {
    return insets != systemBarMaintainer.activeInsets
}

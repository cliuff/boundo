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

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import com.madness.collision.R
import com.madness.collision.util.ThemeUtil

/**
 * Pass as argument, differ from the actual color
 */
class SystemBarConfig(
    val isDarkIcon: Boolean,
    val barColor: Int = Color.TRANSPARENT,
    val isTransparentBar: Boolean = barColor == Color.TRANSPARENT,
    val setDarkIcon: Boolean = true,
)

object SystemBarUtils {
    fun applyStatusBarConfig(context: Context, window: Window, config: SystemBarConfig) {
        val statusBarController = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarController(context, config)
        } else {
            BrightIconStatusBarController(context, config)
        }
        statusBarController.setBarColor(window)
        statusBarController.setIconBrightness(window)
    }

    fun applyNavBarConfig(context: Context, window: Window, config: SystemBarConfig) {
        val navBarController = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NavBarController(context, config)
        } else {
            BrightIconNavBarController(context, config)
        }
        navBarController.setBarColor(window)
        navBarController.setIconBrightness(window)
    }
}

private abstract class SystemBarController(protected val config: SystemBarConfig) {
    abstract val barColor: Int
    abstract fun setIconBrightness(window: Window)
    abstract fun setBarColor(window: Window)
}

private val Window.insetsCtrl: WindowInsetsController?
    @RequiresApi(Build.VERSION_CODES.R)
    get() {
        // avoid NullPointerException:
        // 'WindowInsetsController com.android.internal.policy.DecorView.getWindowInsetsController()'
        peekDecorView() ?: return null
        return insetsController
    }

@RequiresApi(Build.VERSION_CODES.M)
private class StatusBarController(context: Context, config: SystemBarConfig) :
    SystemBarController(config) {
    override val barColor: Int = when {
        config.isTransparentBar -> Color.TRANSPARENT
        config.barColor != Color.TRANSPARENT -> config.barColor
        else -> {
            val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
            colorBack and 0xE6FFFFFF.toInt()
        }
    }

    override fun setIconBrightness(window: Window) {
        if (config.setDarkIcon.not()) return
        // fixme Neither method can change status bar color of MainActivity on Android 11 (R)
        // (tested on Pixel 2 XL and emulator)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            val appearance = if (config.isDarkIcon) mask else 0
            window.insetsCtrl?.setSystemBarsAppearance(appearance, mask)
        } else {
            applyStatusBarConfigLegacy(window, config)
        }
    }

    @Suppress("deprecation")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun applyStatusBarConfigLegacy(window: Window, config: SystemBarConfig) {
        window.decorView.apply {
            systemUiVisibility = if (config.isDarkIcon) {
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    override fun setBarColor(window: Window) {
        window.statusBarColor = barColor
    }
}

private class BrightIconStatusBarController(context: Context, config: SystemBarConfig) :
    SystemBarController(config) {
    override val barColor: Int = when {
        config.isDarkIcon || !config.isTransparentBar -> {
            if (config.barColor != Color.TRANSPARENT) {
                config.barColor
            } else if (!config.isDarkIcon) {
                val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
                colorBack and 0xE6FFFFFF.toInt()
            } else {
                val colorBack = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
                colorBack and 0x45FFFFFF
            }
        }
        else -> Color.TRANSPARENT
    }

    override fun setIconBrightness(window: Window) {
    }

    override fun setBarColor(window: Window) {
        window.statusBarColor = barColor
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private class NavBarController(context: Context, config: SystemBarConfig) :
    SystemBarController(config) {
    override val barColor: Int = when {
        config.isTransparentBar -> Color.TRANSPARENT
        config.barColor != Color.TRANSPARENT -> config.barColor
        else -> {
            val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
            colorBack and 0x56FFFFFF
        }
    }

    override fun setIconBrightness(window: Window) {
        if (config.setDarkIcon.not()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            val appearance = if (config.isDarkIcon) mask else 0
            window.insetsCtrl?.setSystemBarsAppearance(appearance, mask)
        } else {
            applyNavBarConfigLegacy(window, config)
        }
    }

    @Suppress("deprecation")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyNavBarConfigLegacy(window: Window, config: SystemBarConfig) {
        window.decorView.apply {
            systemUiVisibility = if (config.isDarkIcon) {
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
    }

    override fun setBarColor(window: Window) {
        window.navigationBarColor = barColor
    }
}

private class BrightIconNavBarController(context: Context, config: SystemBarConfig) :
    SystemBarController(config) {
    override val barColor: Int = when {
        config.isDarkIcon || !config.isTransparentBar -> {
            if (config.barColor != Color.TRANSPARENT) {
                config.barColor
            } else if (!config.isDarkIcon) {
                val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
                colorBack and 0x56FFFFFF
            } else {
                val colorBack = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
                colorBack and 0x19FFFFFF
            }
        }
        else -> Color.TRANSPARENT
    }

    override fun setIconBrightness(window: Window) {
    }

    override fun setBarColor(window: Window) {
        window.navigationBarColor = barColor
    }
}

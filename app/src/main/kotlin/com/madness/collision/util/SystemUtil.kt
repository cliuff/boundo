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

package com.madness.collision.util

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.os.LocaleList
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.os.OsUtils
import java.util.*

/**
 * Pass as argument, differ from the actual color
 */
class SystemBarConfig(val isDarkIcon: Boolean, val barColor: Int = Color.TRANSPARENT,
                      val isTransparentBar: Boolean = barColor == Color.TRANSPARENT,
                      val setDarkIcon: Boolean = true)


object SystemUtil {

    fun applyDefaultSystemUiVisibility(context: Context, window: Window, insetBottom: Int)
            : Pair<SystemBarConfig, SystemBarConfig> {
        val darkBar = mainApplication.isPaleTheme
        val isTransparentNav = insetBottom < X.size(context, 20f, X.DP)
        return applyStatusBarColor(context, window, darkBar, true) to
                applyNavBarColor(context, window, darkBar, isTransparentNav)
    }

    fun applyEdge2Edge(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    fun applyStatusBarColor(context: Context, window: Window, isDarkIcon: Boolean,
                            isTransparentBar: Boolean = false,
                            color: Int = Color.TRANSPARENT): SystemBarConfig {
        SystemBarConfig(isDarkIcon, color, isTransparentBar).let {
            applyStatusBarConfig(context, window, it)
            return it
        }
    }

    private val Window.insetsCtrl: WindowInsetsController?
        @RequiresApi(X.R)
        get() = try {
            insetsController
        } catch (e: NullPointerException) {
            e.printStackTrace()
            null
        }

    fun isDarkStatusIcon(window: Window): Boolean {
        if (OsUtils.satisfy(OsUtils.R)) window.insetsCtrl?.run {
            return systemBarsAppearance and WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS != 0
        }
        if (OsUtils.satisfy(OsUtils.M)) return isDarkStatusIconLegacy(window)
        return false
    }

    @Suppress("deprecation")
    @RequiresApi(OsUtils.M)
    private fun isDarkStatusIconLegacy(window: Window): Boolean {
        return window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0
    }

    fun isDarkNavIcon(window: Window): Boolean {
        if (OsUtils.satisfy(OsUtils.R)) window.insetsCtrl?.run {
            return systemBarsAppearance and WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS != 0
        }
        if (OsUtils.satisfy(OsUtils.O)) return isDarkNavIconLegacy(window)
        return false
    }

    @Suppress("deprecation")
    @RequiresApi(OsUtils.O)
    private fun isDarkNavIconLegacy(window: Window): Boolean {
        return window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR != 0
    }

    fun applyStatusBarConfig(context: Context, window: Window, config: SystemBarConfig){
        if (OsUtils.satisfy(OsUtils.M)) {
            if (config.setDarkIcon) {
                // todo use substitute in androidx
                // The IME needs a text view to be focused to be shown
//            val insetsCtrl = WindowInsetsControllerCompat(window, null)
                // todo Neither method can change status bar color of MainActivity on Android 11 (R)
                // (tested on Pixel 2 XL and emulator)
                if (OsUtils.satisfy(OsUtils.R)) {
                    val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    val appearance = if (config.isDarkIcon) mask else 0
                    window.insetsCtrl?.setSystemBarsAppearance(appearance, mask)
                } else {
                    applyStatusBarConfigLegacy(window, config)
                }
            }
            window.statusBarColor = when {
                config.isTransparentBar -> Color.TRANSPARENT
                config.barColor != Color.TRANSPARENT -> config.barColor
                else -> {
                    val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
                    colorBack and 0xE6FFFFFF.toInt()
                }
            }
        } else window.statusBarColor = when {
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
    }

    @Suppress("deprecation")
    @RequiresApi(X.M)
    private fun applyStatusBarConfigLegacy(window: Window, config: SystemBarConfig) {
        window.decorView.apply {
            systemUiVisibility = if (config.isDarkIcon) {
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    fun applyNavBarColor(context: Context, window: Window, isDarkIcon: Boolean,
                         isTransparentBar: Boolean = false,
                         color: Int = Color.TRANSPARENT): SystemBarConfig {
        SystemBarConfig(isDarkIcon, color, isTransparentBar).let {
            applyNavBarConfig(context, window, it)
            return it
        }
    }

    fun applyNavBarConfig(context: Context, window: Window, config: SystemBarConfig){
        if (X.aboveOn(X.O)) {
            if (config.setDarkIcon) {
                // todo use substitute in androidx
                if (X.aboveOn(X.R)) {
                    val mask = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    val appearance = if (config.isDarkIcon) mask else 0
                    window.insetsCtrl?.setSystemBarsAppearance(appearance, mask)
                } else {
                    applyNavBarConfigLegacy(window, config)
                }
            }
            window.navigationBarColor = when {
                config.isTransparentBar -> Color.TRANSPARENT
                config.barColor != Color.TRANSPARENT -> config.barColor
                else -> {
                    val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
                    colorBack and 0xE6FFFFFF.toInt()
                }
            }
        } else window.navigationBarColor = when {
            config.isDarkIcon || !config.isTransparentBar -> {
                if (config.barColor != Color.TRANSPARENT) {
                    config.barColor
                } else if (!config.isDarkIcon) {
                    val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
                    colorBack and 0xE6FFFFFF.toInt()
                } else {
                    val colorBack = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
                    colorBack and 0x40FFFFFF
                }
            }
            else -> Color.TRANSPARENT
        }
    }

    @Suppress("deprecation")
    @RequiresApi(X.O)
    private fun applyNavBarConfigLegacy(window: Window, config: SystemBarConfig) {
        window.decorView.apply {
            systemUiVisibility = if (config.isDarkIcon) {
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
    }

    fun applyIfLandscape(activity: Activity, normal: () -> Any, superior: () -> Any) {
        if (X.aboveOn(X.N) && activity.isInMultiWindowMode){
            normal.invoke()
            return
        }

        val display = getDisplay(activity) ?: return
        val rotation = display.rotation
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) superior.invoke() else normal.invoke()
    }

    /**
     * Not suitable for getting screen size, use [WindowMetrics] instead.
     */
    fun getDisplay(context: Context): Display? {
        return if (OsUtils.satisfy(OsUtils.R)) {
            try {
                context.display
            } catch (e: UnsupportedOperationException) {
                e.printStackTrace()
                null
            }
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            windowManager?.defaultDisplayLegacy
        }
    }

    @Suppress("deprecation")
    private val WindowManager.defaultDisplayLegacy: Display
        get() = defaultDisplay

    /**
     * A foldable phone folding inward has two physical displays.
     * A foldable phone folding outward has only one physical display but divided into two partitions,
     * only one of them is activated when the phone is folded.
     * Both window and display API can only get the size of the activated partition.
     * Runtime means that this API does not describe the real device.
     */
    fun getRuntimeMaximumSize(context: Context): Point {
        if (OsUtils.satisfy(OsUtils.R)) {
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?)?.let {
                val bounds = it.maximumWindowMetrics.bounds
                return Point(bounds.width(), bounds.height())
            }
        }
        val size = Point()
        getDisplay(context)?.getRealSizeLegacy(size)
        return size
    }

    @Suppress("deprecation")
    private fun Display.getRealSizeLegacy(size: Point) = getRealSize(size)

    fun getRuntimeWindowSize(context: Context): Point {
        if (OsUtils.satisfy(OsUtils.R)) {
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?)?.let {
                val bounds = it.currentWindowMetrics.bounds
                return Point(bounds.width(), bounds.height())
            }
        }
        val size = Point()
        // get display with activity context to get window size
        getDisplay(context)?.getSizeLegacy(size)
        return size
    }

    @Suppress("deprecation")
    private fun Display.getSizeLegacy(size: Point) = getSize(size)

    /**
     * Language settings in system
     */
    fun getLocaleSys(): Locale{
        return if (X.aboveOn(X.N)) LocaleList.getDefault()[0] else Locale.getDefault()
    }

    /**
     * Language system adjusted for app
     */
    fun getLocaleApp(): Locale{
        return if (X.aboveOn(X.N)) LocaleList.getAdjustedDefault()[0] else Locale.getDefault()
    }

    /**
     * In-app usr selected language, for development use only
     */
    fun getLocaleUsr(context: Context): Locale{
        return if (!mainApplication.debug) getLocaleApp()
        else SettingsFunc.getLocale(SettingsFunc.getLanguage(context))
    }

    fun getLocaleContext(context: Context, locale: Locale): Context {
        val newConfig = Configuration(context.resources.configuration)
        newConfig.setLocale(locale)
        return context.createConfigurationContext(newConfig)
    }

    fun getLocaleContextSys(context: Context): Context {
        return getLocaleContext(context, getLocaleSys())
    }

    @RequiresApi(X.R)
    fun hideIme(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.hide(WindowInsets.Type.ime())
    }

    @RequiresApi(X.R)
    fun showIme(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.show(WindowInsets.Type.ime())
    }

    fun hideImeCompat(context: Context, view: View, window: Window) {
        if (X.aboveOn(X.R)) {
            hideIme(window)
        } else {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager ?: return
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun showImeCompat(context: Context, view: View, window: Window) {
        if (X.aboveOn(X.R)) {
            showIme(window)
        } else {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager ?: return
            inputMethodManager.showSoftInput(view, 0)
        }
    }

    @RequiresApi(X.R)
    fun hideStatusBars(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.hide(WindowInsets.Type.statusBars())
    }

    @RequiresApi(X.R)
    fun showStatusBars(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.show(WindowInsets.Type.statusBars())
    }

    @Suppress("deprecation")
    private val flagFullscreenLegacy = WindowManager.LayoutParams.FLAG_FULLSCREEN

    fun hideStatusBarsCompat(window: Window) {
        if (X.aboveOn(X.R)) {
            hideStatusBars(window)
        } else {
            window.setFlags(flagFullscreenLegacy, flagFullscreenLegacy)
        }
    }

    fun showStatusBarsCompat(window: Window) {
        if (X.aboveOn(X.R)) {
            showStatusBars(window)
        } else {
            window.clearFlags(flagFullscreenLegacy)
        }
    }

    @RequiresApi(X.R)
    fun hideNavigationBars(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.hide(WindowInsets.Type.navigationBars())
    }

    @RequiresApi(X.R)
    fun showNavigationBars(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.show(WindowInsets.Type.navigationBars())
    }

    @RequiresApi(X.R)
    fun hideSystemBars(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.hide(WindowInsets.Type.systemBars())
    }

    @RequiresApi(X.R)
    fun showSystemBars(window: Window) {
        val controller = window.insetsCtrl ?: return
        controller.show(WindowInsets.Type.systemBars())
    }

    fun getResUiModeType(context: Context): Int {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    }

    fun getResUiModeNight(context: Context): Int {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    fun getSystemUiModeType(context: Context): Int {
        return (context.getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager).currentModeType
    }

    fun isSystemTvUi(context: Context): Boolean = getSystemUiModeType(context) == Configuration.UI_MODE_TYPE_TELEVISION

    fun unifyTvNavKeyCode(keyCode: Int): Int = when(keyCode) {
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> KeyEvent.KEYCODE_BACK
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> KeyEvent.KEYCODE_ENTER
        else -> keyCode
    }

}

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
import android.graphics.Point
import android.os.LocaleList
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.os.OsUtils
import java.util.*

object SystemUtil {

    private val Window.insetsCtrl: WindowInsetsController?
        @RequiresApi(X.R)
        get() {
            // avoid NullPointerException:
            // 'WindowInsetsController com.android.internal.policy.DecorView.getWindowInsetsController()'
            peekDecorView() ?: return null
            return insetsController
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
        @Suppress("DEPRECATION")
        getDisplay(context)?.getRealSize(size)
        return size
    }

    fun getRuntimeWindowSize(context: Context): Point {
        if (OsUtils.satisfy(OsUtils.R)) {
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?)?.let {
                val bounds = it.currentWindowMetrics.bounds
                return Point(bounds.width(), bounds.height())
            }
        }
        val size = Point()
        // get display with activity context to get window size
        @Suppress("DEPRECATION")
        getDisplay(context)?.getSize(size)
        return size
    }

    fun getLocaleContext(context: Context, locale: Locale): Context {
        val newConfig = Configuration(context.resources.configuration).apply {
            if (OsUtils.satisfy(OsUtils.N)) {
                val appList = LocaleUtils.getApp()
                val list = buildList(appList.size + 1) {
                    add(locale)
                    addAll(appList.filterNot { it == locale })
                }
                val array = list.toTypedArray()
                setLocales(LocaleList(*array))
            } else {
                setLocale(locale)
            }
        }
        return context.createConfigurationContext(newConfig)
    }

    fun getLocaleContextSys(context: Context): Context {
        return getLocaleContext(context, LocaleUtils.getApp()[0])
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

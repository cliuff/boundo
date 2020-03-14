package com.madness.collision.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.LocaleList
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import java.util.*

/**
 * Pass as argument, differ from the actual color
 */
class SystemBarConfig(val isDarkIcon: Boolean, val barColor: Int = Color.TRANSPARENT, val isTransparentBar: Boolean = barColor == Color.TRANSPARENT)


object SystemUtil {

    fun applyDefaultSystemUiVisibility(context: Context, window: Window, insetBottom: Int): Pair<SystemBarConfig, SystemBarConfig> {
        val darkBar = mainApplication.isPaleTheme
        val isTransparentNav = insetBottom < X.size(context, 20f, X.DP)
        return applyStatusBarColor(context, window, darkBar, true) to applyNavBarColor(context, window, darkBar, isTransparentNav)
    }

    fun applyEdge2Edge(window: Window){
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    }

    fun applyStatusBarColor(context: Context, window: Window, isDarkIcon: Boolean, isTransparentBar: Boolean = false, color: Int = Color.TRANSPARENT): SystemBarConfig {
        SystemBarConfig(isDarkIcon, color, isTransparentBar).let {
            applyStatusBarConfig(context, window, it)
            return it
        }
    }

    fun applyStatusBarConfig(context: Context, window: Window, config: SystemBarConfig){
        if (X.aboveOn(X.M)) {
            window.decorView.apply {
                systemUiVisibility = if (config.isDarkIcon) {
                    systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
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
        }else{
            window.statusBarColor = when {
                config.isDarkIcon || !config.isTransparentBar -> {
                    if (config.barColor != Color.TRANSPARENT){
                        config.barColor
                    }else if (!config.isDarkIcon){
                        val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
                        colorBack and 0xE6FFFFFF.toInt()
                    }else{
                        val colorBack = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
                        colorBack and 0x45FFFFFF
                    }
                }
                else -> Color.TRANSPARENT
            }
        }
    }

    fun applyNavBarColor(context: Context, window: Window, isDarkIcon: Boolean, isTransparentBar: Boolean = false, color: Int = Color.TRANSPARENT): SystemBarConfig {
        SystemBarConfig(isDarkIcon, color, isTransparentBar).let {
            applyNavBarConfig(context, window, it)
            return it
        }
    }

    fun applyNavBarConfig(context: Context, window: Window, config: SystemBarConfig){
        if (X.aboveOn(X.O)) {
            window.decorView.apply {
                systemUiVisibility = if (config.isDarkIcon) {
                    systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
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
        }else{
            window.navigationBarColor = when {
                config.isDarkIcon || !config.isTransparentBar -> {
                    if (config.barColor != Color.TRANSPARENT){
                        config.barColor
                    } else if (!config.isDarkIcon){
                        val colorBack = ThemeUtil.getColor(context, R.attr.colorABackground)
                        colorBack and 0xE6FFFFFF.toInt()
                    }else{
                        val colorBack = ThemeUtil.getColor(context, R.attr.colorAOnBackground)
                        colorBack and 0x40FFFFFF
                    }
                }
                else -> Color.TRANSPARENT
            }
        }
    }

    fun applyIfLandscape(activity: Activity, normal: () -> Any, superior: () -> Any){
        if (X.aboveOn(X.N) && activity.isInMultiWindowMode){
            normal.invoke()
            return
        }

        val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager? ?: return
        val rotation = windowManager.defaultDisplay.rotation
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) superior.invoke() else normal.invoke()
    }

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

}

/*
 * Copyright 2020 Clifford Liu
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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.PowerManager
import android.util.TypedValue
import androidx.core.content.res.use
import com.madness.collision.R
import com.madness.collision.misc.MiscMain
import com.madness.collision.unit.themed_wallpaper.ThemedWallpaperEasyAccess
import java.io.File
import java.util.*

object ThemeUtil {
    fun getThemeId(context: Context): Int {
        val resValue = TypedValue()
        context.theme.resolveAttribute(R.attr.themeId, resValue, false)
        return resValue.data
    }

    fun getIsDarkTheme(context: Context): Boolean{
        val resIsDark = TypedValue()
        context.theme.resolveAttribute(R.attr.isDarkTheme, resIsDark, true)
        return resIsDark.data != 0 // cannot be resIsDark.data == 1
    }

    fun getIsPaleTheme(context: Context): Boolean{
        val resIsPale = TypedValue()
        context.theme.resolveAttribute(R.attr.isPaleTheme, resIsPale, true)
        return resIsPale.data != 0 // cannot be resIsPale.data == 1
    }

    fun getColor(context: Context, attr: Int): Int{
        val resColor = TypedValue()
        context.theme.resolveAttribute(attr, resColor, true)
        return resColor.data
    }

    fun getBackColor(color: Int, fraction: Float): Int{
        return if (mainApplication.isDarkTheme) ColorUtil.darkenAs(color, fraction)
        else ColorUtil.lightenAs(color, fraction)
    }

    fun getIsNight(context: Context): Boolean{
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> true // Night mode is active, we're using dark theme
            else -> false
        }
    }

    fun shouldChangeTheme4ThemedWallpaper(context: Context, prefSettings: SharedPreferences): Boolean{
        val resources = context.resources

        if (getIsNight(context)) return updateIsDark4TW(context, true)

        val keyApplyDarkPlan = resources.getString(R.string.prefExteriorKeyDarkPlan)
        val planValue = prefSettings.getString(keyApplyDarkPlan, resources.getString(R.string.prefExteriorDefaultDarkPlan)) ?: ""
        if(planValue == resources.getString(R.string.prefExteriorDarkPlanValueSchedule)) {
            val keyScheduleStart = resources.getString(R.string.prefExteriorKeyDarkPlanScheduleStart)
            val keyScheduleEnd = resources.getString(R.string.prefExteriorKeyDarkPlanScheduleEnd)
            val scheduleStart = prefSettings.getString(keyScheduleStart, resources.getString(R.string.prefExteriorDefaultDarkPlanScheduleStart)) ?: ""
            val scheduleEnd = prefSettings.getString(keyScheduleEnd, resources.getString(R.string.prefExteriorDefaultDarkPlanScheduleEnd)) ?: ""
            val regex = "(\\d{2})(\\d{2})".toRegex()
            val (hStart, mStart) = regex.find(scheduleStart)!!.destructured
            val timeStart = Calendar.getInstance()
            timeStart.set(Calendar.HOUR_OF_DAY, hStart.toInt())
            timeStart.set(Calendar.MINUTE, mStart.toInt())
            val (hEnd, mEnd) = regex.find(scheduleEnd)!!.destructured
            val timeEnd = Calendar.getInstance()
            timeEnd.set(Calendar.HOUR_OF_DAY, hEnd.toInt())
            timeEnd.set(Calendar.MINUTE, mEnd.toInt())
            val cal = Calendar.getInstance()
            if (timeStart.before(timeEnd)){
                if (cal.after(timeStart) && cal.before(timeEnd)) return updateIsDark4TW(context, true)
            }else{
                if (cal.after(timeStart) || cal.before(timeEnd)) return updateIsDark4TW(context, true)
            }
        }
        return updateIsDark4TW(context, false)
    }

    private fun updateIsDark4TW(context: Context, isDarkTheme: Boolean): Boolean{
        synchronized(ThemedWallpaperEasyAccess){
            val isDarkThemePrevious = ThemedWallpaperEasyAccess.isDark
            ThemedWallpaperEasyAccess.isDark = isDarkTheme
            val darkChanged = isDarkThemePrevious != isDarkTheme
            if (darkChanged) {
                val app = ThemedWallpaperEasyAccess
                val backPath = if (app.isDark) F.valFilePubTwPortraitDark(context)
                else F.valFilePubTwPortrait(context)
                app.background = if (File(backPath).exists()) Drawable.createFromPath(backPath)
                else ColorDrawable(if (app.isDark) Color.BLACK else Color.WHITE)
                ThemedWallpaperEasyAccess.wallpaperTimestamp = System.currentTimeMillis()
            }
            return darkChanged
        }
    }

    fun updateTheme(context: Context, prefSettings: SharedPreferences, setNewTheme: Boolean = true): Int{
        val resources = context.resources
        var themeId: Int = R.style.AppTheme

        val keyApplyDarkPlan = resources.getString(R.string.prefExteriorKeyDarkPlan)
        val planValue = prefSettings.getString(keyApplyDarkPlan, resources.getString(R.string.prefExteriorDefaultDarkPlan)) ?: ""

        val keyDarkTheme = resources.getString(R.string.prefExteriorKeyDarkTheme)
        val darkValue = prefSettings.getString(keyDarkTheme, resources.getString(R.string.prefExteriorDefaultDarkTheme)) ?: ""
        val valuesDarkTheme = resources.obtainTypedArray(R.array.prefExteriorDarkThemeValues)
        // entriesDarkTheme and valuesDarkTheme are recycled during the call
        val darkIndex = P.getPrefIndex(darkValue, valuesDarkTheme)
        val darkThemeId = resources.obtainTypedArray(R.array.prefExteriorDarkThemeRes).use {
            it.getResourceId(darkIndex, -1)
        }

        val isAlways = planValue == resources.getString(R.string.prefExteriorDarkPlanValueAlways)
        if (isAlways){
            if (setNewTheme) {
                context.setTheme(darkThemeId)
                updateIsDarkTheme(context, true)
            }
            themeId = darkThemeId
            return themeId
        }

        val keyLightTheme = resources.getString(R.string.prefExteriorKeyLightTheme)
        val lightValue = prefSettings.getString(keyLightTheme, resources.getString(R.string.prefExteriorDefaultLightTheme)) ?: ""

        val valuesLightTheme = resources.obtainTypedArray(R.array.prefExteriorLightThemeValues)
        val lightIndex = P.getPrefIndex(lightValue, valuesLightTheme)
        val lightThemeId = resources.obtainTypedArray(R.array.prefExteriorLightThemeRes).use {
            it.getResourceId(lightIndex, -1)
        }

        val keyBS = resources.getString(R.string.prefExteriorKeyDarkByBatterySaver)
        val valBSDefault = resources.getBoolean(R.bool.prefExteriorDefaultDarkByBatterySaver)
        val byBatterySaver = prefSettings.getBoolean(keyBS, valBSDefault)
        if (byBatterySaver){
            val powerSaveMode = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
            if (powerSaveMode) {
                if (setNewTheme) {
                    context.setTheme(darkThemeId)
                    updateIsDarkTheme(context, true)
                }
                themeId = darkThemeId
                return themeId
            }
        }

        val lightValueDefault = resources.getString(R.string.prefExteriorLightThemeValueWhite)
        when(planValue){
            resources.getString(R.string.prefExteriorDarkPlanValueNever) -> {
                if (lightValue != lightValueDefault || getThemeId(context) == R.style.LaunchScreen) {
                    if (setNewTheme) {
                        context.setTheme(lightThemeId)
                        updateIsDarkTheme(context, false)
                    }
                    themeId = lightThemeId
                }
                return themeId
            }
            resources.getString(R.string.prefExteriorDarkPlanValueAuto) -> {
                if (getIsNight(context)) {
                    if (setNewTheme) {
                        context.setTheme(darkThemeId)
                        updateIsDarkTheme(context, true)
                    }
                    themeId = darkThemeId
                    return themeId
                }
            }
            resources.getString(R.string.prefExteriorDarkPlanValueSchedule) -> {
                val keyScheduleStart = resources.getString(R.string.prefExteriorKeyDarkPlanScheduleStart)
                val keyScheduleEnd = resources.getString(R.string.prefExteriorKeyDarkPlanScheduleEnd)
                val scheduleStart = prefSettings.getString(keyScheduleStart, resources.getString(R.string.prefExteriorDefaultDarkPlanScheduleStart)) ?: ""
                val scheduleEnd = prefSettings.getString(keyScheduleEnd, resources.getString(R.string.prefExteriorDefaultDarkPlanScheduleEnd)) ?: ""
                val regex = "(\\d{2})(\\d{2})".toRegex()
                val (hStart, mStart) = regex.find(scheduleStart)!!.destructured
                val timeStart = Calendar.getInstance()
                timeStart.set(Calendar.HOUR_OF_DAY, hStart.toInt())
                timeStart.set(Calendar.MINUTE, mStart.toInt())
                val (hEnd, mEnd) = regex.find(scheduleEnd)!!.destructured
                val timeEnd = Calendar.getInstance()
                timeEnd.set(Calendar.HOUR_OF_DAY, hEnd.toInt())
                timeEnd.set(Calendar.MINUTE, mEnd.toInt())
                val cal = Calendar.getInstance()
                if (timeStart.before(timeEnd)){
                    if (cal.after(timeStart) && cal.before(timeEnd)){
                        if (setNewTheme) {
                            context.setTheme(darkThemeId)
                            updateIsDarkTheme(context, true)
                        }
                        themeId = darkThemeId
                        return themeId
                    }
                }else{
                    if (cal.after(timeStart) || cal.before(timeEnd)){
                        if (setNewTheme) {
                            context.setTheme(darkThemeId)
                            updateIsDarkTheme(context, true)
                        }
                        themeId = darkThemeId
                        return themeId
                    }
                }
            }
        }
        if (lightValue != lightValueDefault || getThemeId(context) == R.style.LaunchScreen) {
            if (setNewTheme) context.setTheme(lightThemeId)
            themeId = lightThemeId
        }
        updateIsDarkTheme(context, false)
        return themeId
    }

    /**
     * Update isDarkTheme and isPaleTheme
     * @return isDarkTheme value is changed
     */
    fun updateIsDarkTheme(context: Context, isDarkTheme: Boolean): Boolean{
        synchronized(mainApplication){
            mainApplication.isPaleTheme = getIsPaleTheme(context)
            val isDarkThemePrevious = mainApplication.isDarkTheme
            mainApplication.isDarkTheme = isDarkTheme
            val darkChanged = isDarkThemePrevious != isDarkTheme
            if (darkChanged) MiscMain.updateExteriorBackgrounds(context)
            return darkChanged
        }
    }
}

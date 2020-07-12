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

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import android.content.SharedPreferences
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.P

internal object EasyAccess {
    var shouldRoundIcon = false
    var shouldManifestedRound = false
    var shouldClip2Round = false
    var isSweet: Boolean = false
    var shouldIncludeDisabled = false
    /**
     * indicate that in viewing target api mode
     */
    var isViewingTarget = false
    var loadLimitHalf = 90
    var preloadLimit = 80
    var loadAmount = 10
    var shouldShowTagPackageInstallerGooglePlay = false
    var shouldShowTagPackageInstallerPackageInstaller = false
    val shouldShowTagPackageInstaller: Boolean
        get() = shouldShowTagPackageInstallerGooglePlay || shouldShowTagPackageInstallerPackageInstaller
    var shouldShowTagCrossPlatformFlutter = false
    var shouldShowTagCrossPlatformReactNative = false
    var shouldShowTagCrossPlatformXarmarin = false
    val shouldShowTagCrossPlatform: Boolean
        get() = shouldShowTagCrossPlatformFlutter || shouldShowTagCrossPlatformReactNative || shouldShowTagCrossPlatformXarmarin
    var shouldShowTagNativeLibArm = false
    var shouldShowTagNativeLibX86 = false
    val shouldShowTagNativeLib: Boolean
        get() = shouldShowTagNativeLibArm || shouldShowTagNativeLibX86
    var shouldShowTagPrivilegeSystem = false
    var shouldShowTagIconAdaptive = false
    var shouldShowTagHasSplits = false

    fun init(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)) {
        load(context, prefSettings, false)
    }

    fun isChanged(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)): Boolean {
        return load(context, prefSettings, true)
    }

    /**
     * @param isLazy whether to fast detect change. True to detect change, false to enforce complete loading.
     * @return whether there is change
     */
    fun load(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE), isLazy: Boolean): Boolean {
        var isChanged = false
        isViewingTarget = prefSettings.getBoolean(PrefUtil.AV_VIEWING_TARGET, PrefUtil.AV_VIEWING_TARGET_DEFAULT).also {
            isChanged = isChanged || it != isViewingTarget
            if (isLazy && isChanged) return true
        }
        shouldRoundIcon = prefSettings.getBoolean(PrefUtil.API_CIRCULAR_ICON, PrefUtil.API_CIRCULAR_ICON_DEFAULT).also {
            isChanged = isChanged || it != shouldRoundIcon
            if (isLazy && isChanged) return true
        }
        shouldManifestedRound = prefSettings.getBoolean(PrefUtil.API_PACKAGE_ROUND_ICON, PrefUtil.API_PACKAGE_ROUND_ICON_DEFAULT).also {
            isChanged = isChanged || it != shouldManifestedRound
            if (isLazy && isChanged) return true
        }
        shouldClip2Round = prefSettings.getBoolean(PrefUtil.AV_CLIP_ROUND, PrefUtil.AV_CLIP_ROUND_DEFAULT).also {
            isChanged = isChanged || it != shouldClip2Round
            if (isLazy && isChanged) return true
        }
        isSweet = prefSettings.getBoolean(PrefUtil.AV_SWEET, PrefUtil.AV_SWEET_DEFAULT).also {
            isChanged = isChanged || it != isSweet
            if (isLazy && isChanged) return true
        }
        shouldIncludeDisabled = prefSettings.getBoolean(PrefUtil.AV_INCLUDE_DISABLED, PrefUtil.AV_INCLUDE_DISABLED_DEFAULT).also {
            isChanged = isChanged || it != shouldIncludeDisabled
            if (isLazy && isChanged) return true
        }

        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, HashSet())!!
        shouldShowTagPackageInstallerGooglePlay = tagSettings.contains(context.getString(R.string.prefAvTagsValuePackageInstallerGp)).also {
            isChanged = isChanged || it != shouldShowTagPackageInstallerGooglePlay
            if (isLazy && isChanged) return true
        }
        shouldShowTagPackageInstallerPackageInstaller = tagSettings.contains(context.getString(R.string.prefAvTagsValuePackageInstallerPi)).also {
            isChanged = isChanged || it != shouldShowTagPackageInstallerPackageInstaller
            if (isLazy && isChanged) return true
        }
        shouldShowTagCrossPlatformFlutter = tagSettings.contains(context.getString(R.string.prefAvTagsValueCrossPlatformFlu)).also {
            isChanged = isChanged || it != shouldShowTagCrossPlatformFlutter
            if (isLazy && isChanged) return true
        }
        shouldShowTagCrossPlatformReactNative = tagSettings.contains(context.getString(R.string.prefAvTagsValueCrossPlatformRn)).also {
            isChanged = isChanged || it != shouldShowTagCrossPlatformReactNative
            if (isLazy && isChanged) return true
        }
        shouldShowTagCrossPlatformXarmarin = tagSettings.contains(context.getString(R.string.prefAvTagsValueCrossPlatformXar)).also {
            isChanged = isChanged || it != shouldShowTagCrossPlatformXarmarin
            if (isLazy && isChanged) return true
        }
        shouldShowTagNativeLibArm = tagSettings.contains(context.getString(R.string.prefAvTagsValueNativeLibArm)).also {
            isChanged = isChanged || it != shouldShowTagNativeLibArm
            if (isLazy && isChanged) return true
        }
        shouldShowTagNativeLibX86 = tagSettings.contains(context.getString(R.string.prefAvTagsValueNativeLibX86)).also {
            isChanged = isChanged || it != shouldShowTagNativeLibX86
            if (isLazy && isChanged) return true
        }
        shouldShowTagPrivilegeSystem = tagSettings.contains(context.getString(R.string.prefAvTagsValuePrivilegeSys)).also {
            isChanged = isChanged || it != shouldShowTagPrivilegeSystem
            if (isLazy && isChanged) return true
        }
        shouldShowTagIconAdaptive = tagSettings.contains(context.getString(R.string.prefAvTagsValueIconAdaptive)).also {
            isChanged = isChanged || it != shouldShowTagIconAdaptive
            if (isLazy && isChanged) return true
        }
        shouldShowTagHasSplits = tagSettings.contains(context.getString(R.string.prefAvTagsValueHasSplits)).also {
            isChanged = isChanged || it != shouldShowTagHasSplits
            if (isLazy && isChanged) return true
        }
        return isChanged
    }
}

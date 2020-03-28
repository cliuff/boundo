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

    fun init(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)) {
        isViewingTarget = prefSettings.getBoolean(PrefUtil.AV_VIEWING_TARGET, PrefUtil.AV_VIEWING_TARGET_DEFAULT)
        shouldRoundIcon = prefSettings.getBoolean(PrefUtil.API_CIRCULAR_ICON, PrefUtil.API_CIRCULAR_ICON_DEFAULT)
        shouldManifestedRound = prefSettings.getBoolean(PrefUtil.API_PACKAGE_ROUND_ICON, PrefUtil.API_PACKAGE_ROUND_ICON_DEFAULT)
        shouldClip2Round = prefSettings.getBoolean(PrefUtil.AV_CLIP_ROUND, PrefUtil.AV_CLIP_ROUND_DEFAULT)
        isSweet = prefSettings.getBoolean(PrefUtil.AV_SWEET, PrefUtil.AV_SWEET_DEFAULT)
        shouldIncludeDisabled = prefSettings.getBoolean(PrefUtil.AV_INCLUDE_DISABLED, PrefUtil.AV_INCLUDE_DISABLED_DEFAULT)

        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, HashSet())!!
        shouldShowTagPackageInstallerGooglePlay = tagSettings.contains(context.getString(R.string.prefAvTagsValuePackageInstallerGp))
        shouldShowTagPackageInstallerPackageInstaller = tagSettings.contains(context.getString(R.string.prefAvTagsValuePackageInstallerPi))
        shouldShowTagCrossPlatformFlutter = tagSettings.contains(context.getString(R.string.prefAvTagsValueCrossPlatformFlu))
        shouldShowTagCrossPlatformReactNative = tagSettings.contains(context.getString(R.string.prefAvTagsValueCrossPlatformRn))
        shouldShowTagCrossPlatformXarmarin = tagSettings.contains(context.getString(R.string.prefAvTagsValueCrossPlatformXar))
        shouldShowTagNativeLibArm = tagSettings.contains(context.getString(R.string.prefAvTagsValueNativeLibArm))
        shouldShowTagNativeLibX86 = tagSettings.contains(context.getString(R.string.prefAvTagsValueNativeLibX86))
        shouldShowTagPrivilegeSystem = tagSettings.contains(context.getString(R.string.prefAvTagsValuePrivilegeSys))
    }
}

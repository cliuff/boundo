package com.madness.collision.unit.api_viewing.data

import android.content.Context
import android.content.SharedPreferences
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

    fun init(context: Context) {
        init(context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE))
    }

    fun init(prefSettings: SharedPreferences) {
        isViewingTarget = prefSettings.getBoolean(P.AV_VIEWING_TARGET, P.AV_VIEWING_TARGET_DEFAULT)
        shouldRoundIcon = prefSettings.getBoolean(P.API_CIRCULAR_ICON, P.API_CIRCULAR_ICON_DEFAULT)
        shouldManifestedRound = prefSettings.getBoolean(P.API_PACKAGE_ROUND_ICON, P.API_PACKAGE_ROUND_ICON_DEFAULT)
        shouldClip2Round = prefSettings.getBoolean(P.AV_CLIP_ROUND, P.AV_CLIP_ROUND_DEFAULT)
        isSweet = prefSettings.getBoolean(P.AV_SWEET, P.AV_SWEET_DEFAULT)
        shouldIncludeDisabled = prefSettings.getBoolean(P.AV_INCLUDE_DISABLED, P.AV_INCLUDE_DISABLED_DEFAULT)
    }
}

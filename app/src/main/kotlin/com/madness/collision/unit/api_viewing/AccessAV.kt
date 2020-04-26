package com.madness.collision.unit.api_viewing

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitAccess
import com.madness.collision.util.P

object AccessAV: UnitAccess(Unit.UNIT_NAME_API_VIEWING) {

    const val EXTRA_DATA_STREAM = "apiData"
    const val HANDLE_DISPLAY_APK = 1
    const val EXTRA_LAUNCH_MODE: String  = "launch mode"
    const val LAUNCH_MODE_SEARCH: Int  = 1
    /**
     * from url link sharing
     */
    const val LAUNCH_MODE_LINK: Int  = 2

    fun clearSeals() {
        invokeWithoutArg("clearSeals")
    }

    fun clearApps(activity: ComponentActivity) {
        getMethod("clearApps", ComponentActivity::class).invoke(activity)
    }

    fun initTagSettings(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)) {
        getMethod("initTagSettings", Context::class, SharedPreferences::class).invoke(context, prefSettings)
    }

    fun updateTagSettingsAi(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)) {
        getMethod("updateTagSettingsAi", Context::class, SharedPreferences::class).invoke(context, prefSettings)
    }

}

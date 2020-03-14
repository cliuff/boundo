package com.madness.collision.unit.api_viewing

import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitAccess

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

    fun clearApps(activity: AppCompatActivity) {
        getMethod("clearApps", AppCompatActivity::class).invoke(activity)
    }
}

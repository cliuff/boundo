package com.madness.collision.util

import androidx.activity.ComponentActivity
import com.madness.collision.unit.api_viewing.AccessAV

object MemoryManager {

    fun ensureSpace(amount: Int) {
    }

    fun clearSpace(activity: ComponentActivity? = null) {
        AccessAV.clearSeals()
        if (activity == null) return
        AccessAV.clearApps(activity)
    }
}

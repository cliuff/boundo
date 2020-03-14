package com.madness.collision.util

import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.unit.api_viewing.AccessAV

object MemoryManager {

    fun ensureSpace(amount: Int) {
    }

    fun clearSpace(activity: AppCompatActivity) {
        AccessAV.clearSeals()
        AccessAV.clearApps(activity)
    }
}

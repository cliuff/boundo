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

import android.app.ActivityManager
import android.content.Context
import androidx.activity.ComponentActivity
import com.madness.collision.unit.api_viewing.AccessAV

object MemoryManager {

    fun ensureSpace(amount: Int) {
    }

    fun clearSpace(activity: ComponentActivity? = null) {
        AccessAV.clearTags()
        if (activity == null) return
        AccessAV.clearApps(activity)
    }

    fun requireMemoryIntensive(context: Context, block: () -> Unit) {
        if (context.availableMemory.lowMemory.not()) block()
    }

    // Get a MemoryInfo object for the device's current memory status.
    fun getAvailableMemory(context: Context): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also {
            activityManager.getMemoryInfo(it)
        }
    }
}

val Context.availableMemory: ActivityManager.MemoryInfo
    get() = MemoryManager.getAvailableMemory(this)

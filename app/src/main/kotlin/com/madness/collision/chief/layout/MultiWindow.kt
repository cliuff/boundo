/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.chief.layout

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.madness.collision.chief.os.DistroSpec
import com.madness.collision.chief.os.distro

/**
 * Check whether in desktop windowing mode.
 *
 * OneUI: Samsung DeX mode;
 * HyperOS/MIUI: 小窗、迷你小窗、工作台模式 (desktop windowing).
 */
@RequiresApi(Build.VERSION_CODES.N)
fun Activity.isInFreeformMode(): Boolean =
    when {
        !isInMultiWindowMode -> false
        DistroSpec.OneUI in distro -> isInDesktopMode_OneUI(this)
        DistroSpec.MIUI in distro -> isInFreeformWindowingMode(this)
        else -> isInFreeformWindowingMode(this)
    }

@RequiresApi(Build.VERSION_CODES.N)
private fun isInFreeformWindowingMode(activity: Activity): Boolean {
    val config = activity.resources.configuration.toString()
    Log.d("MultiWindow", config)
    return "WindowingMode=freeform" in config
}

@Suppress("FunctionName")
@RequiresApi(Build.VERSION_CODES.N)
private fun isInDesktopMode_OneUI(activity: Activity): Boolean {
    val config = activity.resources.configuration
    val isDexEnabled = runCatching {
        val clazz = config::class.java
        val semDexMode = clazz.getField("SEM_DESKTOP_MODE_ENABLED").getInt(null)
        val mIsDex = clazz.getField("semDesktopModeEnabled").getInt(config)
        mIsDex == semDexMode
    }.onFailure(Throwable::printStackTrace)
    return isDexEnabled.getOrDefault(false)
}

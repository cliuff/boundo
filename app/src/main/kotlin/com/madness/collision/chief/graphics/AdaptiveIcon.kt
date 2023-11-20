/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.chief.graphics

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import android.graphics.RegionIterator
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import com.madness.collision.chief.chiefContext
import com.madness.collision.chief.os.DistroSpec
import com.madness.collision.chief.os.distro
import com.madness.collision.util.os.OsUtils

object AdaptiveIcon {
    init {
        val systemRect = when {
            OsUtils.satisfy(OsUtils.O) -> getSystemIconMask()?.let(::isFullRect)
            else -> null
        }
        val miuiRect = distro[DistroSpec.MIUI]?.let { getMiuiIconMask()?.let(::isFullRect) }
        Log.d("AdaptiveIcon", "init/sysRect:$systemRect/miuiRect:$miuiRect")
    }

    fun hasRectIconMask(drawable: Drawable): Boolean {
        val isRect = when {
            OsUtils.satisfy(OsUtils.O) && drawable is AdaptiveIconDrawable ->
                drawable.iconMask.let(::isFullRect)
            else -> null
        }
        if (isRect == true) Log.d("AdaptiveIcon", "hasRectIconMask/hasRect:$isRect")
        return isRect == true
    }

    /**
     * Behaves similar to [getSystemIconMask],
     * the value is overridden by custom icon packs (in most cases null).
     * Prefer this than [getSystemIconMask], which always returns full rect on MIUI.
     */
    private fun getMiuiIconMask(): Path? {
        return MiuiIconCustomizer.getIconMaskPath()
    }

    @SuppressLint("PrivateApi", "DiscouragedApi")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getSystemIconMask(): Path? {
        val maskId = Resources.getSystem()
            .getIdentifier("config_icon_mask", "string", "android")
        if (maskId <= 0) return null
        try {
            val pathData = chiefContext.getString(maskId)
            Log.d("AdaptiveIcon", "getIconMask/config_icon_mask[$pathData]")
            return PathParser().parsePathString(pathData).toPath().asAndroidPath()
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
        }
        return null
    }

    /** Whether the [path] is a rectangle covering 100% area */
    private fun isFullRect(path: Path): Boolean? {
        if (path.isEmpty) return false
        // Path.isRect() produces a native exception on Android 8.0/8.1
        // (tested on Nexus 9 and Google emulators)
        if (OsUtils.dissatisfy(OsUtils.P)) return null
        if (!path.isRect(null)) return false
        val clip = Region(0, 0, 100, 100)
        val region = Region().apply { setPath(path, clip) }
        val iterator = RegionIterator(region)
        val rect = Rect()
        var area = 0
        while (iterator.next(rect)) {
            area += rect.width() * rect.height()
        }
        return area == 10000
    }
}
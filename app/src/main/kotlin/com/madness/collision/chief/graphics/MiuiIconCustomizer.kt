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

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import java.util.concurrent.atomic.AtomicBoolean

object MiuiIconCustomizer {
    fun getIconMaskPath(): Path? {
        try {
            val iconCustomizer = Class.forName("miui.content.res.IconCustomizer")
            val maskValue = iconCustomizer.getDeclaredMethod("getConfigIconMaskValue")
                .apply { isAccessible = true }.invoke(null)
            Log.d("AdaptiveIcon", "getMiuiIconMask/[$maskValue]")
            val pathData = maskValue as? String? ?: return null
            return PathParser().parsePathString(pathData).toPath().asAndroidPath()
        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private var inaccessibleMiuiMaskBitmap = AtomicBoolean(false)

    fun getIconMaskBitmap(): Bitmap? {
        if (inaccessibleMiuiMaskBitmap.get()) return null
        try {
            val iconMask = Class.forName("miui.content.res.IconCustomizer")
                .getDeclaredMethod("getRawIcon", String::class.java)
                .apply { isAccessible = true }.invoke(null, "icon_mask.png")
            if (iconMask != null && iconMask is Bitmap) {
                inaccessibleMiuiMaskBitmap.set(false)
                return iconMask
            }
        } catch (e: ReflectiveOperationException) {
            e.printStackTrace()
            inaccessibleMiuiMaskBitmap.set(true)
        } catch (e: Exception) {
            e.printStackTrace()
            when (e) {
                is SecurityException, is IllegalArgumentException ->
                    inaccessibleMiuiMaskBitmap.set(true)
            }
        }
        return null
    }

    private var inaccessibleMiuiStyledIcon = AtomicBoolean(false)

    fun generateStyledIcon(baseIcon: Drawable): BitmapDrawable? {
        if (inaccessibleMiuiStyledIcon.get()) return null
        try {
            val styledIcon = Class.forName("miui.content.res.IconCustomizer")
                .getDeclaredMethod("generateIconStyleDrawable", Drawable::class.java)
                .apply { isAccessible = true }.invoke(null, baseIcon)
            if (styledIcon != null && styledIcon is BitmapDrawable) {
                inaccessibleMiuiStyledIcon.set(false)
                return styledIcon
            }
        } catch (e: ReflectiveOperationException) {
            e.printStackTrace()
            inaccessibleMiuiStyledIcon.set(true)
        } catch (e: Exception) {
            e.printStackTrace()
            when (e) {
                is SecurityException, is IllegalArgumentException ->
                    inaccessibleMiuiStyledIcon.set(true)
            }
        }
        return null
    }
}
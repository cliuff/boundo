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
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import com.madness.collision.chief.chiefContext
import com.madness.collision.util.os.OsUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

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
            val gen = Class.forName("miui.content.res.IconCustomizer")
                .getDeclaredMethod("generateIconStyleDrawable", Drawable::class.java)
                .apply { isAccessible = true }
            // convert AdaptiveIconDrawables to work on HyperOS
            val nonAdaptiveIcon =
                if (OsUtils.satisfy(OsUtils.O) && baseIcon is AdaptiveIconDrawable) {
                    val res = chiefContext.resources
                    val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72f, res.displayMetrics)
                    BitmapDrawable(res, AdaptiveIconLoader.loadRectAdaptiveIcon(baseIcon, size.roundToInt()))
                } else {
                    baseIcon
                }
            val styledIcon = gen.invoke(null, nonAdaptiveIcon)
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
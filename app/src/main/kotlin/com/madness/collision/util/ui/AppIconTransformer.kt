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

package com.madness.collision.util.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import com.madness.collision.chief.graphics.AdaptiveIcon
import com.madness.collision.chief.os.EmuiDistro
import com.madness.collision.chief.os.HarmonyOsDistro
import com.madness.collision.chief.os.MiuiDistro
import com.madness.collision.chief.os.UndefDistro
import com.madness.collision.chief.os.distro
import me.zhanghai.android.appiconloader.iconloaderlib.ShadowGenerator
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

fun AppIconTransformer(): AppIconTransformer {
    val pathProvider = SmoothCornerPathProvider()
    return when (distro) {
        // confirmed issues on MIUI and HarmonyOS
        is MiuiDistro, is HarmonyOsDistro, is EmuiDistro -> RectAppIconTransformer(pathProvider)
        // unconditionally apply to other distros
        UndefDistro -> RectAppIconTransformer(pathProvider)
    }
}

interface AppIconTransformer {
    fun apply(icon: Drawable, scale: Float, size: Int, src: Bitmap): Bitmap
}

class EmptyAppIconTransformer : AppIconTransformer {
    override fun apply(icon: Drawable, scale: Float, size: Int, src: Bitmap) = src
}

/**
 * Fix rect app icon for AdaptiveIconDrawables on MIUI/HarmonyOS,
 * specifically system apps with default app icon, when custom icon pack is used (MIUI).
 * Note rect BitmapDrawables are N/A.
 */
class RectAppIconTransformer(private val pathProvider: AppIconPathProvider) : AppIconTransformer {
    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
    private val path = Path().apply {
        fillType = Path.FillType.INVERSE_EVEN_ODD
    }
    private val canvas = Canvas().apply {
        drawFilter = PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG)
    }

    override fun apply(icon: Drawable, scale: Float, size: Int, src: Bitmap): Bitmap {
        // apply to AdaptiveIconDrawables with rect mask only
        if (AdaptiveIcon.hasRectIconMask(icon).not()) return src
        // values from BaseIconFactory.createIconBitmap()
        val offset = max(
            ceil(ShadowGenerator.BLUR_FACTOR * size).toInt(),
            (size * (1 - scale) / 2).roundToInt())
        pathProvider.getPath(path, size, offset)
        val result = Bitmap.createBitmap(src)
        canvas.setBitmap(result)
        canvas.drawPath(path, paint)
        // reset
        canvas.setBitmap(null)
        path.reset()
        return result
    }
}

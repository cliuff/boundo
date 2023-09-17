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
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import com.madness.collision.BuildConfig
import com.madness.collision.chief.graphics.AdaptiveIcon
import com.madness.collision.chief.graphics.AdaptiveIconLoader
import com.madness.collision.chief.graphics.MiuiIconCustomizer
import com.madness.collision.chief.os.EmuiDistro
import com.madness.collision.chief.os.HarmonyOsDistro
import com.madness.collision.chief.os.MiuiDistro
import com.madness.collision.chief.os.UndefDistro
import com.madness.collision.chief.os.distro
import com.madness.collision.util.os.OsUtils
import me.zhanghai.android.appiconloader.iconloaderlib.ShadowGenerator
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

fun AppIconTransformer(): AppIconTransformer {
    // transformers target adaptive icons which require Android Oreo
    if (OsUtils.dissatisfy(OsUtils.O)) return EmptyAppIconTransformer()
    if (BuildConfig.DEBUG) getDebugTransformer()?.let { return it }
    return when (distro) {
        // confirmed issues on MIUI and HarmonyOS
        is MiuiDistro -> MiuiAppIconTransformer(SmoothCornerPathProvider())
        is HarmonyOsDistro, is EmuiDistro -> RectAppIconTransformer(SmoothCornerPathProvider())
        // unconditionally apply to other distros
        UndefDistro -> RectAppIconTransformer(SmoothCornerPathProvider())
    }
}

private var debugTransformerType = -1
@RequiresApi(Build.VERSION_CODES.O)
private fun getDebugTransformer() = when (debugTransformerType) {
    0 -> OverrideAppIconTransformer(SmoothCornerPathProvider(), AdaptiveIconLoader::loadRectAdaptiveIcon)
    1 -> OverrideAppIconTransformer(LocalMaskPathProvider(), AdaptiveIconLoader::loadMiuiAdaptiveIcon)
    2 -> OverrideAppIconTransformer(MiuiMaskPathProvider(), AdaptiveIconLoader::loadMiuiAdaptiveIcon)
    else -> null
}

interface AppIconTransformer {
    fun applySrc(icon: Drawable): Drawable = icon
    fun apply(icon: Drawable, scale: Float, size: Int, getBitmap: () -> Bitmap): Bitmap = getBitmap()

    fun calculateOffset(scale: Float, size: Int): Int {
        // values from BaseIconFactory.createIconBitmap()
        return max(
            ceil(ShadowGenerator.BLUR_FACTOR * size).toInt(),
            (size * (1 - scale) / 2).roundToInt())
    }
}

class EmptyAppIconTransformer : AppIconTransformer

class MiuiAppIconTransformer(fallbackPathProvider: AppIconPathProvider) : AppIconTransformer {
    private val fallback by lazy { RectAppIconTransformer(fallbackPathProvider) }
    // mapped icon to src icon
    private val srcIcons = ConcurrentHashMap<Drawable, Drawable>()

    override fun applySrc(icon: Drawable): Drawable {
        // apply to AdaptiveIconDrawables with rect mask only
        if (AdaptiveIcon.hasRectIconMask(icon).not()) return icon
        // map to the original icon to indicate fallback
        val mapped = MiuiIconCustomizer.generateStyledIcon(icon) ?: icon
        return mapped.also { srcIcons[it] = icon }
    }

    override fun apply(icon: Drawable, scale: Float, size: Int, getBitmap: () -> Bitmap): Bitmap {
        return if (srcIcons.remove(icon) == icon) fallback.apply(icon, scale, size, getBitmap) else getBitmap()
    }
}

/**
 * Fix rect app icon for AdaptiveIconDrawables on MIUI/HarmonyOS,
 * specifically system apps with default app icon, when custom icon pack is used (MIUI).
 * Note rect BitmapDrawables are N/A.
 */
class RectAppIconTransformer(private val pathProvider: AppIconPathProvider) : AppIconTransformer {
    private val insetBitmap = InsetBitmap()
    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
    private val maskPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val path = Path().apply {
        fillType = Path.FillType.INVERSE_EVEN_ODD
    }
    private val canvas = Canvas().apply {
        drawFilter = PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG)
    }

    override fun apply(icon: Drawable, scale: Float, size: Int, getBitmap: () -> Bitmap): Bitmap {
        // apply to AdaptiveIconDrawables with rect mask only
        if (AdaptiveIcon.hasRectIconMask(icon).not()) return getBitmap()
        val offset = calculateOffset(scale, size)
        return applyUnconditionally(icon, size, offset, when {
            // override getBitmap's masked src to draw a full icon to ensure better mask result
            OsUtils.satisfy(OsUtils.O) && icon is AdaptiveIconDrawable -> {
                val rectIcon = AdaptiveIconLoader.loadRectAdaptiveIcon(icon, size - 2 * offset)
                insetBitmap(size, offset) { rectIcon }
            }
            else -> getBitmap()
        })
    }

    fun applyUnconditionally(icon: Drawable, size: Int, offset: Int, src: Bitmap): Bitmap {
        val result = applyMask(size, offset, icon, src, pathProvider.getType())
        canvas.setBitmap(null)
        path.reset()
        return result
    }

    private fun applyMask(size: Int, offset: Int, icon: Drawable, src: Bitmap, type: AppIconPathProvider.Type): Bitmap {
        return when (type) {
            AppIconPathProvider.Type.MaskPath -> {
                pathProvider.getPath(path, size, offset)
                Bitmap.createBitmap(src).also { dest ->
                    canvas.setBitmap(dest)
                    canvas.drawPath(path, paint)
                }
            }
            AppIconPathProvider.Type.MaskBitmap ->
                when (val mask = pathProvider.getMaskBitmap(size, offset)) {
                    null -> applyMask(size, offset, icon, src, AppIconPathProvider.Type.MaskPath)
                    else -> Bitmap.createBitmap(src).also { dest ->
                        canvas.setBitmap(dest)
                        canvas.drawBitmap(mask, 0f, 0f, maskPaint)
                    }
                }
        }
    }
}

/** Override masked src and apply path unconditionally, for debugging */
@RequiresApi(Build.VERSION_CODES.O)
class OverrideAppIconTransformer(
    pathProvider: AppIconPathProvider,
    private val loadSrc: (icon: AdaptiveIconDrawable, size: Int) -> Bitmap,
) : AppIconTransformer {
    private val delegate = RectAppIconTransformer(pathProvider)
    private val insetBitmap = InsetBitmap()
    override fun apply(icon: Drawable, scale: Float, size: Int, getBitmap: () -> Bitmap): Bitmap {
        if (OsUtils.satisfy(OsUtils.O) && icon is AdaptiveIconDrawable) {
            // override masked src with unmasked one to apply path to all adaptive icons
            val offset = calculateOffset(scale, size)
            val bitmap = insetBitmap(size, offset) { loadSrc(icon, size - 2 * offset) }
            return delegate.applyUnconditionally(icon, size, offset, bitmap)
        }
        return getBitmap()
    }
}

class InsetBitmap {
    val paint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
    val canvas = Canvas().apply {
        drawFilter = PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG)
    }

    /** Inset [src] by offset in target [size] bitmap */
    inline operator fun invoke(size: Int, offset: Int, src: () -> Bitmap): Bitmap {
        if (offset <= 0) return src()
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            canvas.setBitmap(bitmap)
            canvas.drawBitmap(src(), offset.toFloat(), offset.toFloat(), paint)
            canvas.setBitmap(null)
        }
    }
}

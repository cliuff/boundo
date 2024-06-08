/*
 * Copyright 2022 Clifford Liu
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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.madness.collision.R
import com.madness.collision.util.os.OsUtils
import java.lang.Math.cbrt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object GraphicsUtil {

    /**
     * Scale the src if any of its dimensions is larger than specified
     */
    fun fit2(src: Bitmap, width: Int, height: Int): Bitmap{
        val oW = src.width
        val oH = src.height
        val image: Bitmap
        image = if (oW > width || oH > height){
            if (oW * height / oH >= width) Bitmap.createScaledBitmap(src, width, oH * width / oW, true)
            else Bitmap.createScaledBitmap(src, oW * height / oH, height, true)
        }else src
        if (oW == width && oH == height) return image
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val top = (width - oW).toFloat() / 2
        val left = (height - oH).toFloat() / 2
        val canvas = Canvas(result)
        canvas.drawBitmap(image, left, top, Paint(Paint.ANTI_ALIAS_FLAG))
        return result
    }

    fun scale(image: Bitmap, degree: Float): Bitmap {
        val degreeInt = degree.roundToInt()
        val destWidth = image.width * degreeInt
        val destHeight = image.height * degreeInt
        return Bitmap.createScaledBitmap(image, destWidth, destHeight, true)
    }

    /**
     * clip the src image to specified width and height
     * width and height must not be smaller than that of the src
     */
    fun clip2(src: Bitmap, width: Int, height: Int): Bitmap{
        val oWidth = src.width
        val oHeight = src.height
        if (oWidth == width && oHeight == height) return src
        require(!(oWidth < width || oHeight < height)) { "width and height must not be greater than the original's" }
        return Bitmap.createBitmap(src, (oWidth - width) / 2, (oHeight - height) / 2, width, height)
    }

    fun clipDown2( src: Bitmap, denominator: Int): Bitmap{
        return clip2(src, src.width / denominator, src.height / denominator)
    }

    fun clipDown2( src: Bitmap, denominator4Width: Int, denominator4Height: Int): Bitmap{
        return clip2(src, src.width / denominator4Width, src.height / denominator4Height)
    }

    fun drawOn( context: Context, res: Int,  bitmap: Bitmap?): Bitmap{
        val drawable = ContextCompat.getDrawable(context, res)!!.mutate()
        val re: Bitmap = bitmap ?: Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        return drawOn(drawable, re)
    }

    fun drawOn( context: Context, res: Int, width: Int, height: Int): Bitmap{
        val drawable = ContextCompat.getDrawable(context, res)!!.mutate()
        return drawOn(drawable, width, height)
    }

    fun drawOn(drawable: Drawable, width: Int, height: Int): Bitmap{
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return drawOn(drawable, bitmap)
    }

    fun drawOn(drawable: Drawable, bitmap: Bitmap): Bitmap{
        //drawable draws nothing on canvas if the bounds are not set
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    /** Set bounds with respect to the original ratio */
    fun resolveBounds(drawable: Drawable, width: Int, height: Int) {
        val ow = drawable.intrinsicWidth
        val oh = drawable.intrinsicHeight
        val lenMax = max(ow, oh)
        val drawW = width * ow / lenMax
        val drawH = height * oh / lenMax
        drawable.setBounds((width - drawW) / 2, (height - drawH) / 2, drawW, drawH)
    }

    fun drawAs(origin: Drawable, width: Int, height: Int): Bitmap{
        val re = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val backup = origin.bounds
        resolveBounds(origin, width, height)
        origin.draw(Canvas(re))
        origin.bounds = backup
        return re
    }

    /**
     * see if the icon should be stroked
     */
    fun shouldStroke(context: Context, src: Bitmap, flavor: Int): Boolean{
        val bitmap = Bitmap.createBitmap(src)
        val width = bitmap.width
        val height = bitmap.height
        // lighten bitmap
        val lightenPaint = Paint(Color.WHITE)
        lightenPaint.colorFilter = LightingColorFilter(0xF2F2F2, 0x0D0D0D)
        Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, lightenPaint)
        // clear center pixels
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.TRANSPARENT
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        val radius = width / 2f
        val radiusInner = radius * 0.96f
        val offset = radius - radiusInner
        val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
        when (flavor) {
            AI_FLAVOR_ROUND -> path.addCircle(radius, radius, radiusInner, Path.Direction.CW)
            AI_FLAVOR_ROUNDED -> {
                val cornerRadius = X.size(context, 12.6f, X.DP)
                val end = radiusInner * 2 + offset
                path.addRoundRect(offset, offset, end, end, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            AI_FLAVOR_SQUIRCLE -> path.set(getSquirclePath(offset, offset, radiusInner))
        }
        val canvas = Canvas(bitmap)
        canvas.drawPath(path, paint)

        val backgroundColor = ThemeUtil.getColor(context, R.attr.colorABackground)
        val blockWidth = width / 2
        val blockHeight = height / 2
        var blockCount = 0
        arrayOf(
                Bitmap.createBitmap(bitmap, 0, 0, blockWidth, blockHeight),
                Bitmap.createBitmap(bitmap, blockWidth, 0, blockWidth, blockHeight),
                Bitmap.createBitmap(bitmap, 0, blockHeight, blockWidth, blockHeight),
                Bitmap.createBitmap(bitmap, blockWidth, blockHeight, blockWidth, blockHeight)
        ).forEach { block ->
            val dominantColor = Palette.from(block).generate().getDominantColor(Color.WHITE).let {
                ColorUtils.blendARGB(Color.BLACK, it, 0.95f)
            }
            val contrast = ColorUtils.calculateContrast(dominantColor, backgroundColor)
            if (contrast < 1.375) blockCount++
        }
        return blockCount > 1
    }

    const val AI_FLAVOR_ROUND = 1
    const val AI_FLAVOR_ROUNDED = 2
    const val AI_FLAVOR_SQUIRCLE = 3
    @RequiresApi(api = X.O)
    fun drawAI(context: Context, drawable: Drawable, flavor: Int, allowStroke: Boolean = true, strokeOnlyWhenShould: Boolean = true, oneSixthDiameter: Int = 0): Bitmap {
        val ai = drawable as AdaptiveIconDrawable
        var back = ai.background
        if (back != null) back = back.mutate()
        var fore = ai.foreground
        if (fore != null) fore = fore.mutate()

        val stroke = if (allowStroke) {
            if (strokeOnlyWhenShould) {
                val oneSixthD = X.size(context, 3.5f, X.DP).roundToInt()
                val bitmap = drawAI(context, drawable, flavor, allowStroke = false, oneSixthDiameter = oneSixthD)
                shouldStroke(context, bitmap, flavor)
            } else true
        } else false

        val strokeWidthInt = if (stroke) X.size(context, 1f, X.DP).roundToInt() else 0
        val strokeWidth = if (stroke) strokeWidthInt.toFloat() else 0f
        // standard icon size: 108dp * 108dp
        val unitOneSixth = if (oneSixthDiameter <= 0) X.size(context, 18f, X.DP).toInt() else oneSixthDiameter
        val unitOneSixthFloat = unitOneSixth.toFloat()
        val targetRadius = unitOneSixth * 3
        val targetRadiusFloat = targetRadius.toFloat()
        val targetDiameter = targetRadius * 2
        val displayRadius = unitOneSixth * 2
        val displayRadiusFloat = displayRadius.toFloat()
        val displayDiameter = displayRadius * 2
        val displayDiameterFloat = displayDiameter.toFloat()

        val resultWidth = if (stroke) (displayDiameter + (2 * strokeWidthInt)) else displayDiameter
        val result = Bitmap.createBitmap(resultWidth, resultWidth, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val translateWidth = if (stroke) (unitOneSixthFloat - strokeWidth) else unitOneSixthFloat
        canvas.translate(-translateWidth, -translateWidth)

        val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
        when (flavor) {
            AI_FLAVOR_ROUND -> path.addCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, Path.Direction.CW)
            AI_FLAVOR_ROUNDED -> {
                val radius = X.size(context, 12.6f, X.DP)
                val end = displayDiameterFloat + unitOneSixthFloat
                path.addRoundRect(unitOneSixthFloat, unitOneSixthFloat, end, end, radius, radius, Path.Direction.CW)
            }
            AI_FLAVOR_SQUIRCLE -> path.set(getSquirclePath(unitOneSixthFloat, unitOneSixthFloat, displayRadiusFloat))
            else -> {
                val end = displayDiameterFloat + unitOneSixthFloat
                path.addRect(unitOneSixthFloat, unitOneSixthFloat, end, end, Path.Direction.CW)
            }
        }
        // below: draw background
        if (back != null) {
            val shaderBack = drawAs(back, targetDiameter, targetDiameter)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = BitmapShader(shaderBack, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            canvas.drawPath(path, paint)
        }
        // below: draw foreground
        if (fore != null) {
            val shaderFore = drawAs(fore, targetDiameter, targetDiameter)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = BitmapShader(shaderFore, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            canvas.drawPath(path, paint)
        }

        // stroke
        if (stroke) {
            val pathStroke = Path().apply { fillType = Path.FillType.EVEN_ODD }
            when (flavor) {
                AI_FLAVOR_ROUND -> pathStroke.addCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, Path.Direction.CW)
                AI_FLAVOR_ROUNDED -> {
                    val radius = X.size(context, 12.6f, X.DP)
                    val end = displayDiameterFloat + unitOneSixthFloat
                    pathStroke.addRoundRect(unitOneSixthFloat, unitOneSixthFloat, end, end, radius, radius, Path.Direction.CW)
                }
                AI_FLAVOR_SQUIRCLE -> pathStroke.set(getSquirclePath(unitOneSixthFloat, unitOneSixthFloat, displayRadiusFloat))
                else -> {
                    val end = displayDiameterFloat + unitOneSixthFloat
                    path.addRect(unitOneSixthFloat, unitOneSixthFloat, end, end, Path.Direction.CW)
                }
            }
            val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG)
            paintStroke.color = ThemeUtil.getColor(context, R.attr.colorStroke)
            paintStroke.strokeWidth = strokeWidth
            paintStroke.style = Paint.Style.STROKE
            canvas.drawPath(pathStroke, paintStroke)
        }

        return result
    }

    /**
     * @param left left translation on canvas
     * @param top top translation on canvas
     */
    fun getSquirclePath(left: Float, top: Float, radius: Float): Path {
        // formula: (|x|)^3 + (|y|)^3 = radius^3
        val radiusToPow = (radius * radius * radius).toDouble()

        val path = Path()
        path.moveTo(-radius, 0f)
        val radiusInt = radius.toInt()
        for (x in -radiusInt..radiusInt)
            path.lineTo(x.toFloat(), cbrt(radiusToPow - abs(x * x * x)).toFloat())
        for (x in radiusInt downTo -radiusInt)
            path.lineTo(x.toFloat(), -cbrt(radiusToPow - abs(x * x * x)).toFloat())
        path.close()

        val matrix = Matrix()
        matrix.postTranslate(left + radius, top + radius)
        path.transform(matrix)

        return path
    }

    /**
     * normal drawable that is not AdaptiveIconDrawable
     * @param drawable drawable res
     * @return bitmap
     */
    fun convertDrawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return toMutable(drawable.bitmap)

        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight
        // Single color bitmap will be created of 1x1 pixel,
        // update: it might as well be gradient drawable apart from single color drawable
        val bitmap: Bitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0)
            Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        else
            Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun toMutable(bitmap: Bitmap): Bitmap {
        val isImmutable = !bitmap.isMutable ||
                OsUtils.satisfy(OsUtils.O) && bitmap.config == Bitmap.Config.HARDWARE
        return if (isImmutable) bitmap.copy(Bitmap.Config.ARGB_8888, true) else bitmap
    }
}

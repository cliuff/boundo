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

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.madness.collision.R
import com.madness.collision.util.GraphicsUtil
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import com.madness.collision.util.os.OsUtils
import kotlin.math.max
import kotlin.math.roundToInt

internal object AppIconProcessor {

    fun retrieveAppIcon(
        context: Context,
        isDefined: Boolean,
        iconDrawable: Drawable,
        iconDetails: IconRetrievingDetails
    ): Bitmap? {
        var logoDrawable = iconDrawable
        if (isDefined) {
            if (iconDetails.isDefault) {
                logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_android_24) ?: return null
            }
        } else {
            iconDetails.width = logoDrawable.intrinsicWidth
            iconDetails.height = logoDrawable.intrinsicHeight
            if (iconDetails.width <= 0 || iconDetails.height <= 0) {
                iconDetails.isDefault = true
                logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_android_24) ?: return null
                iconDetails.width = logoDrawable.intrinsicWidth
                iconDetails.height = logoDrawable.intrinsicHeight
            }

            // below: shrink size if it's too large in case of consuming too much memory
            iconDetails.standardWidth = X.size(context, 72f, X.DP).roundToInt()
            val maxLength = max(iconDetails.width, iconDetails.height)
            if (maxLength > iconDetails.standardWidth){
                val fraction: Float = iconDetails.standardWidth.toFloat() / maxLength
                iconDetails.width = (iconDetails.width * fraction).roundToInt()
                iconDetails.height = (iconDetails.height * fraction).roundToInt()
            }
        }

        if (iconDetails.width <= 0 || iconDetails.height <= 0) return null
        var logo = Bitmap.createBitmap(iconDetails.width, iconDetails.height, Bitmap.Config.ARGB_8888)
        logoDrawable.setBounds(0, 0, iconDetails.width, iconDetails.height)
        logoDrawable.draw(Canvas(logo))
        // make it square and properly centered
        logo = GraphicsUtil.properly2Square(logo)
        return X.toTarget(logo, iconDetails.standardWidth, iconDetails.standardWidth)
    }

    private fun determineIconClip(context: Context, icon2Clip: Bitmap, iconDetails: IconRetrievingDetails, alphaLimit: Int) {
        val radius = X.size(context, 10f, X.DP).roundToInt()
        val radiusFloat = radius.toFloat()
        val diameter = 2 * radius

        val sample = X.toTarget(icon2Clip, diameter, diameter)
        // exam whether to trim it
        val exam = Bitmap.createBitmap(sample)
        val canvasExam = Canvas(exam)
        val paintExam = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = Color.BLACK
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        val pathExam = Path().apply { fillType = Path.FillType.INVERSE_EVEN_ODD }
        pathExam.addCircle(radiusFloat, radiusFloat, radiusFloat, Path.Direction.CW)
        canvasExam.drawPath(pathExam, paintExam)
        val pixels = IntArray(exam.width * exam.height)
        exam.getPixels(pixels, 0, exam.width, 0, 0, exam.width, exam.height)
        val transparentPixels = pixels.filter { (it ushr 24 and 0xFF) < alphaLimit }
        val transparentSize = transparentPixels.size.toFloat()
        val total = pixels.size.toFloat()
        val ratio = transparentSize / total
        iconDetails.shouldClip = ratio < 0.015

        if (iconDetails.shouldClip) {
            paintExam.shader = BitmapShader(sample, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            pathExam.fillType = Path.FillType.EVEN_ODD
            val roundSample = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
            Canvas(roundSample).drawPath(pathExam, paintExam)
            iconDetails.shouldStroke = GraphicsUtil.shouldStroke(context, roundSample, GraphicsUtil.AI_FLAVOR_ROUND)
        }
    }

    /**
     * make icon round
     */
    fun roundIcon(
        context: Context,
        isDefined: Boolean,
        logo: Bitmap,
        roundIconDrawable: Drawable?,
        iconDetails: IconRetrievingDetails
    ): Bitmap {
        val doPackageIcon = roundIconDrawable != null
        // below: convert into round
        // the higher the harder to be recognized as round icon
        val alphaLimit = 0x90
        val icon2Clip by lazy {
            GraphicsUtil.removeOuterTransparentPixels(logo, noTransparency = false, alphaLimit = alphaLimit).let {
                GraphicsUtil.properly2Square(it)
            }
        }
        if (!isDefined && !doPackageIcon && EasyAccess.shouldClip2Round) {
            determineIconClip(context, icon2Clip, iconDetails, alphaLimit)
        }

        if (!isDefined && !doPackageIcon && !iconDetails.shouldClip) iconDetails.shouldStroke = true
        val strokeWidth = if (iconDetails.shouldStroke) X.size(context, 1f, X.DP).roundToInt() else 0
        val strokeWidthFloat = if (iconDetails.shouldStroke) strokeWidth.toFloat() else 0f

        // below: get tools ready to draw
        val fraction = 0.6f
        // standard icon size: 48dp * 48dp
        val radiusStandard = X.size(context, 36 / fraction, X.DP).roundToInt()
        // the radius visible to be seen
        val displayRadius = if (iconDetails.shouldStroke) radiusStandard + strokeWidth else radiusStandard
        val displayRadiusFloat = displayRadius.toFloat()
        val displayDiameter = 2 * displayRadius
        // the true radius of image
        val targetRadius = displayRadius + strokeWidth
        val targetRadiusFloat = targetRadius.toFloat()
        val targetDiameter = 2 * targetRadius

        val logoCircular = Bitmap.createBitmap(targetDiameter, targetDiameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(logoCircular!!)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorStroke = if (iconDetails.shouldStroke) ThemeUtil.getColor(context, R.attr.colorStroke) else 0

        // below: process round icon from package
        if (doPackageIcon) {
            var logo2Draw = iconDrawable2Bitmap(context, roundIconDrawable!!)
            // below: ensure it is round
            logo2Draw = X.toTarget(logo2Draw, displayDiameter, displayDiameter)
            paint.shader = BitmapShader(logo2Draw, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paint)
            if (!iconDetails.shouldStroke) return logoCircular
        }

        if (iconDetails.shouldClip){
            paint.shader = BitmapShader(
                X.toTarget(icon2Clip, displayDiameter, displayDiameter),
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paint)
            if (!iconDetails.shouldStroke) return logoCircular
        }

        // below: draw icon on top of a white background
        if (!doPackageIcon && !iconDetails.shouldClip){
            // below: draw background
            paint.color = ThemeUtil.getColor(context, R.attr.colorApiLegacyBack)
            canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paint)

            // below: draw shrunk app icon
            val shrunkDiameter = (displayDiameter * fraction).toInt()
            val shrunkLogo = X.toTarget(logo, shrunkDiameter, shrunkDiameter)
            val offset = targetRadiusFloat * (1 - fraction)
            canvas.drawBitmap(shrunkLogo, offset, offset, Paint(Paint.ANTI_ALIAS_FLAG))
        }

        // stroke
        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = colorStroke
            this.strokeWidth = strokeWidthFloat
            this.style = Paint.Style.STROKE
        }
        canvas.drawCircle(targetRadiusFloat, targetRadiusFloat, displayRadiusFloat, paintStroke)
        return logoCircular
    }

    private fun iconDrawable2Bitmap(context: Context, drawable: Drawable): Bitmap {
        if (OsUtils.satisfy(OsUtils.O) && drawable is AdaptiveIconDrawable) {
            return GraphicsUtil.drawAIRound(context, drawable)
        }
        return X.drawableToBitmap(drawable)
    }

}

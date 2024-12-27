/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.unit.themed_wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.madness.collision.util.BackgroundUtil
import com.madness.collision.util.GaussianBlur
import com.madness.collision.util.X
import kotlin.math.roundToInt

internal class ThemedWallpaper(private var wallpaper: Drawable) {

    companion object {
        private const val KEY_INT_ARRAY = "intArray"
        private const val KEY_FLOAT = "float"
        private const val FPS: Int = 60
        private const val DURATION: Float = 0.5f // seconds
        const val FRAME_GAP: Long = (1000 / FPS).toLong()
        const val PROGRESS_INCREMENT: Float = 1 / (DURATION * FPS)
    }

    /**
     * Desired width to draw
     */
    private var drawWidth: Int = 0
    /**
     * Desired height to draw
     */
    private var drawHeight: Int = 0
    /**
     * Width of drawable after scale
     */
    private var adaptedWidth = 0
    /**
     * Height of drawable after scale
     */
    private var adaptedHeight = 0
    private var extraWidth = 0
    private var extraHeight = 0
    private var offsetVer = 0f
    private var savedAdaptationPrevious: Bundle? = null
    private var savedAdaptation: Bundle? = null
    private var lastOffsetRatio: Float = 0f
    private var lastOffsetStepRatio: Float = 0f

    private var previousWallpaperBitmap4Motion: Bitmap? = null
    private var previousWallpaper: Drawable? = null
    private var gaussianBlur: GaussianBlur? = null
    private var wallpaperBitmap4Motion: Bitmap? = null
    private var motionFrame: Drawable? = null
    var translateProgress = 1f
        private set
    val isTranslating: Boolean
        get() = translateProgress.let { it >= 0f && it < 1f }
    val isTranslateCompleted: Boolean
        get() = translateProgress == 1f
    val isTranslateStarting: Boolean
        get() = translateProgress == 0f

    fun startTranslate() {
        translateProgress = if (previousWallpaper == null) 1f else 0f
    }

    fun completeTranslate() {
        translateProgress = 1f
    }

    fun updateTranslateProgress() {
        translateProgress += PROGRESS_INCREMENT
        if (translateProgress > 1f) completeTranslate()
    }

    /**
     * Scale drawable in order to fit the desired drawing size
     */
    private fun adapt(drawable: Drawable, width: Int, height: Int){
        val bounds = BackgroundUtil.adjustBounds(drawable, width, height)
        adaptedWidth = bounds.width()
        adaptedHeight = bounds.height()
        extraWidth = adaptedWidth - drawWidth
        extraHeight = adaptedHeight - drawHeight
        offsetVer = extraHeight / 2f
        savedAdaptation = saveAdaptation()
    }

    private fun adapt(width: Int, height: Int){
        adapt(wallpaper, width, height)
    }

    private fun saveAdaptation(): Bundle {
        val aInt = intArrayOf(adaptedWidth, adaptedHeight, extraWidth, extraHeight)
        val aFloat = offsetVer
        return Bundle().apply {
            putIntArray(KEY_INT_ARRAY, aInt)
            putFloat(KEY_FLOAT, aFloat)
        }
    }

    private fun restoreAdaptation(bundle: Bundle?) {
        val aInt = bundle?.getIntArray(KEY_INT_ARRAY) ?: return
        val aFloat = bundle.getFloat(KEY_FLOAT)
        adaptedWidth = aInt[0]
        adaptedHeight = aInt[1]
        extraWidth = aInt[2]
        extraHeight = aInt[3]
        offsetVer = aFloat
    }

    fun setSize(width: Int, height: Int){
        drawWidth = width
        drawHeight = height
        adapt(width, height)
    }

    fun updateWallpaper(wallpaper: Drawable){
        savedAdaptationPrevious = savedAdaptation
        previousWallpaper = if (this.wallpaper === wallpaper) null else this.wallpaper
        this.wallpaper = wallpaper
        adapt(drawWidth, drawHeight)
    }

    fun translate(canvas: Canvas, offsetRatio: Float, offsetStepRatio: Float) {
        translate(motionFrame ?: wallpaper, canvas, offsetRatio, offsetStepRatio)
    }

    private fun translate(drawable: Drawable, canvas: Canvas, offsetRatio: Float, offsetStepRatio: Float) {
        lastOffsetRatio = offsetRatio
        lastOffsetStepRatio = offsetStepRatio
        val offsetHor: Float = when {
            extraWidth == 0 -> 0f
            // center the wallpaper when there's only one screen
            offsetStepRatio <= 0f || offsetStepRatio > 1f -> 0.5f * extraWidth
            // center the wallpaper and map the offset ratio from [0,1] to [0.5,1]
            else -> (0.5f + offsetRatio * 0.5f).coerceIn(0.5f, 1f) * extraWidth
        }
        canvas.translate(-offsetHor, -offsetVer)
        drawable.draw(canvas)
    }

    fun makeMotion(context: Context) {
        val motionBitmap: Bitmap
        if (gaussianBlur == null) gaussianBlur = GaussianBlur(context)
        val radius: Float  //range from 0 to 20, the larger the blurrier
        when (translateProgress) {
            in 0f..0.333f -> {
                if (previousWallpaperBitmap4Motion == null) {
                    previousWallpaperBitmap4Motion = Bitmap.createBitmap(drawWidth, drawHeight, Bitmap.Config.ARGB_8888)
                    val realDrawable = previousWallpaper ?: wallpaper
                    restoreAdaptation(savedAdaptationPrevious)
                    translate(realDrawable, Canvas(previousWallpaperBitmap4Motion!!), lastOffsetRatio, lastOffsetStepRatio)
                    previousWallpaperBitmap4Motion = previousWallpaperBitmap4Motion!!.run {
                        val scale2 = 50
                        X.toMin(this, scale2)
                    }
                }
                motionBitmap = previousWallpaperBitmap4Motion!!
                radius = 60 * translateProgress
            }
            in 0.333f..0.666f -> {
                if (wallpaperBitmap4Motion == null) {
                    wallpaperBitmap4Motion = Bitmap.createBitmap(drawWidth, drawHeight, Bitmap.Config.ARGB_8888)
                    restoreAdaptation(savedAdaptation)
                    translate(wallpaper, Canvas(wallpaperBitmap4Motion!!), lastOffsetRatio, lastOffsetStepRatio)
                    wallpaperBitmap4Motion = wallpaperBitmap4Motion!!.run {
                        val scale2 = 50 //scale to this size in advance, the smaller the blurrier, is 100 in exterior activity
                        X.toMin(this, scale2)
                    }
                }
                val pre = previousWallpaperBitmap4Motion!!
                val new = wallpaperBitmap4Motion!!
                val motionWidth = new.width
                val motionHeight = new.height
                motionBitmap = Bitmap.createBitmap(motionWidth, motionHeight, Bitmap.Config.ARGB_8888)
                val ratio = (translateProgress - 0.333f) * 3
                // getPixel: IllegalArgumentException: x must be < bitmap.width()
                try {
                    for (i in 0 until motionHeight)
                        for (j in 0 until motionWidth) {
                            val cPre = pre[j, i].let { it or (((it ushr 24 and 0xFF) * (1 - ratio)).roundToInt() shl 24) }
                            val cNew = new[j, i].let { it or (((it ushr 24 and 0xFF) * ratio).roundToInt() shl 24) }
                            motionBitmap[j, i] = ColorUtils.blendARGB(cPre, cNew, ratio)
                        }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
                radius = 20f
            }
            in 0.666f..1f -> {
                motionBitmap = wallpaperBitmap4Motion!!
                radius = 60 * (1 - translateProgress)
            }
            else -> {
                motionBitmap = wallpaperBitmap4Motion!!
                radius = 0f
            }
        }
        val blurred = gaussianBlur!!.blur(motionBitmap, radius)
        val frameBitmap = X.toTarget(blurred, motionBitmap.width, motionBitmap.height)
        motionFrame = BitmapDrawable(context.resources, frameBitmap)
        motionFrame!!.setBounds(0, 0, drawWidth, drawHeight)
        extraWidth = 0
        extraHeight = 0
        offsetVer = 0f
    }

    fun destroyMotion() {
        restoreAdaptation(savedAdaptation)
        motionFrame = null
        wallpaperBitmap4Motion = null
        previousWallpaperBitmap4Motion = null
        previousWallpaper = null
        gaussianBlur?.destroy()
        gaussianBlur = null
    }
}

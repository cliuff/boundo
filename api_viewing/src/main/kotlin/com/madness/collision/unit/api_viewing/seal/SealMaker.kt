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

package com.madness.collision.unit.api_viewing.seal

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import com.madness.collision.unit.api_viewing.R as MyR

typealias SealManager = SealMaker

object SealMaker {

    fun getAndroidCodenameImageRes(letter: Char): Int {
        if (!EasyAccess.isSweet) return 0
        return when (letter) {
            't' -> MyR.drawable.seal_t
            's' -> MyR.drawable.seal_s
            'r' -> MyR.drawable.seal_r_vector
            'q' -> MyR.drawable.seal_q_vector
            'p' -> MyR.drawable.seal_p_vector
            'o' -> MyR.drawable.seal_o
            'n' -> MyR.drawable.seal_n
            'm' -> MyR.drawable.seal_m_vector
            'l' -> MyR.drawable.seal_l_vector
            'k' -> MyR.drawable.seal_k_vector
            'j' -> MyR.drawable.seal_j_vector
            'i' -> MyR.drawable.seal_i
            'h' -> MyR.drawable.seal_h_vector
            else -> 0
        }
    }

    fun getItemColorBack(context: Context, apiLevel: Int): Int {
        return itemColorInfo(context, apiLevel, false)
    }

    fun getItemColorAccent(context: Context, apiLevel: Int): Int {
        return itemColorInfo(context, apiLevel, true)
    }

    fun getItemColorForIllustration(context: Context, apiLevel: Int): Int {
        return itemColorInfo(context, apiLevel, isAccent = true, isForIllustration = true)
    }

    fun getItemColorText(apiLevel: Int) = when (apiLevel) {
        X.M -> Color.BLACK
        else -> Color.WHITE
    }

    private fun itemColorInfo(context: Context, apiLevel: Int, isAccent: Boolean, isForIllustration: Boolean = false): Int {
        if (!EasyAccess.shouldShowDesserts && !isForIllustration) {
            val attrRes = if (isAccent) android.R.attr.textColor else R.attr.colorASurface
            return ThemeUtil.getColor(context, attrRes)
        }
        when (apiLevel) {
            OsUtils.T -> if (isAccent) "a3d5c1" else "d7fbf0"
            OsUtils.S, OsUtils.S_V2 -> if (isAccent) "acdcb2" else "defbde"
            X.R -> if (isAccent) "acd5c1" else "defbf0"
            X.Q -> if (isAccent) "c1d5ac" else "f0fbde"
            X.P -> if (isAccent) "e0c8b0" else "fff6d5"
            X.O, X.O_MR1 -> if (isAccent) "b0b0b0" else "eeeeee"
            X.N, X.N_MR1 -> if (isAccent) "ffb2a8" else "ffecf6"
            X.M -> if (isAccent) "b0c9c5" else "e0f3f0"
            X.L, X.L_MR1 -> if (isAccent) "ffb0b0" else "ffeeee"
            X.K, X.K_WATCH -> if (isAccent) "d0c7ba" else "fff3e0"
            X.J, X.J_MR1, X.J_MR2 -> if (isAccent) "baf5ba" else "eeffee"
            X.I, X.I_MR1 -> if (isAccent) "d8d0c0" else "f0f0f0"
            X.H, X.H_MR1, X.H_MR2 -> if (isAccent) "f0c8b4" else "fff5f0"
            else -> if (isAccent) "c5e8b0" else X.getColorHex(context, R.color.androidRobotGreenBack)
        }.run {
            val color = Color.parseColor("#$this")
            if (isAccent && !isForIllustration) return color
            return when {
                mainApplication.isPaleTheme -> color
                mainApplication.isDarkTheme -> ColorUtil.darkenAs(color, if (isForIllustration) 0.7f else 0.15f)
                else -> ColorUtil.darkenAs(color, if (isForIllustration) 0.9f else 0.55f)
            }
        }
    }

    private fun readImage(file: File): Bitmap? {
        try {
            return ImageUtil.getBitmap(file)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getSealPath(index: Char): String {
        val seal = F.valCachePubAvSeal(appContext)
        return F.createPath(seal, "seal-$index.png")
    }

    fun getSealCacheFile(index: Char): File? {
        return File(getSealPath(index)).takeIf { it.exists() }
    }

    private val sealMutex = Mutex()

    suspend fun getSealFile(context: Context, index: Char, itemLength: Int): File? {
        return withContext(Dispatchers.IO) {
            // avoid making seal concurrently
            sealMutex.withLock { makeSeal(context, index, itemLength) }
            getSealCacheFile(index)
        }
    }

    // get storage cache or make one
    private fun getSealBitmap(context: Context, index: Char, itemLength: Int): Bitmap? {
        // return storage cache
        getSealCacheFile(index)?.let { return readImage(it) }
        // make storage cache
        return makeSealCache(context, index, itemLength)
    }

    /**
     * @return [Unit] for success, null for failure
     */
    fun makeSeal(context: Context, index: Char, itemLength: Int): Unit? {
        getSealCacheFile(index)?.let { return Unit }
        makeSealCache(context, index, itemLength) ?: return null
        return Unit
    }

    private fun makeSealCache(context: Context, index: Char, itemLength: Int): Bitmap? {
        val res = getAndroidCodenameImageRes(index)
        if (res == 0) return null
        val drawable: Drawable
        try {
            drawable = ContextCompat.getDrawable(context, res) ?: return null
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            return null
        }
        val bitmap = Bitmap.createBitmap(itemLength, itemLength, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, itemLength, itemLength)
        drawable.draw(Canvas(bitmap))
        val path = getSealPath(index)
        if (F.prepare4(path)) X.savePNG(bitmap, path)
        return bitmap
    }

    @JvmInline
    private value class BlurredIndex(val value: Char)

    private fun mapToBlurredIndex(index: Char): BlurredIndex {
        val value = when {
            !EasyAccess.isSweet -> '?'
            getAndroidCodenameImageRes(index) != 0 -> index
            else -> '?'
        }
        return BlurredIndex(value)
    }

    private fun getBlurredPath(index: BlurredIndex): String {
        val seal = F.valCachePubAvSeal(appContext)
        return F.createPath(seal, "back-${index.value}.png")
    }

    private fun getBlurredCacheFile(index: BlurredIndex): File? {
        return File(getBlurredPath(index)).takeIf { it.exists() }
    }

    fun getBlurredCacheFile(index: Char): File? {
        return getBlurredCacheFile(mapToBlurredIndex(index))
    }

    private val blurredMutex = Mutex()

    suspend fun getBlurredFile(context: Context, index: Char, itemLength: Int): File? {
        return withContext(Dispatchers.IO) {
            // avoid making blurred seal concurrently
            blurredMutex.withLock { makeBlurred(context, index, itemLength) }
            getBlurredCacheFile(index)
        }
    }

    /**
     * @return [Unit] for success, null for failure
     */
    fun makeBlurred(context: Context, index: Char, itemLength: Int): Unit? {
        val bIndex = mapToBlurredIndex(index)
        getBlurredCacheFile(bIndex)?.let { return Unit }
        makeBlurredCache(context, bIndex, itemLength) ?: return null
        return Unit
    }

    private fun makeBlurredCache(context: Context, index: BlurredIndex, itemLength: Int): Bitmap? {
        val bitmap = if (getAndroidCodenameImageRes(index.value) == 0) {
            drawBlurredColor(context, index)
        } else {
            val seal = getSealBitmap(context, index.value, itemLength) ?: return null
            drawBlurredImage(context, seal)
        }
        val path = getBlurredPath(index)
        if (F.prepare4(path)) X.savePNG(bitmap, path)
        return bitmap
    }

    private fun drawBlurredColor(context: Context, index: BlurredIndex): Bitmap {
        val color = when (index.value) {
            'k' -> Color.parseColor("#753500")
            else -> X.getColor(context, R.color.androidRobotGreen)
        }
        val blurWidth = X.size(context, 60f, X.DP).toInt() * 2
        val bitmap = Bitmap.createBitmap(blurWidth, blurWidth, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        return bitmap
    }

    private fun drawBlurredImage(context: Context, sealBitmap: Bitmap): Bitmap {
        var seal: Bitmap = sealBitmap.collisionBitmap
        val targetLength = seal.width / 10
        val bitmapWidth = targetLength * 2
        val sealOffsetX = targetLength * 4
        val sealOffsetY = targetLength * 2
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapWidth, Bitmap.Config.ARGB_8888)
        seal = Bitmap.createBitmap(seal, sealOffsetX, sealOffsetY, bitmap.width, bitmap.height)
        val canvas2Draw = Canvas(bitmap)
        canvas2Draw.drawColor(Color.parseColor("#fff5f5f5"))
        canvas2Draw.drawBitmap(seal, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        val blurWidth = X.size(context, 60f, X.DP).toInt() * 2
        return X.blurBitmap(context, bitmap, blurWidth, blurWidth)
    }
}
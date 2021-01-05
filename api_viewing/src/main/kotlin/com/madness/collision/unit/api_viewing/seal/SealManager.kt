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
import com.madness.collision.unit.api_viewing.R as MyR
import com.madness.collision.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

object SealManager {
    var seals = HashMap<Char, Bitmap>().toMutableMap()
    var sealBack = HashMap<Char, Bitmap>().toMutableMap()

    init {
        GlobalScope.launch {
            val seal = File(F.valCachePubAvSeal(mainApplication))
            val files = seal.listFiles() ?: return@launch
            val regex = "(.+)-(.+)".toRegex()
            files@
            for (file in files) {
                val re = regex.find(file.nameWithoutExtension) ?: continue@files
                val (cat, index) = re.destructured
                val map = when (cat) {
                    "seal" -> seals
                    "back" -> sealBack
                    else -> continue@files
                }
                val letter = index[0]
                // may get deleted during cache clearing on app starts
                if (map.containsKey(letter) || !file.exists()) continue@files
                try {
                    val src = ImageUtil.getBitmap(file) ?: continue
                    map[letter] = src
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                    continue@files
                } catch (e: Exception) {
                    e.printStackTrace()
                    continue@files
                }
//                    try {
//                    }catch (e: FileNotFoundException){
//                        e.printStackTrace()
//                    }
            }
        }
    }

    fun getAndroidCodenameImageRes(letter: Char): Int {
        if (!EasyAccess.isSweet) return 0
        return when (letter) {
            'r' -> MyR.drawable.seal_r_vector
            'q' -> MyR.drawable.seal_q_vector
            'p' -> MyR.drawable.seal_p_vector  // Pie
            'o' -> MyR.drawable.seal_o  // Oreo
            'n' -> MyR.drawable.seal_n  // Nougat
            'm' -> MyR.drawable.seal_m_vector  // Marshmallow
            'l' -> MyR.drawable.seal_l_vector  // Lollipop
            'k' -> MyR.drawable.seal_k_vector  // KitKat
            'j' -> MyR.drawable.seal_j_vector  // Jelly Bean
            'i' -> MyR.drawable.seal_i  // Ice Cream Sandwich
            'h' -> MyR.drawable.seal_h_vector  // Honeycomb
            /*10, 9 ->  // Gingerbread
            8 ->  // Froyo
            7, 6, 5 ->  // Eclair
            4 ->  // Donut
            3 ->  // Cupcake
            2 ->  // null
            1 ->  // null*/
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
            return if (mainApplication.isDarkTheme) ColorUtil.darkenAs(color, if (isForIllustration) 0.7f else 0.15f) else color
        }
    }

    fun populate4Seal(context: Context, index: Char, itemLength: Int) {
        if (seals.containsKey(index)) return
        val res = getAndroidCodenameImageRes(index)
        if (res == 0) return
        val drawable: Drawable
        try {
            drawable = ContextCompat.getDrawable(context, res) ?: return
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            return
        }
        val bitmap = Bitmap.createBitmap(itemLength, itemLength, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, itemLength, itemLength)
        drawable.draw(Canvas(bitmap))
        seals[index] = bitmap
        val path = F.createPath(F.valCachePubAvSeal(context), "seal-$index.png")
        if (F.prepare4(path)) X.savePNG(bitmap, path)
    }

    fun getSealForIllustration(context: Context, index: Char, size: Int): Bitmap? {
        if (seals.containsKey(index)) {
            seals[index]?.let {
                return X.toMax(it, size)
            }
        }
        val res = getAndroidCodenameImageRes(index)
        if (res == 0) return null
        val drawable: Drawable
        try {
            drawable = ContextCompat.getDrawable(context, res) ?: return null
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            return null
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    fun disposeSealBack(context: Context, letter: Char, itemLength: Int): Bitmap {
        if (sealBack.containsKey(letter)) return sealBack[letter]!!
        //final int INITIAL_colorPlain = -1;//color #00000000 has the value of 0
        val colorPlain: Int
        val blurWidth: Int
        var bitmap: Bitmap
        val resImageID = getAndroidCodenameImageRes(letter)
        // draw color
        if (resImageID == 0) {
            val index4SealBack: Char
            if (letter == 'k' && EasyAccess.isSweet) {
                index4SealBack = letter
                colorPlain = Color.parseColor("#753500")
            } else {
                index4SealBack = '?'
                if (sealBack.containsKey(index4SealBack)) return sealBack[index4SealBack]!!
                colorPlain = X.getColor(context, R.color.androidRobotGreen)
            }
            blurWidth = X.size(context, 60f, X.DP).toInt() * 2
            bitmap = Bitmap.createBitmap(blurWidth, blurWidth, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(colorPlain)
            sealBack[index4SealBack] = bitmap
            val path = F.createPath(F.valCachePubAvSeal(context), "back-$index4SealBack.png")
            if (F.prepare4(path)) X.savePNG(bitmap, path)
            return bitmap
        }
        // draw image res
        populate4Seal(context, letter, itemLength)
        var seal: Bitmap = seals[letter]!!.collisionBitmap
        val targetLength = seal.width / 10
        val bitmapWidth = targetLength * 2
        val sealOffsetX = targetLength * 4
        val sealOffsetY = targetLength * 2
        bitmap = Bitmap.createBitmap(bitmapWidth, bitmapWidth, Bitmap.Config.ARGB_8888)
        seal = Bitmap.createBitmap(seal, sealOffsetX, sealOffsetY, bitmap.width, bitmap.height)
        val canvas2Draw = Canvas(bitmap)
        canvas2Draw.drawColor(Color.parseColor("#fff5f5f5"))
        canvas2Draw.drawBitmap(seal, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        blurWidth = X.size(context, 60f, X.DP).toInt() * 2
        bitmap = X.blurBitmap(context, bitmap, blurWidth, blurWidth)
        sealBack[letter] = bitmap
        val path = F.createPath(F.valCachePubAvSeal(context), "back-$letter.png")
        if (F.prepare4(path)) X.savePNG(bitmap, path)
        return bitmap
    }
}
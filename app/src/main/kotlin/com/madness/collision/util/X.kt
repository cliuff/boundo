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

package com.madness.collision.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.util.component1
import androidx.core.util.component2
import com.madness.collision.util.notice.ToastUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.math.min

object X {
    fun deleteFolder( folder: File) = folder.deleteRecursively()

    fun copyFileLessTwoGB( src: File,  dst: File) {
        val inStream = FileInputStream(src)
        val outStream = FileOutputStream(dst)
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inChannel.close()
        outChannel.close()
        inStream.close()
        outStream.close()
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap = GraphicsUtil.convertDrawableToBitmap(drawable)

    /**
     * clip the image to target
     * @param src image to be processed
     * @param targetWidth target width
     * @param targetHeight target height
     * @return product
     */
    fun toTarget(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap{
        if (targetWidth == 0 || targetHeight == 0) return src
        val originalWidth = src.width
        val originalHeight = src.height
        if (originalHeight == targetHeight && originalWidth == targetWidth) return src
        var image: Bitmap
        val cropWidth = originalWidth * targetHeight / originalHeight >= targetWidth
        if (cropWidth){
            val cropLength = (originalWidth - originalHeight * targetWidth / targetHeight) / 2
            image = Bitmap.createBitmap(src, cropLength, 0, originalWidth - 2 * cropLength, originalHeight)
            image = Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
        }else {
            val cropLength = (originalHeight - originalWidth * targetHeight / targetWidth) / 2
            image = Bitmap.createBitmap(src, 0, cropLength, originalWidth, originalHeight - 2 * cropLength)
            image = Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
        }
        return image
    }

    fun toMax( image: Bitmap, maximumLength: Int): Bitmap{
        val (targetWidth, targetHeight) = image.size.doOptimal(maximumLength)
        return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
    }

    fun toMin( image: Bitmap, minimumLength: Int): Bitmap{
        val (targetWidth, targetHeight) = image.size.doMinimum(minimumLength)
        return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
    }

    fun toSquare( image: Bitmap): Bitmap{
        val lengthHorizontal = image.width
        val lengthVertical = image.height
        if (lengthHorizontal == lengthVertical) return image
        return when {
            lengthHorizontal > lengthVertical -> {
                val translateX = (lengthHorizontal - lengthVertical)/2
                val translateY = 0
                Bitmap.createBitmap(image, translateX, translateY, lengthVertical, lengthVertical)
            }
            lengthHorizontal < lengthVertical -> {
                val translateX = 0
                val translateY = (lengthVertical - lengthHorizontal) / 2
                Bitmap.createBitmap(image, translateX, translateY, lengthHorizontal, lengthHorizontal)
            }
            else -> image
        }
    }

    fun getColor( context: Context, colorRes: Int): Int{
        return ContextCompat.getColor(context, colorRes)
    }

    fun getColorHex( context: Context, colorRes: Int): String{
        return Integer.toHexString(getColor(context, colorRes) and 0x00ffffff)
    }

    fun getCurrentAppResolution(context: Context): Point = SystemUtil.getRuntimeWindowSize(context)

    /**
     * get circular bitmap
     * @param source
     * original (non)rectangular bitmap
     * @return
     * circular bitmap
     */
    fun circularBitmap( source: Bitmap): Bitmap{
        val width = source.width
        val height = source.height
        val radius = min(width, height) * 0.5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bitmap = source.toMutable()
        //画布设置遮罩效果
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        //处理图像数据
        val circular = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(circular)
        canvas.drawCircle(width * 0.5f, height * 0.5f, radius, paint)
        return circular
    }

    const val DP = TypedValue.COMPLEX_UNIT_DIP
    const val SP = TypedValue.COMPLEX_UNIT_SP
    fun size(context: Context, value: Float, type: Int): Float {
        return TypedValue.applyDimension(type, value, context.resources.displayMetrics)
    }

    fun saveImage( image: Bitmap,  path: String): Boolean{
        val file = File(path)
        if (!com.madness.collision.util.F.prepare4(file)) return false
        return savePNG(image, path)
    }

    fun savePNG(image: Bitmap,  path: String): Boolean{
        val file = File(path)
        return try {
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, com.madness.collision.util.P.WEBP_COMPRESS_QUALITY_FIRST, stream)
            true
        } catch ( e: FileNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    fun curvedCard( card: CardView){
        card.measure()
        card.radius = (card.measuredHeight / 2).toFloat()
    }

    fun blurBitmap( context: Context,  src: Bitmap): Bitmap{
        return blur(context, src, src.width, src.height)
    }

    fun blurBitmap(context: Context,  src: Bitmap, width: Int, height: Int): Bitmap{
        return blur(context, src, width, height)
    }

    private fun blur(context: Context,  src: Bitmap, width: Int, height: Int): Bitmap{
        val radius = 20f //range from 0 to 20, the larger the blurrier
        val scale2 = 50 //scale to this size in advance, the smaller the blurrier, is 100 in exterior activity
        val blurred = GaussianBlur(context).blurOnce(toMin(src, scale2), radius)
        return toTarget(blurred, width, height)
    }

    fun copyText2Clipboard( context: Context,  content: CharSequence, toastId: Int){
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager? ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("link", content))
        toast(context, toastId, Toast.LENGTH_LONG)
    }

    const val DEV = Build.VERSION_CODES.CUR_DEVELOPMENT
    const val R = Build.VERSION_CODES.R
    const val Q = Build.VERSION_CODES.Q
    const val P = Build.VERSION_CODES.P
    const val O_MR1 = Build.VERSION_CODES.O_MR1
    const val O = Build.VERSION_CODES.O
    const val N_MR1 = Build.VERSION_CODES.N_MR1
    const val N = Build.VERSION_CODES.N
    const val M = Build.VERSION_CODES.M
    const val L_MR1 = Build.VERSION_CODES.LOLLIPOP_MR1
    const val L = Build.VERSION_CODES.LOLLIPOP
    const val K_WATCH = Build.VERSION_CODES.KITKAT_WATCH
    const val K = Build.VERSION_CODES.KITKAT
    const val J_MR2 = Build.VERSION_CODES.JELLY_BEAN_MR2
    const val J_MR1 = Build.VERSION_CODES.JELLY_BEAN_MR1
    const val J = Build.VERSION_CODES.JELLY_BEAN
    const val I_MR1 = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
    const val I = Build.VERSION_CODES.ICE_CREAM_SANDWICH
    const val H_MR2 = Build.VERSION_CODES.HONEYCOMB_MR2
    const val H_MR1 = Build.VERSION_CODES.HONEYCOMB_MR1
    const val H = Build.VERSION_CODES.HONEYCOMB
    const val G_MR1 = Build.VERSION_CODES.GINGERBREAD_MR1
    const val G = Build.VERSION_CODES.GINGERBREAD
    const val F = Build.VERSION_CODES.FROYO
    const val E_MR1 = Build.VERSION_CODES.ECLAIR_MR1
    const val E_0_1 = Build.VERSION_CODES.ECLAIR_0_1
    const val E = Build.VERSION_CODES.ECLAIR
    const val D = Build.VERSION_CODES.DONUT
    const val C = Build.VERSION_CODES.CUPCAKE
    const val B = Build.VERSION_CODES.BASE_1_1
    const val A = Build.VERSION_CODES.BASE

    fun aboveOn(apiLevel: Int): Boolean = Build.VERSION.SDK_INT >= apiLevel

    fun belowOff(apiLevel: Int): Boolean = Build.VERSION.SDK_INT < apiLevel

    fun toast(context: Context, messageRes: Int, duration: Int) {
        toast(context, context.getString(messageRes), duration)
    }

    fun toast(context: Context, message: CharSequence, duration: Int) {
        GlobalScope.launch {
            ToastUtils.toast(context, message, duration)
        }
    }
}

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

package com.madness.collision.wearable.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal object X {
    /**
     * normal drawable that are not AdaptiveIconDrawable
     * @param drawable drawable res
     * @return bitmap
     */
    fun drawableToBitmap ( drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap.collisionBitmap

        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight
        val bitmap: Bitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0)
            Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        else
            Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

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

    fun getColor( context: Context, colorRes: Int): Int{
        return ContextCompat.getColor(context, colorRes)
    }

    fun getColorHex( context: Context, colorRes: Int): String{
        return Integer.toHexString(getColor(context, colorRes) and 0x00ffffff)
    }

    fun  getCurrentAppResolution(context: Context): Point{
        val size = Point()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager? ?: return Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

    const val DP = TypedValue.COMPLEX_UNIT_DIP
    const val SP = TypedValue.COMPLEX_UNIT_SP
    fun size( context: Context, value: Float, type: Int): Float = TypedValue.applyDimension(type, value, context.resources.displayMetrics)

    fun getAndroidVersionByAPI(api: Int, exact: Boolean): String{
        return when (api){
            DEV -> if (exact) "DEV" else ""
            U -> "14"
            T -> "13"
            S_V2 -> if (exact) "12L" else "12"
            S -> "12"
            R -> "11"
            Q -> "10"
            P -> "9"  // 9 Pie
            O_MR1 -> if (exact) "8.1.0" else "8"  // 8.1.0 Oreo
            O -> if (exact) "8.0.0" else "8"  // 8.0.0 Oreo
            N_MR1-> if (exact) "7.1" else "7"  // 7.1 Nougat
            N -> if (exact) "7.0" else "7"  // 7.0 Nougat
            M -> if (exact) "6.0" else "6"  // 6.0 Marshmallow
            L_MR1 -> if (exact) "5.1" else "5"  // 5.1 Lollipop
            L -> if (exact) "5.0" else "5"  // 5.0 Lollipop
            K_WATCH -> if (exact) "4.4W" else "4"  // 4.4W KitKat
            K -> if (exact) "4.4 - 4.4.4" else "4"  // 4.4 - 4.4.4 KitKat
            J_MR2 -> if (exact) "4.3.x" else "4"  // 4.3.x Jelly Bean
            J_MR1 -> if (exact) "4.2.x" else "4"  // 4.2.x Jelly Bean
            J -> if (exact) "4.1.x" else "4"  // 4.1.x Jelly Bean
            I_MR1 -> if (exact) "4.0.3 - 4.0.4" else "4"  // 4.0.3 - 4.0.4 Ice Cream Sandwich
            I -> if (exact) "4.0.1 - 4.0.2" else "4"  // 4.0.1 - 4.0.2 Ice Cream Sandwich
            H_MR2 -> if (exact) "3.2.x" else "3"  // 3.2.x Honeycomb
            H_MR1 -> if (exact) "3.1" else "3"  // 3.1 Honeycomb
            H -> if (exact) "3.0" else "3"  // 3.0 Honeycomb
            G_MR1 -> if (exact)  "2.3.3 - 2.3.7" else "2"  // 2.3.3 - 2.3.7 Gingerbread
            G -> if (exact) "2.3 - 2.3.2" else "2"  // 2.3 - 2.3.2 Gingerbread
            F -> if (exact) "2.2.x" else "2"  // 2.2.x Froyo
            E_MR1 -> if (exact) "2.1" else "2"  // 2.1 Eclair
            E_0_1 -> if (exact) "2.0.1" else "2"  // 2.0.1 Eclair
            E -> if (exact) "2.0" else "2"  // 2.0 Eclair
            D -> if (exact) "1.6" else "1"  // 1.6 Donut
            C -> if (exact) "1.5" else "1"  // 1.5 Cupcake
            B -> if (exact) "1.1" else "1"  // 1.1 null
            A -> if (exact) "1.0" else "1"  // 1.0 null
            else -> ""
        }
    }

    const val DEV = Build.VERSION_CODES.CUR_DEVELOPMENT
    const val U = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    const val T = Build.VERSION_CODES.TIRAMISU
    const val S_V2 = Build.VERSION_CODES.S_V2
    const val S = Build.VERSION_CODES.S
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

    fun aboveOff(apiLevel: Int): Boolean = Build.VERSION.SDK_INT > apiLevel

    fun aboveOn(apiLevel: Int): Boolean = Build.VERSION.SDK_INT >= apiLevel

    fun belowOff(apiLevel: Int): Boolean = Build.VERSION.SDK_INT < apiLevel

    fun belowOn(apiLevel: Int): Boolean = Build.VERSION.SDK_INT <= apiLevel

    fun toast( context: Context, messageRes: Int, duration: Int){
        toast(context, context.getString(messageRes), duration)
    }

    fun toast( context: Context,  message: CharSequence, duration: Int){
        GlobalScope.launch(Dispatchers.Main){
            if (!mainApplication.notificationAvailable) {
                return@launch
            }
            Toast.makeText(context, message, duration).show()
        }
    }
}

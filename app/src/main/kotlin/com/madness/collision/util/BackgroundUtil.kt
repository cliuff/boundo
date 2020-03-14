package com.madness.collision.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

object BackgroundUtil {
    /**
     * Make the size of src at least width x height
     */
    fun adjustBounds(src: Drawable, width: Int, height: Int): Rect{
        var widthTotal = if (src is BitmapDrawable) src.bitmap.width else src.intrinsicWidth
        var heightTotal = if (src is BitmapDrawable) src.bitmap.height else src.intrinsicHeight
        if (widthTotal < width){
            val fr = width.toFloat() / widthTotal.toFloat()
            widthTotal = (widthTotal * fr).roundToInt()
            heightTotal = (heightTotal * fr).roundToInt()
        }
        if (heightTotal < height){
            val fr = height.toFloat() / heightTotal.toFloat()
            widthTotal = (widthTotal * fr).roundToInt()
            heightTotal = (heightTotal * fr).roundToInt()
        }
        src.setBounds(0, 0, widthTotal, heightTotal)
        return src.bounds
    }

    /**
     * App background
     */
    fun getBackground(src: Drawable, width: Int, height: Int): Bitmap{
        val bounds = adjustBounds(src, width, height)
        val widthTotal = bounds.width()
        val heightTotal = bounds.height()
        val offsetHor: Int = (widthTotal - width) / 2
        val offsetVer: Int = (heightTotal - height) / 2
        val re = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(re)
        canvas.translate(-offsetHor.toFloat(), -offsetVer.toFloat())
        src.draw(canvas)
        return re
    }
}

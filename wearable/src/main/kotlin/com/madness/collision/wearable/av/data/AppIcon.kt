package com.madness.collision.wearable.av.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import com.madness.collision.wearable.util.GraphicsUtil
import com.madness.collision.wearable.util.X
import kotlin.math.roundToInt

internal class AppIcon(private val context: Context, val drawable: Drawable) {
    val isAdaptive = X.aboveOn(X.O) && drawable is AdaptiveIconDrawable
    val bitmap: Bitmap = if(!isAdaptive || X.belowOff(X.O)) X.drawableToBitmap(drawable) else {
        val ai = drawable as AdaptiveIconDrawable
        var back = ai.background
        if (back != null) back = back.mutate()
        var fore = ai.foreground
        if (fore != null) fore = fore.mutate()

        val targetRadius = X.size(context, 54f, X.DP).roundToInt()
        val targetRadiusFloat = targetRadius.toFloat()
        val targetDiameter = targetRadius * 2

        val result = Bitmap.createBitmap(targetDiameter, targetDiameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        if (back != null) {
            back.setBounds(0, 0, targetDiameter, targetDiameter)
            back.draw(canvas)
        }
        if (fore != null) {
            fore.setBounds(0, 0, targetDiameter, targetDiameter)
            fore.draw(canvas)
        }
        result
    }
    val round: Bitmap by lazy {
        if (!isAdaptive || X.belowOff(X.O)) bitmap
        else GraphicsUtil.drawAIRound(context, drawable)
    }
    val rounded: Bitmap by lazy {
        if (!isAdaptive || X.belowOff(X.O)) bitmap
        else GraphicsUtil.drawAIRounded(context, drawable)
    }
    val squircle: Bitmap by lazy {
        if (!isAdaptive || X.belowOff(X.O)) bitmap
        else GraphicsUtil.drawAISquircle(context, drawable)
    }
}

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.madness.collision.unit.api_viewing.list.IconInfo
import com.madness.collision.util.GraphicsUtil
import com.madness.collision.util.X
import kotlin.math.roundToInt

class AppIcon(private val context: Context, val icon: IconInfo.AdaptiveIcon) {
    val bitmap: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        var back = icon.background
        if (back != null) back = back.mutate()
        var fore = icon.foreground
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
    val round: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        if (X.belowOff(X.O)) return@lazy bitmap
        GraphicsUtil.drawAI(context, icon.drawable, GraphicsUtil.AI_FLAVOR_ROUND, allowStroke = false)
    }
    val rounded: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        if (X.belowOff(X.O)) return@lazy bitmap
        GraphicsUtil.drawAI(context, icon.drawable, GraphicsUtil.AI_FLAVOR_ROUNDED, allowStroke = false)
    }
    val squircle: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        if (X.belowOff(X.O)) return@lazy bitmap
        GraphicsUtil.drawAI(context, icon.drawable, GraphicsUtil.AI_FLAVOR_SQUIRCLE, allowStroke = false)
    }
}

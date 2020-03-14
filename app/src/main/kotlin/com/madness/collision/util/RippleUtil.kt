package com.madness.collision.util

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import java.util.*

object RippleUtil {

    fun getSelectableDrawablePure(color: Int, roundCornerRadius: Float): Drawable {
        val pressedColor: ColorStateList = ColorStateList.valueOf(color)
        val defaultColor = ColorDrawable(Color.TRANSPARENT)
        val rippleColor: Drawable = getRippleColor(color, roundCornerRadius)
        return RippleDrawable(pressedColor, defaultColor, rippleColor)
    }

    fun getSelectableDrawable(color: Int, roundCornerRadius: Float): Drawable {
        val pressedColor = ColorStateList.valueOf(ColorUtil.lightenOrDarken(color, 0.9f))
        val defaultColor = ColorDrawable(color)
        val rippleColor = getRippleColor(color, roundCornerRadius)
        return RippleDrawable(pressedColor, defaultColor, rippleColor)
    }

    private fun getRippleColor(color: Int, radius: Float): Drawable {
        val outerRadii = FloatArray(8)
        Arrays.fill(outerRadii, radius)
        val r = RoundRectShape(outerRadii, null, null)
        val shapeDrawable = ShapeDrawable(r)
        shapeDrawable.paint.color = color
        return shapeDrawable
    }

}

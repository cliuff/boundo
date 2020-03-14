package com.madness.collision.wearable.util

import android.graphics.Color
import androidx.core.graphics.ColorUtils

internal object ColorUtil{

    fun lightenOrDarken(color: Int, fraction: Float): Int {
        canLighten(color, fraction.toDouble()).let {
            return if (it) lightenAs(color, fraction) else darkenAs(color, fraction)
        }
    }

    /**
     * lighten the color as it occupying fraction of the whole, and white occupying the remaining
     */
    fun lightenAs(color: Int, fraction: Float): Int {
        var red = Color.red(color)
        var green = Color.green(color)
        var blue = Color.blue(color)
        red = ColorUtils.blendARGB(Color.WHITE, red, fraction)
        green = ColorUtils.blendARGB(Color.WHITE, green, fraction)
        blue = ColorUtils.blendARGB(Color.WHITE, blue, fraction)
        val alpha = Color.alpha(color)
        return Color.argb(alpha, red, green, blue)
    }

    /**
     * darken the color as it occupying fraction of the whole, and black occupying the remaining
     */
    fun darkenAs(color: Int, fraction: Float): Int {
        var red = Color.red(color)
        var green = Color.green(color)
        var blue = Color.blue(color)
        red = ColorUtils.blendARGB(Color.BLACK, red, fraction)
        green = ColorUtils.blendARGB(Color.BLACK, green, fraction)
        blue = ColorUtils.blendARGB(Color.BLACK, blue, fraction)
        val alpha = Color.alpha(color)

        return Color.argb(alpha, red, green, blue)
    }

    private fun canLighten(color: Int, fraction: Double): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return canLightenComponent(red, fraction) && canLightenComponent(green, fraction) && canLightenComponent(blue, fraction)
    }

    private fun canLightenComponent(colorComponent: Int, fraction: Double): Boolean {
        val red = Color.red(colorComponent)
        val green = Color.green(colorComponent)
        val blue = Color.blue(colorComponent)
        return red + (red * fraction) < 255 && green + (green * fraction) < 255 && blue + (blue * fraction) < 255
    }
}

package com.madness.collision.wearable.util

import android.content.Context
import android.util.TypedValue
import com.madness.collision.wearable.R

internal object ThemeUtil {
    fun getThemeId(context: Context): Int {
        val resValue = TypedValue()
        context.theme.resolveAttribute(R.attr.themeId, resValue, false)
        return resValue.data
    }

    fun getColor(context: Context, attr: Int): Int{
        val resColor = TypedValue()
        context.theme.resolveAttribute(attr, resColor, true)
        return resColor.data
    }

    fun getBackColor(color: Int, fraction: Float): Int{
        return ColorUtil.darkenAs(color, fraction)
    }
}

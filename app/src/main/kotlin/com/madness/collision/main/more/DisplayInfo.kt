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

package com.madness.collision.main.more

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import com.madness.collision.util.os.OsUtils
import kotlin.math.roundToInt

internal object DisplayInfo {

    private class DevSize(val pixelX: Int, val pixelY: Int) {

        constructor(point: Point) : this(point.x, point.y)

        fun toString(resources: Resources): String {
            val density = resources.displayMetrics.density
            val dpX = (pixelX / density).roundToInt()
            val dpY = (pixelY / density).roundToInt()
            return "$pixelX × $pixelY px / $dpX × $dpY dp"
        }
    }

    private fun Point.getSizeString(resources: Resources): String {
        return DevSize(this).toString(resources)
    }

    fun getInfo(context: Context, buffer: Appendable): Unit = buffer.run {
        val resources = context.resources
        val densityDpi = resources.configuration.densityDpi
        val density = resources.displayMetrics.density
        append("Density: ").append("$densityDpi dpi").append(", ${density}x\n")

        val windowMetricsCalculator = WindowMetricsCalculator.getOrCreate()
        val currentMetrics = windowMetricsCalculator.computeCurrentWindowMetrics(context)
        val maximumMetrics = windowMetricsCalculator.computeMaximumWindowMetrics(context)
        append("Current window size: ")
        append(currentMetrics.bounds.run { Point(width(), height()) }.getSizeString(resources)).append('\n')
        append("Maximum window size: ")
        append(maximumMetrics.bounds.run { Point(width(), height()) }.getSizeString(resources)).append('\n')

        if (OsUtils.dissatisfy(OsUtils.R)) return
        val winMan = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        if (winMan == null) {
            append("Failed to access window manager\n")
            return
        }
        val funWinMetrics: (WindowMetrics) -> Unit = {
            val regex = """Insets\{left=(\d+), top=(\d+), right=(\d+), bottom=(\d+)\}""".toRegex()
            val pxDp: (String, Int) -> String = { px, dp -> if (dp == 0) "0" else "$px/$dp" }
            val windowInsetsOutput = it.windowInsets.toString().replace(regex) { re ->
                val iPx = re.groupValues.drop(1)
                var isNone = true
                val iDp = iPx.map { px ->
                    if (px == "0") return@map 0
                    isNone = false
                    (px.toInt() / density).roundToInt()
                }
                // return [0] when all are zeros
                if (isNone) return@replace "[0]"
                // convert px and dp values to output
                val o = List(iPx.size) { i -> pxDp(iPx[i], iDp[i]) }
                "[L${o[0]}, T${o[1]}, R${o[2]}, B${o[3]}; px/dp]"
            }
            append("Bounds: ").append(it.bounds.toString()).append('\n')
            append("Insets:\n").append(windowInsetsOutput).append('\n')
        }
        append("\nCurrent Window Metrics\n")
        funWinMetrics(winMan.currentWindowMetrics)
        append("\nMaximum Window Metrics\n")
        funWinMetrics(winMan.maximumWindowMetrics)
    }

    fun getDisplaysAndInfo(context: Context): CharSequence {
        val strBuilder = StringBuilder()

        val displayMan = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        val displays = displayMan?.displays ?: emptyArray()
        strBuilder.append(if (displays.size > 1) "Displays: " else "Display: ")
        displays.joinTo(strBuilder) { it.name }
        strBuilder.append('\n')

        getInfo(context, strBuilder)
        return strBuilder
    }
}

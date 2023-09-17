/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.chief.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import com.madness.collision.util.GraphicsUtil
import kotlin.math.roundToInt

object AdaptiveIconLoader {
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadMiuiAdaptiveIcon(drawable: AdaptiveIconDrawable, size: Int): Bitmap {
        // circle mask sample: mask/size=174/214
        // stock: 2|8|2=8/12, miui: 1|1|8|1|1=10/12
        // size occupies 10/12 of full icon size
        val fullSize = (size * 6f / 5f).roundToInt()
        val rect = Rect(0, 0, fullSize, fullSize)
        return drawAdaptiveIcon(drawable, size, rect, (fullSize - size) / 2f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadFullAdaptiveIcon(drawable: AdaptiveIconDrawable, size: Int): Bitmap {
        val rect = Rect(0, 0, size, size)
        return drawAdaptiveIcon(drawable, size, rect, 0f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadRectAdaptiveIcon(drawable: AdaptiveIconDrawable, size: Int): Bitmap {
        // size occupies 4/6 of full icon size
        val fullSize = (size * 3f / 2f).roundToInt()
        val rect = Rect(0, 0, fullSize, fullSize)
        return drawAdaptiveIcon(drawable, size, rect, (fullSize - size) / 2f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun drawAdaptiveIcon(drawable: AdaptiveIconDrawable, size: Int, bounds: Rect, offset: Float): Bitmap {
        val layerList = listOfNotNull(drawable.background?.mutate(), drawable.foreground?.mutate())
        layerList.forEach { GraphicsUtil.resolveBounds(it, bounds.width(), bounds.height()) }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            Canvas(bitmap).let { canvas ->
                if (offset != 0f) canvas.translate(-offset, -offset)
                layerList.forEach { it.draw(canvas) }
            }
        }
    }
}
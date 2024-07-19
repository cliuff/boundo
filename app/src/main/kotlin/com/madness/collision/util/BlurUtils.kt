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

import android.graphics.*
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.madness.collision.util.os.OsUtils
import java.util.function.Consumer

internal object BlurUtils {

    @RequiresApi(OsUtils.S)
    fun blur(src: Bitmap, radiusX: Float, radiusY: Float): Bitmap? {
        // create result bitmap
        val output = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        // create canvas from result bitmap
        val canvas = Canvas(output)
        // check hardware acceleration
        if (canvas.isHardwareAccelerated.not()) return null
        // create src effect from src bitmap
        val srcEffect = RenderEffect.createBitmapEffect(src)
        // create blur effect from src effect
        val blurEffect = RenderEffect.createBlurEffect(radiusX, radiusY, srcEffect, Shader.TileMode.MIRROR)
        // create render node and set render effect
        val renderNode = RenderNode(null).apply { setRenderEffect(blurEffect) }
        // draw render node
        canvas.drawRenderNode(renderNode)
        return output
    }

    @RequiresApi(OsUtils.S)
    fun Bitmap.blurred(radiusX: Float, radiusY: Float): Bitmap? = blur(this, radiusX, radiusY)

    @RequiresApi(OsUtils.S)
    fun blurBehind(window: Window, blurRadius: Int) {
        val context = window.context ?: return
        // Blur window background
        window.setBackgroundBlurRadius(blurRadius)
        // Blur behind window
        window.attributes.run {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            blurBehindRadius = blurRadius
        }
        // Cross-window blur
        val windowManager = context.getSystemService(WindowManager::class.java) as WindowManager
        val isCrossWindowBlurEnabled = windowManager.isCrossWindowBlurEnabled
        val listener: Consumer<Boolean> = Consumer { }
        windowManager.addCrossWindowBlurEnabledListener(listener)
        windowManager.removeCrossWindowBlurEnabledListener(listener)
    }
}
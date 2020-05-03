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

package com.madness.collision.util

import android.content.Context
import android.graphics.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.madness.collision.R

/**
 * Quick access
 */
internal object QuickAccess{

    /**
     * using ImageDecoder to create rounded corner images
     * 19040223
     */
    @RequiresApi(X.P)
    fun imageTransformerRoundedCorner(context: Context) = ImageDecoder.OnHeaderDecodedListener { decoder, _, _ ->
        val path = Path().apply { fillType = Path.FillType.INVERSE_EVEN_ODD }
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        val radius = context.resources.getDimension(R.dimen.radius)
        decoder.postProcessor = PostProcessor { canvas ->
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val direction = Path.Direction.CW
            path.addRoundRect(0f, 0f, width, height, radius, radius, direction)
            canvas.drawPath(path, paint)
            PixelFormat.TRANSLUCENT
        }
        decoder.isMutableRequired = true
        decoder.onPartialImageListener = imageTransformerGeneralErrorHandler(context)
    }

    @RequiresApi(X.P)
    fun imageTransformerGeneralErrorHandler(context: Context) = ImageDecoder.OnPartialImageListener {
        when (it.error) {
            ImageDecoder.DecodeException.SOURCE_EXCEPTION -> "error reading src"
            ImageDecoder.DecodeException.SOURCE_INCOMPLETE -> "broken src"
            ImageDecoder.DecodeException.SOURCE_MALFORMED_DATA -> "malformed data"
            else -> return@OnPartialImageListener true
        }.run { X.toast(context, this, Toast.LENGTH_SHORT) }
        true
    }

    @RequiresApi(X.P)
    fun generalHeaderDecodedListener(context: Context? = null) = ImageDecoder.OnHeaderDecodedListener { decoder, _, _ ->
        decoder.isMutableRequired = true
        context ?: return@OnHeaderDecodedListener
        decoder.onPartialImageListener = imageTransformerGeneralErrorHandler(context)
    }

}

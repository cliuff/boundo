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
}

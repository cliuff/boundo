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
import android.net.Uri
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import java.io.File


object ImageUtil {

    fun getBitmap(path: String): Bitmap?{
        return getBitmap(File(path))
    }

    /**
     * @return mutable bitmap
     */
    fun getBitmap(file: File): Bitmap?{
        if (!file.exists()) return null
        return if (X.aboveOn(X.P)) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file), QuickAccess.generalHeaderDecodedListener())
        } else {
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inMutable = true })
        }
    }

    /**
     * @return mutable bitmap
     */
    fun getBitmap(context: Context, uri: Uri): Bitmap?{
        return if (X.aboveOn(X.P)) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri), QuickAccess.generalHeaderDecodedListener(context))
        } else {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply { inMutable = true })
            }
        }
    }

    /**
     * @return mutable bitmap
     */
    fun getSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return getSampledBitmap(File(path), reqWidth, reqHeight)
    }

    /**
     * @return mutable bitmap
     */
    fun getSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return if (X.aboveOn(X.P)) {
            getSampledBitmap(reqWidth, reqHeight, ImageDecoder.createSource(file))
        } else {
            getSampledBitmap(reqWidth, reqHeight) {
                BitmapFactory.decodeFile(file.path, it)
            }
        }
    }

    /**
     * @return mutable bitmap
     */
    fun getSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return if (X.aboveOn(X.P)) {
            getSampledBitmap(reqWidth, reqHeight, ImageDecoder.createSource(context.contentResolver, uri), context)
        } else {
            getSampledBitmap(reqWidth, reqHeight) {
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, it)
                }
            }
        }
    }

    private fun calculateInSampleSize(oriSize: Size, reqSize: Size): Int {
        // Raw height and width of image
        val (width: Int, height: Int) = oriSize.run { width to height }
        val (reqWidth: Int, reqHeight: Int) = reqSize.run { width to height }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun BitmapFactory.Options.resolveInSampleSize(reqWidth: Int, reqHeight: Int) {
        inSampleSize = calculateInSampleSize(Size(outWidth, outHeight), Size(reqWidth, reqHeight))
    }

    @RequiresApi(X.P)
    private fun ImageDecoder.resolveInSampleSize(info: ImageDecoder.ImageInfo, reqWidth: Int, reqHeight: Int) {
        setTargetSampleSize(calculateInSampleSize(info.size, Size(reqWidth, reqHeight)))
    }

    /**
     * @return mutable bitmap
     */
    private fun getSampledBitmap(reqWidth: Int, reqHeight: Int, block: (options: BitmapFactory.Options) -> Bitmap?): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            block.invoke(this)
            // Calculate inSampleSize
            resolveInSampleSize(reqWidth, reqHeight)
            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            inMutable = true
            block.invoke(this)
        }
    }

    /**
     * @return mutable bitmap
     */
    @RequiresApi(X.P)
    private fun getSampledBitmap(reqWidth: Int, reqHeight: Int, source: ImageDecoder.Source, context: Context? = null): Bitmap {
        return source.decodeBitmap { info, s ->
            resolveInSampleSize(info, reqWidth, reqHeight)
            QuickAccess.generalHeaderDecodedListener(context).onHeaderDecoded(this, info, s)
        }
    }

}

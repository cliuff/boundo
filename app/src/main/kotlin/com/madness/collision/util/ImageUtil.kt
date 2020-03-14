package com.madness.collision.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object ImageUtil {

    fun getBitmap(path: String): Bitmap?{
        return getBitmap(File(path))
    }

    fun getBitmap(file: File): Bitmap?{
        if (!file.exists()) return null
        return if (X.aboveOn(X.P)) ImageDecoder.decodeBitmap(ImageDecoder.createSource(file))
        else BitmapFactory.decodeFile(file.path)
    }

    fun getBitmap(context: Context, uri: Uri): Bitmap?{
        return if (X.aboveOn(X.P)) ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        else MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

}

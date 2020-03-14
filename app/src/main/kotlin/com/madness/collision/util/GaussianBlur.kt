package com.madness.collision.util

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

class GaussianBlur(context: Context) {
    private val renderScript: RenderScript = RenderScript.create(context)

    /**
     * Gaussian Blur images
     * @param radius Blur radius, value from 0(exclusive) to 25(inclusive) due to performance limit
     * @param src The image to be blurred
     */
    fun blur(src: Bitmap, radius: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(src)
        // use Allocation to allocate memory
        val inputAllocation = Allocation.createFromBitmap(renderScript, bitmap)
        val outputAllocation = Allocation.createTyped(renderScript, inputAllocation.type)
        // use Element.U8_4(renderScript) for Gaussian Blur
        ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript)).run {
            setRadius(radius)
            setInput(inputAllocation) // set input memory allocation
            forEach(outputAllocation) // save data into output memory allocation
            outputAllocation.copyTo(bitmap) // copy data from output to bitmap
            destroy()
        }
        outputAllocation.destroy()
        inputAllocation.destroy()
        return bitmap
    }

    fun blurOnce(src: Bitmap, radius: Float): Bitmap = blur(src, radius).also { destroy() }

    fun destroy() = renderScript.destroy()
}

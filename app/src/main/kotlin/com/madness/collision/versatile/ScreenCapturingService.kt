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

package com.madness.collision.versatile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.madness.collision.R
import com.madness.collision.util.NotificationsUtil
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

internal class ScreenCapturingService: Service() {
    companion object {
        const val ARG_RESULT_CODE = "resultCode"
        const val ARG_DATA = "data"

        private const val CAPTURE_NAME = "BoundoCapture"
        private const val VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        private lateinit var sMediaProjection: MediaProjection

        var capture: Bitmap? = null
        // Service cannot obtain display by its context
        var uiContext: WeakReference<Context>? = null
    }

    private lateinit var mProjectionManager: MediaProjectionManager
    private lateinit var mImageReader: ImageReader
    private lateinit var mHandler: Handler
    private var mDisplay: Display? = null
    private lateinit var mVirtualDisplay: VirtualDisplay
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private lateinit var  mOrientationChangeCallback: OrientationChangeCallback
    private lateinit var windowManager: WindowManager
    private var localeContext: Context? = null
    private var context: Context = this

    inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            if (capture != null) return
            val image = reader.acquireLatestImage() ?: return
            stopProjection()

            val plane = image.planes[0]
            val rowPadding = plane.rowStride - plane.pixelStride * mWidth
            // margin on the right
            val imageMargin = rowPadding / plane.pixelStride
            val width = mWidth + imageMargin

            // result bitmap has a bigger width than the actual image
            val bitmap = Bitmap.createBitmap(width, image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(plane.buffer)
            // crop out the margin on the right of the result bitmap
            capture = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight)

            if (X.aboveOn(X.P)) reader.discardFreeBuffers()
            reader.close()
        }
    }

    inner class OrientationChangeCallback(context: Context) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val display = mDisplay ?: return
            val rotation = display.rotation
            if (rotation == mRotation) return
            mRotation = rotation
            try {
                // clean up
                mVirtualDisplay.release()
                mImageReader.setOnImageAvailableListener(null, null)
                // re-create virtual display depending on device width / height
                createVirtualDisplay()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            mVirtualDisplay.release()
            mImageReader.setOnImageAvailableListener(null, null)
            mOrientationChangeCallback.disable()
            sMediaProjection.unregisterCallback(this@MediaProjectionStopCallback)
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = uiContext?.get() ?: this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (X.aboveOn(X.Q)){
            localeContext = SystemUtil.getLocaleContextSys(context)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(ARG_RESULT_CODE, 0) ?: 0
        val data: Intent = intent?.getParcelableExtra(ARG_DATA) ?: return super.onStartCommand(intent, flags, startId)
        if (X.aboveOn(X.Q)){
            val color = X.getColor(context, if (ThemeUtil.getIsNight(context)) R.color.primaryABlack else R.color.primaryAWhite)
            val builder = NotificationsUtil.Builder(context, NotificationsUtil.CHANNEL_SERVICE)
                    .setSmallIcon(R.drawable.ic_notify_logo)
                    .setColor(color)
                    .setContentTitle(localeContext?.getString(R.string.textCapturingScreen))
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setAutoCancel(true)
            startForeground(NotificationsUtil.ID_SCREEN_CAPTURING, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        }
        startCapturing(resultCode, data)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startCapturing(resultCode: Int, data: Intent){
        // start capture handling thread
        Thread {
            Looper.prepare()
            mHandler = Handler(Looper.myLooper()!!)
            GlobalScope.launch {
                delay(200)
                startProjection(resultCode, data)
            }
            Looper.loop()
        }.start()
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data)
        mDisplay = SystemUtil.getDisplay(context)
        mOrientationChangeCallback = OrientationChangeCallback(context)
        if (mOrientationChangeCallback.canDetectOrientation()) mOrientationChangeCallback.enable()
        sMediaProjection.registerCallback(MediaProjectionStopCallback(), mHandler)
        GlobalScope.launch {
            delay(100)
            createVirtualDisplay()
        }
    }

    private fun createVirtualDisplay() {
        val size = SystemUtil.getRuntimeMaximumSize(context)
        mWidth = size.x
        mHeight = size.y
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 1)
        val dpi = context.resources.displayMetrics.densityDpi
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(
                CAPTURE_NAME, mWidth, mHeight, dpi,
                VIRTUAL_DISPLAY_FLAGS, mImageReader.surface,
                null, mHandler
        )
        mImageReader.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    private fun stopProjection() {
        uiContext = null
        mHandler.post{ sMediaProjection.stop() }
        GlobalScope.launch {
            delay(500)
            stopSelf()
        }
    }
}

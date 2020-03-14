package com.madness.collision.versatile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
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

internal class ScreenCapturingService: Service() {
    companion object {
        const val ARG_RESULT_CODE = "resultCode"
        const val ARG_DATA = "data"

        private const val CAPTURE_NAME = "BoundoCapture"
        private const val VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        private lateinit var sMediaProjection: MediaProjection

        var capture: Bitmap? = null
    }

    private lateinit var mProjectionManager: MediaProjectionManager
    private lateinit var  mImageReader:ImageReader
    private lateinit var  mHandler:Handler
    private lateinit var  mDisplay:Display
    private lateinit var  mVirtualDisplay:VirtualDisplay
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private lateinit var  mOrientationChangeCallback: OrientationChangeCallback
    private lateinit var windowManager: WindowManager
    private var localeContext: Context? = null

    inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            if (capture != null) return
            val image = reader.acquireLatestImage() ?: return
            stopProjection()

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * mWidth

            capture = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888)
            capture!!.copyPixelsFromBuffer(buffer)

            if (X.aboveOn(X.P)) reader.discardFreeBuffers()
            reader.close()
        }
    }

    inner class OrientationChangeCallback(context: Context) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = mDisplay.rotation
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    mVirtualDisplay.release()
                    mImageReader.setOnImageAvailableListener(null, null)
                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                } catch ( e: Exception) {
                    e.printStackTrace()
                }
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
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (X.aboveOn(X.Q)){
            val context = this
            localeContext = SystemUtil.getLocaleContextSys(context)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(ARG_RESULT_CODE, 0) ?: 0
        val data: Intent = intent?.getParcelableExtra(ARG_DATA) ?: return super.onStartCommand(intent, flags, startId)
        if (X.aboveOn(X.Q)){
            val context = this
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
            mHandler = Handler()
            mHandler.postDelayed({
                startProjection(resultCode, data)
            }, 200)
            Looper.loop()
        }.start()
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data)
        mDisplay = windowManager.defaultDisplay
        mOrientationChangeCallback = OrientationChangeCallback(this)
        if (mOrientationChangeCallback.canDetectOrientation()) mOrientationChangeCallback.enable()
        sMediaProjection.registerCallback(MediaProjectionStopCallback(), mHandler)
        Handler().postDelayed({ createVirtualDisplay() }, 100)
    }

    private fun createVirtualDisplay() {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        mWidth = size.x
        mHeight = size.y
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 1)
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(
                CAPTURE_NAME, mWidth, mHeight,
                resources.displayMetrics.densityDpi,
                VIRTUAL_DISPLAY_FLAGS, mImageReader.surface,
                null, mHandler
        )
        mImageReader.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    private fun stopProjection() {
        mHandler.post{ sMediaProjection.stop() }
        mHandler.postDelayed({ stopSelf() }, 500)
    }
}

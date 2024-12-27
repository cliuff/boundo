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

package com.madness.collision.unit.themed_wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.madness.collision.R
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.os.OsUtils
import java.lang.ref.WeakReference

class ThemedWallpaperService : WallpaperService(){

    private val wallpaperDrawable: Drawable
        get() = ThemedWallpaperEasyAccess.background!!
    private var frameRate: Float = 0.1f
        set(value) {
            frameGap = (1000f / value).toLong()
            field = value
        }
    private var frameGap: Long = 10_000L
    private lateinit var prefSettings: SharedPreferences
    private val engineRefs: MutableList<WeakReference<ThemedEngine>> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        prefSettings = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val keyApplyDarkPlan = resources.getString(R.string.prefExteriorKeyDarkPlan)
        val planValue = prefSettings.getString(keyApplyDarkPlan, resources.getString(R.string.prefExteriorDefaultDarkPlan)) ?: ""
        frameRate = if(planValue == resources.getString(R.string.prefExteriorDarkPlanValueSchedule)) 0.05f else 1f
        if (ThemedWallpaperEasyAccess.isDead) {
            ThemedWallpaperEasyAccess.wallpaperTimestamp = System.currentTimeMillis()
            ThemedWallpaperEasyAccess.isDead = false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val iterator = engineRefs.iterator()
        while (iterator.hasNext()) {
            val engine = iterator.next().get()
            when {
                engine == null -> iterator.remove()
                engine.isVisible -> engine.onConfigurationChanged(newConfig)
            }
        }
    }

    override fun onCreateEngine(): Engine {
        val context = baseContext
        val shouldChange = ThemeUtil.shouldChangeTheme4ThemedWallpaper(context, prefSettings)
        // so as to trigger change in order to get initial state
        if (!shouldChange) {
            ThemedWallpaperEasyAccess.isDark = !ThemedWallpaperEasyAccess.isDark
            ThemeUtil.shouldChangeTheme4ThemedWallpaper(context, prefSettings)
        }
        return ThemedEngine()
            .also { eng -> engineRefs.add(WeakReference(eng)) }
    }

    inner class ThemedEngine: Engine() {
        private var wallpaperTimestamp = 0L
        private val handler = Handler(Looper.myLooper()!!)
        private val drawThread = Thread{
            val changed = wallpaperTimestamp != ThemedWallpaperEasyAccess.wallpaperTimestamp || updateWallpaperRes()
            wallpaperTimestamp = ThemedWallpaperEasyAccess.wallpaperTimestamp
            if (changed) {
                themedWallpaper.updateWallpaper(wallpaperDrawable)
                themedWallpaper.completeTranslate()
            }
            updateFrame(themedWallpaper.isTranslating || changed, shouldTranslate = false)
        }
        private var offsetRatio: Float = 0f
        private var offsetStepRatio: Float = 1f
        private val themedWallpaper = ThemedWallpaper(wallpaperDrawable).apply {
            setSize(surfaceHolder.surfaceFrame.width(), surfaceHolder.surfaceFrame.height())
        }

        override fun onDestroy() {
            super.onDestroy()
            cease()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) updateFrame(false, doCheckNow = true)
            else cease()
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            offsetRatio = xOffset
            offsetStepRatio = xOffsetStep
            updateFrame(true)
        }

        override fun onZoomChanged(zoom: Float) {
            super.onZoomChanged(zoom)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            themedWallpaper.setSize(width, height)
            updateFrame(true)
        }

        fun onConfigurationChanged(newConfig: Configuration) {
            updateFrame(false, doCheckNow = true)
        }

        private fun updateFrame(change: Boolean, shouldTranslate: Boolean = false, doCheckNow: Boolean = false) {
            if (change) {
                if (shouldTranslate && themedWallpaper.isTranslateCompleted) themedWallpaper.startTranslate()
                drawFrame()
            }
            cease()
            if (doCheckNow) {
                handler.post(drawThread)
            } else {
                val fg = if (themedWallpaper.isTranslating) ThemedWallpaper.FRAME_GAP else frameGap
                if (isVisible) handler.postDelayed(drawThread, fg)
            }
        }

        private fun drawFrame() {
            val holder: SurfaceHolder = surfaceHolder
            val canvas = holder.lockCanvas()
            if (themedWallpaper.isTranslating) {
                themedWallpaper.updateTranslateProgress()
                if (!themedWallpaper.isTranslateCompleted) themedWallpaper.makeMotion(this@ThemedWallpaperService)
                else themedWallpaper.destroyMotion()
            } else {
                themedWallpaper.destroyMotion()
            }
            themedWallpaper.translate(canvas, offsetRatio, offsetStepRatio)
            holder.unlockCanvasAndPost(canvas)
        }

        private fun cease(){
            handler.removeCallbacks(drawThread)
        }

        private fun updateWallpaperRes(): Boolean {
            // update theme
//        val shouldChange = ThemeUtil.shouldChangeTheme4ThemedWallpaper(applicationContext, prefSettings)
//        if (shouldChange){
//            val context = ContextThemeWrapper(applicationContext, themeId)
//            appTheme = context.theme
//            wallpaperDrawable = ThemedWallpaperEasyAccess.background?.constantState?.newDrawable()
//                    ?: ColorDrawable(if (ThemedWallpaperEasyAccess.isDark) Color.BLACK else Color.WHITE)
//        }
            val context = (if (OsUtils.satisfy(OsUtils.Q)) displayContext else applicationContext) ?: return false
            return ThemeUtil.shouldChangeTheme4ThemedWallpaper(context, prefSettings)
        }
    }
}

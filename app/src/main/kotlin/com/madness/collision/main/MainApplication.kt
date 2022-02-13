/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.main

import android.content.Intent
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.play.core.splitcompat.SplitCompatApplication
import com.madness.collision.BuildConfig
import com.madness.collision.util.X
import com.madness.collision.util.ui.AppIconFetcher
import dagger.hilt.android.HiltAndroidApp
import kotlin.math.roundToInt
import kotlin.system.exitProcess

@HiltAndroidApp
class MainApplication : SplitCompatApplication(), Thread.UncaughtExceptionHandler, ImageLoaderFactory {
    companion object {
        lateinit var INSTANCE: MainApplication
    }

    init {
        INSTANCE = this
    }

    var background: Drawable? = null
    var insetTop: Int = 0
    var insetBottom: Int = 0
    var insetStart: Int = 0
    var insetEnd: Int = 0
    var minBottomMargin: IntArray = intArrayOf(-1, -1)
    var exterior: Boolean = false
    /**
     * Having a primary color that is close to black.
     * Dark themes are tailored for use in low-light environments.
     */
    var isDarkTheme: Boolean = false
    /**
     * Having a primary color that is nearly white.
     * Pale themes use black text color, while other themes use white text color.
     */
    var isPaleTheme: Boolean = false
    var debug: Boolean = false
    var dead: Boolean = true
    var notificationAvailable = true
    var globalValue: Any? = null

    // changing application context locale will change Locale.Default and LocaleList APIs' returning value
    // in which case will not be able to obtain the system adjusted locale for this app
//    override fun attachBaseContext(newBase: Context) {
//        val context = LanguageMan(newBase).getLocaleContext()
//        super.attachBaseContext(context)
//    }

    override fun onCreate() {
        super.onCreate()
        // Setup handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler (this)
    }

    // Configure Coil
    override fun newImageLoader(): ImageLoader {
        val context = applicationContext
        val iconSIze = X.size(context, 48f, X.DP).roundToInt()
        return ImageLoader.Builder(context)
            .crossfade(true)
            .componentRegistry {
                add(AppIconFetcher(iconSIze, false, context))
            }
            .build()
    }

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        // not all Android versions will print the stack trace automatically
        e?.printStackTrace()

        val intent = Intent ()
        intent.action = BuildConfig.APPLICATION_ID + ".IMMORTALITY"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity (intent)

        exitProcess(-1) // kill off the crashed app
    }
}

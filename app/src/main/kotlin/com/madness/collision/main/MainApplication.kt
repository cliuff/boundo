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
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.play.core.splitcompat.SplitCompatApplication
import com.madness.collision.BuildConfig
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.X
import com.madness.collision.util.ui.AppIconFetcher
import com.madness.collision.util.ui.AppIconKeyer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.system.exitProcess

typealias AppAction = Pair<String, Any?>

class MainApplication : SplitCompatApplication(), Thread.UncaughtExceptionHandler, ImageLoaderFactory {
    companion object {
        lateinit var INSTANCE: MainApplication
    }

    init {
        INSTANCE = this
    }

    var insetTop: Int = 0
    var insetBottom: Int = 0
    var insetStart: Int = 0
    var insetEnd: Int = 0
    var minBottomMargin: IntArray = intArrayOf(-1, -1)
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

    private val coroutineScope = MainScope()
    private val _action: MutableSharedFlow<AppAction> = MutableSharedFlow()
    val action: Flow<AppAction> by ::_action

    fun setAction(action: AppAction) {
        coroutineScope.launch { _action.emit(action) }
    }

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
        AccessAV.initUnit(this)
    }

    // Configure Coil
    override fun newImageLoader(): ImageLoader {
        val context = applicationContext
        val iconSIze = X.size(context, 48f, X.DP).roundToInt()
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(AppIconKeyer(context))
                add(AppIconFetcher.Factory(iconSIze, false, context))
            }
            .build()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // not all Android versions will print the stack trace automatically
        e.printStackTrace()

        val intent = Intent ()
        intent.action = BuildConfig.APPLICATION_ID + ".IMMORTALITY"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        // start non-exported internal activity explicitly, required by Android 14
        intent.`package` = packageName
        startActivity (intent)

        exitProcess(-1) // kill off the crashed app
    }
}

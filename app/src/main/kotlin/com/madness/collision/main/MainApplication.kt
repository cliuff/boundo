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

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import coil3.SingletonImageLoader
import com.google.android.play.core.splitcompat.SplitCompat
import com.madness.collision.BuildConfig
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

typealias AppAction = Pair<String, Any?>

class MainApplication : Application(), Thread.UncaughtExceptionHandler, SingletonImageLoader.Factory by CoilInitializer {
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

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // access dynamic feature modules after download
        SplitCompat.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (getProcess() == "$packageName:tag_req") return
        // Setup handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler (this)
        registerActivityLifecycleCallbacks(MainLifecycleCallbacks())
        AccessAV.initUnit(this)
    }

    @SuppressLint("PrivateApi")
    private fun getProcess(): String? {
        if (OsUtils.satisfy(OsUtils.P)) return getProcessName()
        return try {
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentProcessName")
                .invoke(null) as? String
        } catch (e: ReflectiveOperationException) {
            e.printStackTrace()
            null
        }
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

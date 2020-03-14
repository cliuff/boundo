package com.madness.collision.main

import android.content.Intent
import android.graphics.drawable.Drawable
import com.google.android.play.core.splitcompat.SplitCompatApplication
import com.madness.collision.BuildConfig
import kotlin.system.exitProcess

class MainApplication : SplitCompatApplication(), Thread.UncaughtExceptionHandler {
    companion object {
        lateinit var INSTANCE: MainApplication
    }

    init {
        INSTANCE = this
    }

    var background: Drawable? = null
    var statusBarHeight: Int = 0
    var insetTop: Int = 0
    var insetBottom: Int = 0
    var insetLeft: Int = 0
    var insetRight: Int = 0
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

    override fun onCreate() {
        super.onCreate()
        // Setup handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler (this)
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

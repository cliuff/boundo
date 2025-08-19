package com.madness.collision.wearable.main

import android.app.Application
import android.content.Intent
import coil3.SingletonImageLoader
import com.madness.collision.wearable.BuildConfig
import io.cliuff.boundo.conf.CoilInitializer
import kotlin.system.exitProcess

internal class MainApplication : Application(), Thread.UncaughtExceptionHandler, SingletonImageLoader.Factory by CoilInitializer {
    companion object {
        lateinit var INSTANCE: MainApplication
    }

    init {
        INSTANCE = this
    }

    var insetBottom: Int = 0
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

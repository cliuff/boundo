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

package com.madness.collision.unit.audio_timer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.util.*
import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.notice.ToastUtils
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.appLocale
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

internal class AudioTimerService: Service() {
    companion object {
        const val ARG_DURATION = "duration"
        var isRunning = false
        private val tickCallbacks: MutableList<Callback> = mutableListOf()

        fun start(context: Context, duration: Long? = null) {
            val intent = Intent(context, AudioTimerService::class.java)
            // stop if running already
            if (isRunning) {
                context.stopService(intent)
                return
            }
            val targetDuration = if (duration == null) {
                val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
                val timeHour = pref.getInt(P.AT_TIME_HOUR, 0)
                val timeMinute = pref.getInt(P.AT_TIME_MINUTE, 0)
                (timeHour * 60 + timeMinute) * 60000L
            } else {
                duration
            }
            context.stopService(intent)
            intent.putExtra(ARG_DURATION, targetDuration)
            try {
                if (OsUtils.satisfy(OsUtils.O)) {
                    // fixme ForegroundServiceStartNotAllowedException on Android 14
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                GlobalScope.launch {
                    ToastUtils.toast(context, R.string.text_error, Toast.LENGTH_SHORT)
                }
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AudioTimerService::class.java))
        }

        fun addCallback(callback: Callback?) {
            callback ?: return
            tickCallbacks.add(callback)
        }

        fun removeCallback(callback: Callback?) {
            callback ?: return
            tickCallbacks.remove(callback)
        }
    }

    interface Callback {
        fun onTick(targetTime: Long, duration: Long, leftTime: Long)

        fun onTick(displayText: String)
    }

    private lateinit var mNotificationManager: NotificationManagerCompat
    private lateinit var localeContext: Context
    private var mHandler: Handler? = null
    private lateinit var mRunnable: Runnable
    private var targetTime: Long = 0
    private var duration: Long = 0
    private val sysTimeFormat = SimpleDateFormat("mm:ss", LocaleUtils.getApp()[0])
    private val appTimeFormat by lazy { SimpleDateFormat("mm:ss", appLocale) }
    private val notificationId = NotificationsUtil.ID_AUDIO_TIMER
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val context = this
        mNotificationManager = NotificationManagerCompat.from(context)
        localeContext = SystemUtil.getLocaleContextSys(context)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        duration = intent?.getLongExtra(ARG_DURATION, 0) ?: 0
        targetTime = System.currentTimeMillis() + duration
        val context = this
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtras(MainActivity.forItem(MyBridge.unitName))
        }
        val clickPendingIntentFlags = if (OsUtils.satisfy(OsUtils.M)) PendingIntent.FLAG_IMMUTABLE else 0
        val clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, clickPendingIntentFlags)
        val cancelIntent = Intent(context, NotificationActions::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationActions.ACTION, NotificationActions.ACTION_CANCEL)
        }
        val cancelPendingIntentFlags = if (OsUtils.satisfy(OsUtils.M)) PendingIntent.FLAG_IMMUTABLE else 0
        val cancelPendingIntent = PendingIntent.getService(context, 0, cancelIntent, cancelPendingIntentFlags)
        val color = X.getColor(context, if (ThemeUtil.getIsNight(context)) R.color.primaryABlack else R.color.primaryAWhite)
        notificationBuilder = NotificationsUtil.Builder(context, NotificationsUtil.CHANNEL_AUDIO_TIMER)
                .setSmallIcon(R.drawable.ic_timer_24)
                .setColor(color)
                .setContentTitle(localeContext.getString(R.string.unit_audio_timer))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setAutoCancel(true)
                .setContentIntent(clickPendingIntent)
                .addAction(R.drawable.ic_clear_24, localeContext.getString(R.string.text_cancel), cancelPendingIntent)
        if (OsUtils.satisfy(OsUtils.U)) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            startForeground(notificationId, notificationBuilder.build(), type)
        } else {
            startForeground(notificationId, notificationBuilder.build())
        }
        start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        isRunning = false
        mHandler?.removeCallbacks(mRunnable)
        super.onDestroy()
    }

    private fun start() {
        Thread {
            Looper.prepare()
            val handler = Handler(Looper.myLooper()!!)
            mHandler = handler
            mRunnable = runnable {
                val shouldContinue = updateStatus()
                if (shouldContinue) handler.postDelayed(this, 1000)
                else handler.removeCallbacks(this)
            }
            mRunnable.run()
            Looper.loop()
        }.start()
    }

    private val isTimeUp: Boolean
        get() = targetTime == 0L || System.currentTimeMillis() >= targetTime

    private fun updateStatus(): Boolean {
        return if (isTimeUp) {
            completeWork()
            false
        } else {
            tick()
            true
        }
    }

    private val Long.appAdapted: String
        get() = String.format(appLocale, "%d", this)

    private val Long.sysAdapted: String
        get() = String.format(LocaleUtils.getApp()[0], "%d", this)

    private fun tick() {
        val timeLeft = targetTime - System.currentTimeMillis()
        val hour = timeLeft / 3600_000
        val sysTime = sysTimeFormat.format(timeLeft)
        updateNotification(if (hour == 0L) sysTime else "${hour.sysAdapted}:$sysTime")
        // modification in other places during iterating causes exception
        try {
            tickCallbacks.forEach {
                it.onTick(targetTime, duration, timeLeft)
                val appTime = appTimeFormat.format(timeLeft)
                it.onTick(if (hour == 0L) appTime else "${hour.appAdapted}:$appTime")
            }
        } catch (e: ConcurrentModificationException) {
            e.printStackTrace()
        }
    }

    private fun updateNotification(text: String) {
        notificationBuilder.setContentText(text)
        mNotificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun completeWork() {
        stopAudio(this)
        val iterator = tickCallbacks.iterator()
        // modification in other places during iterating causes exception
        try {
            while (iterator.hasNext()) {
                val callback = iterator.next()
                iterator.remove()
                callback.onTick(targetTime, duration, 0L)
                callback.onTick("")
            }
        } catch (e: ConcurrentModificationException) {
            e.printStackTrace()
        }
        GlobalScope.launch {
            delay(500)
            stopSelf()
        }
        isRunning = false
    }

    private fun stopAudio(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager? ?: return
        val result = requestAudioFocus(am)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            notifyBriefly(R.string.text_error)
        }
    }

    private fun requestAudioFocus(audioManager: AudioManager): Int {
        return if (OsUtils.satisfy(OsUtils.O)) {
            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes).build()
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocusLegacy(null, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    @Suppress("deprecation")
    private fun AudioManager.requestAudioFocusLegacy(listener: AudioManager.OnAudioFocusChangeListener?,
                                                     streamType: Int, durationHint: Int): Int {
        return requestAudioFocus(listener, streamType, durationHint)
    }
}

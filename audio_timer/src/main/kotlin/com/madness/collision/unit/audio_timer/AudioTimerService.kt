package com.madness.collision.unit.audio_timer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import java.text.SimpleDateFormat

internal class AudioTimerService: Service() {
    companion object {
        const val ARG_DURATION = "duration"
        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, AudioTimerService::class.java)
            // stop if running already
            if (isRunning) {
                context.stopService(intent)
                return
            }
            val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            val timeHour = pref.getInt(P.AT_TIME_HOUR, 0)
            val timeMinute = pref.getInt(P.AT_TIME_MINUTE, 0)
            val targetDuration = (timeHour * 60 + timeMinute) * 60000L
            context.stopService(intent)
            intent.putExtra(ARG_DURATION, targetDuration)
            context.startService(intent)
        }
    }

    private lateinit var mNotificationManager: NotificationManagerCompat
    private lateinit var localeContext: Context
    private lateinit var mHandler:Handler
    private lateinit var mRunnable: Runnable
    private var targetTime: Long = 0
    private val timeFormat = SimpleDateFormat("mm:ss", SystemUtil.getLocaleSys())
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
        val duration = intent?.getLongExtra(ARG_DURATION, 0) ?: 0
        targetTime = System.currentTimeMillis() + duration
        val context = this
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtras(MainActivity.forItem(MyBridge.unitName))
        }
        val clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, 0)
        val cancelIntent = Intent(context, NotificationActions::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationActions.ACTION, NotificationActions.ACTION_CANCEL)
        }
        val cancelPendingIntent = PendingIntent.getService(context, 0, cancelIntent, 0)
        val color = X.getColor(context, if (ThemeUtil.getIsNight(context)) R.color.primaryABlack else R.color.primaryAWhite)
        notificationBuilder = NotificationsUtil.Builder(context, NotificationsUtil.CHANNEL_AUDIO_TIMER)
                .setSmallIcon(R.drawable.ic_music_off_24)
                .setColor(color)
                .setContentTitle(localeContext.getString(R.string.unit_audio_timer))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setAutoCancel(true)
                .setContentIntent(clickPendingIntent)
                .addAction(R.drawable.ic_clear_24, localeContext.getString(R.string.text_cancel), cancelPendingIntent)
        startForeground(notificationId, notificationBuilder.build())
        start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        isRunning = false
        mHandler.removeCallbacks(mRunnable)
        super.onDestroy()
    }

    private fun start() {
        Thread {
            Looper.prepare()
            mHandler = Handler()
            mRunnable = runnable {
                val shouldContinue = updateStatus()
                if (shouldContinue) mHandler.postDelayed(this, 1000)
                else mHandler.removeCallbacks(this)
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
            updateNotification()
            true
        }
    }

    private fun updateNotification() {
        val timeLeft = targetTime - System.currentTimeMillis()
        val re = timeFormat.format(timeLeft)
        val hour = timeLeft / 3600000
        notificationBuilder.setContentText("${if (hour == 0L) "" else "$hour:"}$re")
        mNotificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun completeWork() {
        stopAudio(this)
        mHandler.postDelayed({ stopSelf() }, 500)
        isRunning = false
    }

    private fun stopAudio(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager? ?: return
        val result = requestAudioFocus(am)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            X.toast(context, R.string.text_error, Toast.LENGTH_SHORT)
        }
    }

    private fun requestAudioFocus(audioManager: AudioManager): Int {
        return if (X.aboveOn(X.O)) {
            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .build()
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

}

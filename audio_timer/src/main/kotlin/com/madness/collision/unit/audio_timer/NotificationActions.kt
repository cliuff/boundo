package com.madness.collision.unit.audio_timer

import android.app.Service
import android.content.Intent
import android.os.IBinder

internal class NotificationActions : Service() {
    companion object{
        const val ACTION = "action"
        const val ACTION_CANCEL = "cancel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(ACTION) ?: ""
        if (action == ACTION_CANCEL){
            val context = baseContext ?: return super.onStartCommand(intent, flags, startId)
            context.stopService(Intent(context, AudioTimerService::class.java))
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}

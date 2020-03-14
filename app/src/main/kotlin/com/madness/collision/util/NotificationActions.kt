package com.madness.collision.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.madness.collision.settings.SettingsFunc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class NotificationActions : Service() {
    companion object{
        const val ACTION = "action"
        const val ACTION_APP_UPDATE = "appUpdate"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(ACTION) ?: ""
        if (action == ACTION_APP_UPDATE){
            GlobalScope.launch {
                SettingsFunc.update(this@NotificationActions)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}

/*
 * Copyright 2020 Clifford Liu
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
            AudioTimerService.stop(context)
        }
        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}

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

import android.content.Context

object AccessAT : AudioTimerAccessor by AudioTimerAccessorImpl()

interface AudioTimerAccessor {
    fun start(context: Context)
    fun start(context: Context, duration: Long)
    fun stop(context: Context)
    fun isRunning(): Boolean
    fun addCallback(callback: AtCallback?)
    fun removeCallback(callback: AtCallback?)
}

class AudioTimerAccessorImpl : AudioTimerAccessor {
    override fun start(context: Context) {
        MyBridge.timerService.start(context)
    }

    override fun start(context: Context, duration: Long) {
        MyBridge.timerService.start(context, duration)
    }

    override fun stop(context: Context) {
        MyBridge.timerService.stop(context)
    }

    override fun isRunning(): Boolean {
        return MyBridge.timerService.isRunning
    }

    override fun addCallback(callback: AtCallback?) {
        callback ?: return
        MyBridge.addCallback(callback)
    }

    override fun removeCallback(callback: AtCallback?) {
        callback ?: return
        MyBridge.removeCallback(callback)
    }
}

interface AtCallback {
    fun onTick(targetTime: Long, duration: Long, leftTime: Long)

    fun onTick(displayText: String)
}

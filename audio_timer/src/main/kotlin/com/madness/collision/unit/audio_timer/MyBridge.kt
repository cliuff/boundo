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
import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_AUDIO_TIMER
    private val tickCallbacks: MutableMap<AtCallback, AudioTimerService.Callback> = mutableMapOf()

    /**
     * @param args empty
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit()
    }

    override fun getUpdates(): UpdatesProvider? {
        return MyUpdatesProvider()
    }

    @Suppress("unused")
    fun start(context: Context) {
        AudioTimerService.start(context)
    }

    @Suppress("unused")
    fun start(context: Context, duration: Long) {
        AudioTimerService.start(context, duration)
    }

    @Suppress("unused")
    fun stop(context: Context) {
        AudioTimerService.stop(context)
    }

    @Suppress("unused")
    fun isRunning(): Boolean {
        return AudioTimerService.isRunning
    }

    @Suppress("unused")
    fun addCallback(callback: AtCallback) {
        val mCallback = object : AudioTimerService.Callback {
            override fun onTick(targetTime: Long, duration: Long, leftTime: Long) {
                callback.onTick(targetTime, duration, leftTime)
            }
            override fun onTick(displayText: String) {
                callback.onTick(displayText)
            }
        }
        tickCallbacks[callback] = mCallback
        AudioTimerService.addCallback(mCallback)
    }

    @Suppress("unused")
    fun removeCallback(callback: AtCallback) {
        AudioTimerService.removeCallback(tickCallbacks[callback])
        tickCallbacks.remove(callback)
    }
}

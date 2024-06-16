/*
 * Copyright 2023 Clifford Liu
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
import android.content.Intent
import androidx.core.content.edit
import com.madness.collision.util.P
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioTimerController(private val context: Context) {
    val isTimerRunning: Boolean get() = AudioTimerService.isRunning

    fun getTimerStatusFlow(scope: CoroutineScope): StateFlow<Boolean> {
        val statusFlow = flow {
            while (true) {
                emit(AudioTimerService.isRunning)
                // delay() is cooperative
                delay(100)
            }
        }
        val isRunning = AudioTimerService.isRunning
        return statusFlow.stateIn(scope, SharingStarted.WhileSubscribed(), isRunning)
    }

    fun getTimerRunningStatus(): Flow<String> {
        val flow = callbackFlow {
            val timerCallback = object : AudioTimerService.Callback {
                override fun onTick(targetTime: Long, duration: Long, leftTime: Long) {
                }

                override fun onTick(displayText: String) {
                    launch { send(displayText) }
                }
            }
            AudioTimerService.addCallback(timerCallback)
            awaitClose { AudioTimerService.removeCallback(timerCallback) }
        }
        return flow
    }

    fun startTimer(state: AtUiState) {
        val context = context
        val h = state.hours ?: 0
        val min = state.minutes ?: 0
        val durationMills = (h * 60 + min) * 60_000L
        val intent = Intent(context, AudioTimerService::class.java)
        context.stopService(intent)
        intent.putExtra(AudioTimerService.ARG_DURATION, durationMills)
        context.startService(intent)

        val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        pref.edit {
            if (state.hours != null) putInt(P.AT_TIME_HOUR, state.hours) else remove(P.AT_TIME_HOUR)
            if (state.minutes != null) putInt(P.AT_TIME_MINUTE, state.minutes) else remove(P.AT_TIME_MINUTE)
        }
    }

    fun stopTimer() {
        val context = context
        AudioTimerService.stop(context)
    }
}

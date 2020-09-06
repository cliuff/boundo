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
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitAccess

object AccessAT: UnitAccess(Unit.UNIT_NAME_AUDIO_TIMER) {

    fun start(context: Context) {
        getMethod("start", Context::class).invoke(context)
    }

    fun start(context: Context, duration: Long) {
        getMethod("start", Context::class, Long::class).invoke(context, duration)
    }

    fun stop(context: Context) {
        getMethod("stop", Context::class).invoke(context)
    }

    fun isRunning(): Boolean {
        val re = invokeWithoutArg("isRunning")
        if (re !is Boolean) return false
        return re
    }

    fun addCallback(callback: AtCallback?) {
        callback ?: return
        getMethod("addCallback", AtCallback::class).invoke(callback)
    }

    fun removeCallback(callback: AtCallback?) {
        callback ?: return
        getMethod("removeCallback", AtCallback::class).invoke(callback)
    }
}

interface AtCallback {
    fun onTick(targetTime: Long, duration: Long, leftTime: Long)

    fun onTick(displayText: String)
}

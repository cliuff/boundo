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

package com.madness.collision.chief.app

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.EnumMap

class LifecycleEventTime private constructor(timeMap: EnumMap<Lifecycle.Event, Long>) :
    Map<Lifecycle.Event, Long> by timeMap {

    /** [SystemClock.uptimeMillis] timestamp when the last event occurred */
    private val mutTime: EnumMap<Lifecycle.Event, Long> = timeMap
    private val eventObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) source.lifecycle.removeObserver(this)
            mutTime[event] = SystemClock.uptimeMillis()
        }
    }

    constructor() : this(timeMap = EnumMap(Lifecycle.Event::class.java))

    fun init(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(eventObserver)
    }

    fun getNotNull(key: Lifecycle.Event): Long = getOrDefault(key, -1)

    fun compareValues(key1: Lifecycle.Event, key2: Lifecycle.Event): Int {
        return getNotNull(key1).compareTo(getNotNull(key2))
    }
}

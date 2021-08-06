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

package com.madness.collision.util

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class ElapsingTime {

    companion object {
        fun elapsed(startNano: Long): Long {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano)
        }
    }

    private val start = AtomicLong()
    private val lastElapsedMills = AtomicLong()

    init {
        reset()
    }

    fun reset() {
        synchronized(start) { start.set(System.nanoTime()) }
        synchronized(lastElapsedMills) { lastElapsedMills.set(0L) }
    }

    /**
     * Elapsed time in ms.
     */
    fun elapsed(): Long {
        synchronized(lastElapsedMills) {
            lastElapsedMills.set(elapsed(start.get()))
        }
        return lastElapsedMills.get()
    }

    /**
     * Last elapsed time in ms.
     */
    fun lastElapsed(): Long {
        return lastElapsedMills.get()
    }

    /**
     * Shortcut for:
     *
     *
     * if (elapsingTime.elapsed() > mills) {
     *
     * elapsingTime.reset();
     *
     * ...
     *
     * }
     *
     */
    fun interval(mills: Long): Boolean {
        val result = elapsed() > mills
        if (result) reset()
        return result
    }
}
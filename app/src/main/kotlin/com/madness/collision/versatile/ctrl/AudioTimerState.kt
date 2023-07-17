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

package com.madness.collision.versatile.ctrl

import kotlin.math.min

interface TimerValues {
    val atMaxValue: Float
    val atStepValue: Float
    var atCurrentValue: Float

    companion object {
        operator fun invoke(max: Float, step: Float, current: Float): TimerValues {
            return object : TimerValues {
                override val atMaxValue: Float = max
                override val atStepValue: Float = step
                override var atCurrentValue: Float = current
            }
        }
    }
}

class TimerState(values: TimerValues) : TimerValues by values {
    private var isChecked = false

    fun stop() {
        atCurrentValue = atMaxValue
    }

    fun updateValues(leftTime: Long): Triple<Float, Float, Float> {
        val leftMin = (leftTime / 60000).toFloat()
        val newCurrentValue = min(leftMin, atMaxValue)
        val lastCurrentValue = atCurrentValue
        atCurrentValue = newCurrentValue

        val checked = isChecked
        if (!checked) isChecked = true
        // check real current value
        val stepValue = if (!checked) 0.02f else atStepValue  // 0.02 min -> 1200 ms
        return Triple(lastCurrentValue, newCurrentValue, stepValue)
    }
}

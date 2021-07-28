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

package com.madness.collision.unit.device_manager.list

import androidx.annotation.UiThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Wait a specified delay time to check newer state update
 * and skip intermediate state update to avoid flickering.
 * [block] will be run on main.
 */
internal class StateUpdateRegulation(
    val delay: Long, private val item: DeviceItem, @UiThread block: suspend () -> Unit
) {
    val block: suspend (regulator: StateUpdateRegulator) -> Unit = reg@{ regulator ->
        // filter out regulations of the same device
        val deviceRegs = regulator.regulations.filter { it.item.mac == item.mac }
        // skip update if there comes a different state update in specified delay time
        if (deviceRegs.size > 1 && deviceRegs.last().item.state != item.state) return@reg
        withContext(Dispatchers.Main) {
            block()
        }
    }
}

internal class StateUpdateRegulator {
    private val _regs: ArrayDeque<StateUpdateRegulation> = ArrayDeque()
    val regulations: ArrayDeque<StateUpdateRegulation>
        get() = _regs

    suspend fun regulate(regulation: StateUpdateRegulation) {
        synchronized(_regs) {
            _regs.add(regulation)
        }
        delay(regulation.delay)
        regulation.block(this@StateUpdateRegulator)
        finish(regulation)
    }

    fun finish(regulation: StateUpdateRegulation) {
        synchronized(_regs) {
            _regs.remove(regulation)
        }
    }
}

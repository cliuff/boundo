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
import androidx.lifecycle.ViewModel
import com.madness.collision.util.P
import com.madness.collision.util.ui.appContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AtUiState(
    val hours: Int?,
    val minutes: Int?,
)

class AtUnitViewModel : ViewModel() {
    private val mutUiState: MutableStateFlow<AtUiState>
    val uiState: StateFlow<AtUiState> by ::mutUiState
    init {
        val state = readPrefState()
        mutUiState = MutableStateFlow(state)
    }

    fun setHours(hours: Int?) {
        val value = hours?.takeIf { it > 0 }
        mutUiState.update { uiState.value.copy(hours = value) }
    }

    fun setMinutes(minutes: Int?) {
        val value = minutes?.takeIf { it > 0 }
        mutUiState.update { uiState.value.copy(minutes = value) }
    }

    private fun readPrefState(): AtUiState {
        val pref = appContext.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val timeHour = pref.getInt(P.AT_TIME_HOUR, -1)
        val timeMinute = pref.getInt(P.AT_TIME_MINUTE, -1)
        return AtUiState(timeHour.takeIf { it > 0 }, timeMinute.takeIf { it > 0 })
    }
}

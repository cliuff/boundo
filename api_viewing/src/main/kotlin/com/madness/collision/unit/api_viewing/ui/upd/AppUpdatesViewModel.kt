/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.upd

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias AppUpdatesUiState = Map<AppUpdatesIndex, List<*>>

class AppUpdatesViewModel : ViewModel() {
    private val mutUiState: MutableStateFlow<AppUpdatesUiState> = MutableStateFlow(emptyMap())
    val uiState: StateFlow<AppUpdatesUiState> = mutUiState.asStateFlow()
    private val updatesChecker = AppUpdatesChecker()
    private val mutexUpdatesCheck = Mutex()

    fun checkUpdates(timestamp: Long, context: Context, lifecycleOwner: LifecycleOwner) {
        if (!updatesChecker.isCheckNeeded()) return
        viewModelScope.launch(Dispatchers.IO) {
            mutexUpdatesCheck.withLock check@{
                if (!updatesChecker.isCheckNeeded()) return@check
                updatesChecker.checkNewUpdate(timestamp, context, lifecycleOwner)
                val sections = updatesChecker.getSections(15, 5, context)
                mutUiState.update { sections }
            }
        }
    }
}

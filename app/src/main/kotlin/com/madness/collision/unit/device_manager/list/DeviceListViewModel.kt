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

package com.madness.collision.unit.device_manager.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

sealed class DeviceListUiState {
    object None : DeviceListUiState()
    object PermissionGranted : DeviceListUiState()
    object PermissionDenied : DeviceListUiState()
    object BluetoothDisabled : DeviceListUiState()
    object AccessAvailable : DeviceListUiState()
}

internal class DeviceListViewModel : ViewModel() {
    val data: MutableLiveData<Pair<List<DeviceItem>, (() -> Unit)?>> = MutableLiveData()
    private val _uiState: MutableStateFlow<DeviceListUiState> = MutableStateFlow(DeviceListUiState.None)
    val uiState: StateFlow<DeviceListUiState> by ::_uiState

    fun setState(state: DeviceListUiState) {
        _uiState.update { state }
    }
}
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

package com.madness.collision.unit.api_viewing.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

internal class AppListViewModel : ViewModel() {
    // for internal access
    private val apps4DisplayInternal: MutableLiveData<List<ApiViewingApp>> = MutableLiveData(emptyList())

    // for external access
    val apps4Display: LiveData<List<ApiViewingApp>>
        get() = apps4DisplayInternal

    // shortcut for external access
    val apps4DisplayValue: List<ApiViewingApp>
        get() = apps4Display.value ?: emptyList()

    /**
     * Reserve app list to restore after filter
     */
    var reservedApps: List<ApiViewingApp>? = null
        private set

    fun updateApps4Display(list: List<ApiViewingApp>) {
        apps4DisplayInternal.value = list.toList()
    }

    fun reserveApps() {
        if (reservedApps != null) return
        reservedApps = apps4DisplayValue
    }

    fun clearReserved() {
        reservedApps = null
    }
}
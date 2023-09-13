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

package com.madness.collision.chief.auth

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.registerForActivityResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.madness.collision.chief.chiefContext

sealed interface PermissionState {
    data object Granted : PermissionState
    class Denied(val requestCount: Int) : PermissionState
    class ShowRationale(val requestCount: Int) : PermissionState
    class PermanentlyDenied(val requestCount: Int) : PermissionState

    val isGranted: Boolean get() = this == Granted
}

inline fun PermissionHandler(
    host: Fragment, permission: String,
    crossinline handle: (handler: PermissionHandler, state: PermissionState) -> Unit,
) : PermissionHandler {
    return object : PermissionHandler(host, permission) {
        override fun onHandle(state: PermissionState) {
            handle(this, state)
        }
    }
}

abstract class PermissionHandler(private val host: Fragment, private val permission: String) {
    private var requestCount: Int = 0
    private val permRequestLauncher = host.registerForActivityResult(
        ActivityResultContracts.RequestPermission(), permission) { granted ->
        val state = when {
            granted -> PermissionState.Granted
            host.shouldShowRequestPermissionRationale(permission) -> PermissionState.ShowRationale(requestCount)
            else -> PermissionState.PermanentlyDenied(requestCount)
        }
        onHandle(state)
    }

    private fun checkState(permission: String): PermissionState {
        return when {
            ContextCompat.checkSelfPermission(chiefContext, permission) ==
                    PackageManager.PERMISSION_GRANTED -> PermissionState.Granted
            host.shouldShowRequestPermissionRationale(permission) -> PermissionState.ShowRationale(requestCount)
            else -> PermissionState.Denied(requestCount)
        }
    }

    abstract fun onHandle(state: PermissionState)

    fun request() {
        requestCount++
        permRequestLauncher.launch()
    }

    fun checkState() {
        val state = checkState(permission)
        onHandle(state)
    }
}

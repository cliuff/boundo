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

package com.madness.collision.unit

class StatefulDescription(
    val unitName: String,
    val description: Description,
    /**
     * Whether is a [dynamic unit][Description].
     */
    var isDynamic: Boolean = false,
    /**
     * Whether is present on device.
     *
     * True if a [static unit][StaticDescription] or
     * installed [dynamic unit][Description], check [Unit.getInstalledUnits].
     */
    var isInstalled: Boolean = false,
    /**
     * Whether meets [unit requirement][Description.isAvailable].
     *
     * No matter [installed][isInstalled] or not.
     */
    var isAvailable: Boolean = false,
    /**
     * Whether is not [disabled][Unit.getDisabledUnits].
     *
     * Preconditions: [installed][isInstalled] and [available][isAvailable].
     */
    var isEnabled: Boolean = false,
    /**
     * Whether is [pinned][Unit.getPinnedUnits].
     *
     * Preconditions: [installed][isInstalled] and [available][isAvailable].
     */
    var isPinned: Boolean = false,
) {
    val isStatic: Boolean
        get() = !isDynamic
    val isUninstalled: Boolean
        get() = !isInstalled
    val isUnavailable: Boolean
        get() = !isAvailable
    val isDisabled: Boolean
        get() = !isEnabled
    val isNotPinned: Boolean
        get() = !isPinned

    fun updateState(newState: StatefulDescription) {
        isInstalled = newState.isInstalled
        isAvailable = newState.isAvailable
        isEnabled = newState.isEnabled
        isPinned = newState.isPinned
    }
}

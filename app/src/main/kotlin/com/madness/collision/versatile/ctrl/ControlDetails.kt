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

import android.graphics.drawable.Icon

sealed interface ControlInfo {
    val title: CharSequence
    val subtitle: CharSequence
    val icon: Icon?

    class Details(
        override val title: CharSequence,
        override val subtitle: CharSequence,
        override val icon: Icon?,
    ) : ControlInfo

    open class Status(details: Details, val status: String) : ControlInfo by details

    class ButtonStatus(
        details: Details,
        status: String,
        val isChecked: Boolean,
        val actionDesc: String,
    ) : Status(details, status)
}

typealias ControlDetails = ControlInfo.Details
typealias ControlStatus = ControlInfo.Status

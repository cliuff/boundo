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

package com.madness.collision.unit.api_viewing


class TriStateSelectable(val name: String, isSelected: Boolean? = null) {

    companion object {
        const val STATE_SELECTED = true
        const val STATE_ANTI_SELECTED = false
        val STATE_DESELECTED: Boolean? = null
    }

    var state: Boolean? = isSelected
    val isSelected: Boolean
        get() = state == STATE_SELECTED
    val isAntiSelected: Boolean
        get() = state == STATE_ANTI_SELECTED
    val isDeselected: Boolean
        get() = state == STATE_DESELECTED

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is TriStateSelectable) return false
        return state == other.state
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        return result
    }
}

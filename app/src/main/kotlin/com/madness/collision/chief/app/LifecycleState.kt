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

package com.madness.collision.chief.app

import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

@JvmInline
value class SavedStateEntry<T>(val savedState: SavedStateHandle) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any, property: KProperty<*>): T? =
        savedState[property.name]
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
        savedState.set(property.name, value)
}

/** Delegate by property. [T] must be a type that could be stored in [Bundle][android.os.Bundle]. */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> SavedStateHandle.prop() = SavedStateEntry<T>(this)

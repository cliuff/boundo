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

/** Delegate by property. Values must can be stored in [Bundle][android.os.Bundle]. */
class SavedStateDelegate(private val savedState: SavedStateHandle) : MutableMap<String, Any?> {

    override fun containsKey(key: String): Boolean = true  // required for map delegate
    override fun get(key: String): Any? = savedState[key]
    override fun put(key: String, value: Any?): Any? =
        savedState.get<Any>(key).also { savedState[key] = value }

    override val size: Int
        get() = throw NotImplementedError()
    override val keys: MutableSet<String>
        get() = throw NotImplementedError()
    override val values: MutableCollection<Any?>
        get() = throw NotImplementedError()
    override val entries: MutableSet<MutableMap.MutableEntry<String, Any?>>
        get() = throw NotImplementedError()

    override fun isEmpty(): Boolean = throw NotImplementedError()
    override fun containsValue(value: Any?): Boolean = throw NotImplementedError()
    override fun putAll(from: Map<out String, Any?>) = throw NotImplementedError()
    override fun remove(key: String): Any = throw NotImplementedError()
    override fun clear() = throw NotImplementedError()
}

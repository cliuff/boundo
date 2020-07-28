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

package com.madness.collision.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PrefsUtil {

    const val VALUE = "value"

    // no number values
    inline fun <reified K, reified V> getCompoundItem(pref: SharedPreferences, key: String) : Map<K, V> {
        val json = pref.getString(key, "")
        if (json.isNullOrEmpty()) return emptyMap()
        return Gson().fromJson(json, TypeToken.getParameterized(Map::class.java, K::class.java, V::class.java).type) ?: emptyMap()
    }

    // no number values
    inline fun <reified K, reified V> putCompoundItem(pref: SharedPreferences, key: String, data: Map<K, V>) {
        pref.edit { putString(key, Gson().toJson(data, TypeToken.getParameterized(Map::class.java, K::class.java, V::class.java).type)) }
    }

    fun removeItem(pref: SharedPreferences, key: String) {
        pref.edit { remove(key) }
    }
}

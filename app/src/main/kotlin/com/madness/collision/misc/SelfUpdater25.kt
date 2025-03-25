/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.misc

import android.content.SharedPreferences
import androidx.core.content.edit

class SelfUpdater25 : SelfUpdater {
    override val maxVersion: Int = 25032514

    override fun apply(oldVersion: Int, prefSettings: SharedPreferences) {
        // check illegal version code
        if (oldVersion < 0) return
        // use ifs instead of when to implement fallthrough
        if (oldVersion < 25032514) {
            val ids = listOf("avTagsValPkgArm32", "avTagsValPkgArm64", "avTagsValPkgX86", "avTagsValPkgX64")
            val tags = prefSettings.getStringSet("AvTags", null)
            if (tags != null && ids.any(tags::contains)) {
                val modSet = tags.toHashSet().apply { removeAll(ids); add("avTagsVal64b") }
                prefSettings.edit { putStringSet("AvTags", modSet) }
            }
        }
    }
}
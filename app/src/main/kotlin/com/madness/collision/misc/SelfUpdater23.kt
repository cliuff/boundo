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

package com.madness.collision.misc

import android.content.SharedPreferences
import androidx.core.content.edit
import com.madness.collision.unit.api_viewing.AccessAV

class SelfUpdater23 : SelfUpdater {
    override val maxVersion: Int = 24100215

    override fun apply(oldVersion: Int, prefSettings: SharedPreferences) {
        val oldVerCode = oldVersion
        // check illegal version code
        if (oldVerCode < 0) return
        // use ifs instead of when to implement fallthrough
        if (oldVerCode < 23092018) {
            AccessAV.addModOverlayTags()
        }
        if (oldVerCode < 24060815) {
            val keys = listOf("apiAPKPreload", "SDKCircularIcon", "APIPackageRoundIcon", "AVClip2Round")
            prefSettings.edit { keys.forEach(::remove) }
        }
        if (oldVerCode < 24100215) {
            prefSettings.edit { remove("AVSweet"); remove("AVIncludeDisabled") }
        }
    }
}

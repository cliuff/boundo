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

package com.madness.collision.unit.api_viewing.ui.list

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import com.madness.collision.unit.api_viewing.AccessAV

class LaunchMethod(
    val mode: Int,
    val textExtra: String? = null,
    val dataStreamExtra: PackageInfo? = null,
) {
    companion object {
        const val EXTRA_DATA_STREAM = AccessAV.EXTRA_DATA_STREAM
        const val EXTRA_LAUNCH_MODE = AccessAV.EXTRA_LAUNCH_MODE
        const val LAUNCH_MODE_NORMAL = 0
        const val LAUNCH_MODE_SEARCH = AccessAV.LAUNCH_MODE_SEARCH
        /** From url link sharing */
        const val LAUNCH_MODE_LINK = AccessAV.LAUNCH_MODE_LINK

        operator fun invoke(bundle: Bundle?): LaunchMethod {
            val mode = when {
                bundle == null -> LAUNCH_MODE_NORMAL
                // only Intent.EXTRA_TEXT is provided, EXTRA_LAUNCH_MODE is not
                bundle.containsKey(Intent.EXTRA_TEXT) -> LAUNCH_MODE_SEARCH
                // args other than launch method are provided (e.g. content padding)
                bundle.containsKey(EXTRA_LAUNCH_MODE).not() -> LAUNCH_MODE_NORMAL
                // below: from share action
                bundle.getInt(EXTRA_LAUNCH_MODE, 0) == LAUNCH_MODE_LINK -> LAUNCH_MODE_LINK
                // below: from text processing activity or text sharing
                else -> LAUNCH_MODE_SEARCH
            }
            return when (mode) {
                LAUNCH_MODE_SEARCH ->
                    LaunchMethod(mode, textExtra = bundle?.getString(Intent.EXTRA_TEXT) ?: "")
                LAUNCH_MODE_LINK ->
                    LaunchMethod(mode, dataStreamExtra = bundle?.getParcelable(EXTRA_DATA_STREAM))
                else -> LaunchMethod(mode)
            }
        }
    }
}

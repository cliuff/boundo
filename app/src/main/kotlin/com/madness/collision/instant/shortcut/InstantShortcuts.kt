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

package com.madness.collision.instant.shortcut

import android.annotation.TargetApi
import com.madness.collision.R
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.X

@TargetApi(X.N_MR1)
internal object InstantShortcuts {

    val SHORTCUTS = listOf(
            InstantShortcut(P.SC_ID_API_VIEWER, R.string.apiViewer, Unit.UNIT_NAME_API_VIEWING),
            InstantShortcut(P.SC_ID_AUDIO_TIMER, R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER),
            InstantShortcut(P.SC_ID_DEVICE_MANAGER, R.string.unit_device_manager, Unit.UNIT_NAME_DEVICE_MANAGER),
    )

}

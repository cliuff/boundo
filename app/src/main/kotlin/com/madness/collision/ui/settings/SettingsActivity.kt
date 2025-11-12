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

package com.madness.collision.ui.settings

import android.content.Intent
import android.os.Bundle
import com.madness.collision.BuildConfig
import com.madness.collision.chief.app.ComposePageActivity
import com.madness.collision.settings.SettingsRouteId

class SettingsActivity : ComposePageActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val route = SettingsRouteId.Settings.asRoute()
        intent = intent.putExtra(EXTRA_ROUTE, route)
        super.onCreate(savedInstanceState)
    }

    companion object {
        fun asNewTask(): Intent =
            Intent()
                .setClassName(BuildConfig.APPLICATION_ID, SettingsActivity::class.java.name)
                // FLAG_ACTIVITY_NEW_TASK + taskAffinity to launch in a separate task/window
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

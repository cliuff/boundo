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

package io.cliuff.boundo.wear.conf

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

interface SelfUpdater {
    val maxVersion: Int
    fun apply(oldVersion: Int, prefSettings: SharedPreferences)
}

internal object MiscMain {
    private const val APPLICATION_V = "v" // int

    fun applyAppVersionUpgrade(context: Context, prefSettings: SharedPreferences) {
        val verDefault = -1
        val verOri = prefSettings.getInt(APPLICATION_V, verDefault)
        val updaters = listOf(SelfUpdater25())
        val ver = updaters.last().maxVersion
        // below: update registered version
        prefSettings.edit { putInt(APPLICATION_V, ver) }
        // below: app gone through update process
        if (verOri == ver) return
        // below: app in update process to the newest
        updaters.forEach { updater ->
            if (verOri in 0..<updater.maxVersion) updater.apply(verOri, prefSettings)
        }
    }
}

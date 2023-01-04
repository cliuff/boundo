/*
 * Copyright 2021 Clifford Liu
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

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.ComponentActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitAccess
import com.madness.collision.util.P

object AccessAV: UnitAccess(Unit.UNIT_NAME_API_VIEWING) {

    const val EXTRA_DATA_STREAM = "apiData"
    const val EXTRA_LAUNCH_MODE = "launch mode"
    const val LAUNCH_MODE_SEARCH: Int  = 1
    /**
     * from url link sharing
     */
    const val LAUNCH_MODE_LINK: Int  = 2

    fun initUnit(context: Context) {
        getMethod("initUnit", Context::class).invoke(context)
    }

    fun clearApps(activity: ComponentActivity) {
        getMethod("clearApps", ComponentActivity::class).invoke(activity)
    }

    fun clearTags() {
        invokeWithoutArg("clearTags")
    }

    fun clearContext() {
        invokeWithoutArg("clearContext")
    }

    fun initTagSettings(
        context: Context,
        prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
    ) {
        getMethod("initTagSettings", SharedPreferences::class).invoke(prefSettings)
    }

    fun updateTagSettings(
        context: Context,
        prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
    ) {
        getMethod("updateTagSettings", SharedPreferences::class).invoke(prefSettings)
    }

    fun resolveUri(context: Context, uri: Uri): Any? {
        return getMethod("resolveUri", Context::class, Uri::class).invoke(context, uri)
    }

    fun clearRoom(context: Context) {
        getMethod("clearRoom", Context::class).invoke(context)
    }

    fun getRoomInfo(context: Context): String {
        return getMethod("getRoomInfo", Context::class).invoke(context) as String
    }

    fun nukeAppRoom(context: Context): Boolean {
        return getMethod("nukeAppRoom", Context::class).invoke(context) as Boolean
    }

}

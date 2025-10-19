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
import android.os.Parcelable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import com.madness.collision.chief.app.ComposePageRoute
import com.madness.collision.unit.Unit as ModUnit

object AccessAV : ApiViewingAccessor by SafeApiViewingAccessor() {
    const val EXTRA_DATA_STREAM = "apiData"
    const val EXTRA_LAUNCH_MODE = "launch mode"
    const val LAUNCH_MODE_SEARCH: Int = 1
    /** from url link sharing */
    const val LAUNCH_MODE_LINK: Int = 2
}

internal class SafeApiViewingAccessor : ApiViewingAccessor {
    private val ax = ModUnit.getBridge(ModUnit.UNIT_NAME_API_VIEWING)?.getAccessor() as? ApiViewingAccessor
    override fun initUnit(context: Context) = ax?.initUnit(context) ?: Unit
    override fun clearTags() = ax?.clearTags() ?: Unit
    override fun clearContext() = ax?.clearContext() ?: Unit
    override fun initTagSettings(pref: SharedPreferences) = ax?.initTagSettings(pref) ?: Unit
    override fun addModOverlayTags() = ax?.addModOverlayTags() ?: Unit
    override fun resolveUri(context: Context, uri: Uri): Any? = ax?.resolveUri(context, uri)
    override fun clearRoom(context: Context) = ax?.clearRoom(context) ?: Unit
    override fun getRoomInfo(context: Context): String = ax?.getRoomInfo(context).orEmpty()
    override fun nukeAppRoom(context: Context): Boolean = ax?.nukeAppRoom(context) ?: false
    override fun getHomeFragment(): Fragment = ax?.getHomeFragment() ?: error("NPE!")
    override fun getAppListRoute(query: CharSequence?, pkgInfo: Parcelable?): ComposePageRoute =
        ax?.getAppListRoute(query, pkgInfo) ?: error("NPE!")
    override fun getPrefsRoute(): ComposePageRoute = ax?.getPrefsRoute() ?: error("NPE!")
    @Composable override fun Prefs(contentPadding: PaddingValues) = ax?.Prefs(contentPadding) ?: Unit
}

interface ApiViewingAccessor {
    fun initUnit(context: Context)
    fun clearTags()
    fun clearContext()
    fun initTagSettings(pref: SharedPreferences)
    fun addModOverlayTags()
    fun resolveUri(context: Context, uri: Uri): Any?
    fun clearRoom(context: Context)
    fun getRoomInfo(context: Context): String
    fun nukeAppRoom(context: Context): Boolean
    fun getHomeFragment(): Fragment
    fun getAppListRoute(query: CharSequence? = null, pkgInfo: Parcelable? = null): ComposePageRoute
    fun getPrefsRoute(): ComposePageRoute
    @Composable fun Prefs(contentPadding: PaddingValues)
}

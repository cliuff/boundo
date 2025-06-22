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

package com.madness.collision.unit.api_viewing.ui.home

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.madness.collision.chief.app.ComposePageRoute
import kotlinx.parcelize.Parcelize

@Parcelize
class AppListNavRoute(private val routeId: AppListRouteId) : ComposePageRoute {

    @Composable
    @NonRestartableComposable
    override fun content() = routeId.RouteContent()
}

interface RouteId<R : ComposePageRoute> {
    fun asRoute(): R
}

sealed interface AppListRouteId : RouteId<AppListNavRoute>, Parcelable {

    override fun asRoute(): AppListNavRoute = AppListNavRoute(this)

    @Parcelize
    class AppQuery(val text: CharSequence) : AppListRouteId

    @Parcelize
    class ApkInfo(val data: Parcelable) : AppListRouteId
}

@Composable
private fun AppListRouteId.RouteContent(): Unit =
    when (this) {
        is AppListRouteId.AppQuery -> {
            StandaloneAppList(query = text) {}
        }
        is AppListRouteId.ApkInfo -> {
            StandaloneAppList(pkgInfo = data) {}
        }
    }

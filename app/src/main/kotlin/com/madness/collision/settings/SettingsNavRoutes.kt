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

package com.madness.collision.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.madness.collision.chief.app.ComposePageRoute
import com.madness.collision.ui.settings.LanguagesPage
import com.madness.collision.ui.settings.StylesPage
import kotlinx.parcelize.Parcelize

@Parcelize
class SettingsNavRoute(private val routeId: SettingsRouteId) : ComposePageRoute {

    @Composable
    @NonRestartableComposable
    override fun content() = routeId.RouteContent()
}

interface RouteId<R : ComposePageRoute> {
    fun asRoute(): R
}

enum class SettingsRouteId : RouteId<SettingsNavRoute> {

    Settings,
    Styles,
    Languages,
    About,
    OssLibraries;

    override fun asRoute(): SettingsNavRoute = SettingsNavRoute(this)
}

@Composable
private fun SettingsRouteId.RouteContent(): Unit =
    when (this) {
        SettingsRouteId.Settings -> {
            SettingsPage()
        }
        SettingsRouteId.Styles -> {
            StylesPage()
        }
        SettingsRouteId.Languages -> {
            LanguagesPage()
        }
        SettingsRouteId.About -> {
            AboutPage()
        }
        SettingsRouteId.OssLibraries -> {
            OssLibsPage()
        }
    }

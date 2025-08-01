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

package com.madness.collision.unit.api_viewing.ui.upd.item

import androidx.compose.runtime.Immutable
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.util.ui.PackageInfo

/** Graphical UI Artifact for the UI layer. */
@Immutable
internal sealed interface GuiArt {
    val identity: Identity

    @Immutable
    data class Identity(
        /**
         * The unique ID to identify among arts. The [packageName] becomes insufficient
         * in cases where multiple versions or types of the same package co-exist.
         */
        val uid: String,
        val packageName: String,
        val label: String,
        val iconPkgInfo: PackageInfo,
    )

    @Immutable
    data class App(
        override val identity: Identity,
        val apiInfo: VerInfo,
        val updateTime: Long,
    ) : GuiArt

    @Immutable
    data class VerUpdate(
        override val identity: Identity,
        val apiInfo: VerInfo,
        val oldVersion: AppInstallVersion,
        val newVersion: AppInstallVersion,
    ) : GuiArt

    @Immutable
    data class ApiUpdate(
        override val identity: Identity,
        val oldApiInfo: VerInfo,
        val newApiInfo: VerInfo,
        val oldVersion: AppInstallVersion,
        val newVersion: AppInstallVersion,
    ) : GuiArt
}

@Immutable
data class AppInstallVersion(val code: Long, val name: String?, val time: String)

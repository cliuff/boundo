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
import com.madness.collision.unit.api_viewing.info.ExpTag
import com.madness.collision.util.ui.PackageInfo
import kotlinx.coroutines.flow.Flow

/** Graphical UI Artifact for the UI layer. */
@Immutable
internal interface GuiArt {
    val identity: Identity
    val expTags: Flow<List<ExpTag>>
    val onClick: () -> Unit

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
}

@Immutable
internal data class GuiArtImpl(
    override val identity: GuiArt.Identity,
    override val expTags: Flow<List<ExpTag>>,
    override val onClick: () -> Unit,
) : GuiArt

/** Update Graphical UI Artifact */
@Immutable
internal data class UpdGuiArt(
    val art: GuiArt,
    val apiInfo: VerInfo,
    val updateTime: String,
) : GuiArt by art

@Immutable
internal data class VerUpdGuiArt(
    val art: GuiArt,
    val apiInfo: VerInfo,
    val oldVersion: AppInstallVersion,
    val newVersion: AppInstallVersion,
): GuiArt by art

@Immutable
internal data class ApiUpdGuiArt(
    val art: GuiArt,
    val oldApiInfo: VerInfo,
    val newApiInfo: VerInfo,
    val oldVersion: AppInstallVersion,
    val newVersion: AppInstallVersion,
): GuiArt by art

@Immutable
data class AppInstallVersion(val code: Long, val name: String?, val time: String)

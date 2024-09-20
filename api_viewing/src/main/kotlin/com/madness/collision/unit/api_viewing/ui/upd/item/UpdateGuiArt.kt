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

/** Update Graphical UI Artifact */
@Immutable
internal data class UpdGuiArt(
    val packageName: String,
    val label: String,
    val iconPkgInfo: PackageInfo,
    val updateTime: String,
    val apiInfo: VerInfo,
    val expTags: Flow<List<ExpTag>>,
    val onClick: () -> Unit,
)

@Immutable
internal data class ApiUpdGuiArt(
    val packageName: String,
    val label: String,
    val iconPkgInfo: PackageInfo,
    val expTags: Flow<List<ExpTag>>,
    val onClick: () -> Unit,
    val oldApiInfo: VerInfo,
    val newApiInfo: VerInfo,
    val oldVersion: AppInstallVersion,
    val newVersion: AppInstallVersion,
)

@Immutable
data class AppInstallVersion(val code: Long, val name: String?, val time: String)

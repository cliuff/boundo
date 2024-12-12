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

package io.cliuff.boundo.org.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CollInfo(
    val id: Int,
    val name: String,
    val groupCount: Int,
) : Parcelable

/** Composite collection. */
@Parcelize
data class CompColl(
    val id: Int,
    val name: String,
    val groups: List<OrgGroup>,
) : Parcelable
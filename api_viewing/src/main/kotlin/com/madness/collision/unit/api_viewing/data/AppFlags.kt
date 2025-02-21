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

package com.madness.collision.unit.api_viewing.data

@JvmInline
value class DexPackageFlags(val value: Int) {
    val isDefined: Boolean
        get() = value != UNDEFINED
    val isJetpackCompose: Boolean
        get() = isDefined && (value and JETPACK_COMPOSE != 0)
    val isComposeMultiplatform: Boolean
        get() = isDefined && (value and COMPOSE_MULTIPLATFORM != 0)

    companion object {
        // binary value of -1 is all ones (i.e. ~0)
        const val UNDEFINED: Int = -1
        const val NONE: Int = 0
        const val JETPACK_COMPOSE: Int = 1 shl 0
        const val COMPOSE_MULTIPLATFORM: Int = 1 shl 1

        fun from(vararg flag: Int): DexPackageFlags {
            if (flag.isEmpty()) return DexPackageFlags(NONE)
            return DexPackageFlags(flag.reduce { acc, i -> acc or i })
        }
    }
}

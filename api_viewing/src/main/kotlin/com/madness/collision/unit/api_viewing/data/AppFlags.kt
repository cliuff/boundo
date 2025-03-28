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
value class ArchiveEntryFlags private constructor(val value: Int) {
    val isDefined: Boolean
        get() = value != UNDEFINED
    val rev: Int
        get() = if (isDefined) value and 0b11111 else -1
    val bits: Int
        get() = if (isDefined) value ushr REV_BITS else BIT_NONE
    val isValidRev: Boolean
        get() = rev == REV

    operator fun contains(bit: Int): Boolean {
        return isDefined && (bits and bit != 0)
    }

    companion object {
        // binary value of -1 is all ones (i.e. ~0)
        const val UNDEFINED: Int = -1
        /** The number of bits used as revision number. */
        const val REV_BITS: Int = 5
        /** The maximum revision number. Number of all ones is reserved. */
        const val REV_MAX: Int = 0b11110

        /* The current revision. Increment on changing bits. */
        const val REV: Int = 2

        const val BIT_NONE: Int = 0
        const val BIT_KOTLIN: Int = 1 shl 0
        const val BIT_NATIVE_LIBS_64B: Int = 1 shl 1
        const val BIT_LIB_FLUTTER: Int = 1 shl 2
        const val BIT_LIB_REACT_NATIVE: Int = 1 shl 3
        const val BIT_LIB_XAMARIN: Int = 1 shl 4
        const val BIT_LIB_MAUI: Int = 1 shl 5
        const val BIT_MAX: Int = 1 shl 26

        val Undefined: ArchiveEntryFlags = ArchiveEntryFlags(UNDEFINED)

        fun of(value: Int): ArchiveEntryFlags {
            return ArchiveEntryFlags(value)
        }

        fun from(vararg flagBits: Int): ArchiveEntryFlags {
            val bits = if (flagBits.isNotEmpty()) flagBits.reduce { acc, i -> acc or i } else 0
            return ArchiveEntryFlags((bits shl REV_BITS) + REV)
        }
    }
}

@JvmInline
value class DexPackageFlags private constructor(val value: Int) {
    val isDefined: Boolean
        get() = value != UNDEFINED
    val rev: Int
        get() = if (isDefined) value and 0b11111 else -1
    val bits: Int
        get() = if (isDefined) value ushr REV_BITS else BIT_NONE
    val isValidRev: Boolean
        get() = rev == REV

    operator fun contains(bit: Int): Boolean {
        return isDefined && (bits and bit != 0)
    }

    companion object {
        // binary value of -1 is all ones (i.e. ~0)
        const val UNDEFINED: Int = -1
        /** The number of bits used as revision number. */
        const val REV_BITS: Int = 5
        /** The maximum revision number. Number of all ones is reserved. */
        const val REV_MAX: Int = 0b11110

        /* The current revision. Increment on changing bits. */
        const val REV: Int = 4

        const val BIT_NONE: Int = 0
        const val BIT_KOTLIN: Int = 1 shl 0
        const val BIT_JETPACK_COMPOSE: Int = 1 shl 1
        const val BIT_COMPOSE_MULTIPLATFORM: Int = 1 shl 2
        const val BIT_MAUI: Int = 1 shl 3
        const val BIT_MAX: Int = 1 shl 26

        val Undefined: DexPackageFlags = DexPackageFlags(UNDEFINED)

        fun of(value: Int): DexPackageFlags {
            return DexPackageFlags(value)
        }

        fun from(vararg flagBits: Int): DexPackageFlags {
            val bits = if (flagBits.isNotEmpty()) flagBits.reduce { acc, i -> acc or i } else 0
            return DexPackageFlags((bits shl REV_BITS) + REV)
        }
    }
}

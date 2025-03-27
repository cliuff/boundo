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

package com.madness.collision.unit.api_viewing.info

import android.os.Build
import java.io.File
import java.util.zip.ZipFile

object ArchiveFlags {
    fun getEntryFlags(path: String): BooleanArray {
        return checkEntryFlags(File(path))
    }
}

private fun checkEntryFlags(file: File): BooleanArray {
    val kotlinEntryNames = arrayOf("kotlin-tooling-metadata.json", "kotlin/kotlin.kotlin_builtins")
    val libNameSet = linkedSetOf("flutter", "reactnativejni", "xamarin-app")
    // Android ABIs, native code in app packages: /lib/<abi>/lib<name>.so
    val libEntryRegex = """lib/([\w-]+)/lib(.+)\.so""".toRegex()

    val systemAbiSet = Build.SUPPORTED_ABIS.toSet()
    val libAbiCheck = BooleanArray(systemAbiSet.size)
    val libNameCheck = BooleanArray(libNameSet.size)
    var hasKotlin = false
    var isLibAbiAllCheck = false
    var isLibNameAllCheck = false

    runCatching {
        ZipFile(file).use { zip ->
            for (entry in zip.entries()) {
                if (!hasKotlin) hasKotlin = kotlinEntryNames.any(entry.name::equals)
                if (hasKotlin && isLibAbiAllCheck && isLibNameAllCheck) break
                if (!entry.name.startsWith("lib/")) continue
                // match entry with system ABIs and lib names
                libEntryRegex.matchEntire(entry.name)?.let { match ->
                    val (abi, lib) = match.destructured
                    val abiIdx = systemAbiSet.indexOf(abi)
                    if (abiIdx >= 0) {
                        val libIdx = libNameSet.indexOf(lib)
                        if (!libAbiCheck[abiIdx]) libAbiCheck[abiIdx] = true
                        if (libIdx >= 0 && !libNameCheck[libIdx]) libNameCheck[libIdx] = true
                        isLibAbiAllCheck = libAbiCheck.all { it }
                        isLibNameAllCheck = libNameCheck.all { it }
                    }
                }
            }
        }
    }
        .onFailure(Throwable::printStackTrace)
        .onFailure { return BooleanArray(2 + libNameSet.size) }

    val abiSet64 = Build.SUPPORTED_64_BIT_ABIS.toSet()
    val abiSet32 = Build.SUPPORTED_32_BIT_ABIS.toSet()
    val lib64 = systemAbiSet.withIndex().any { (i, abi) -> abi in abiSet64 && libAbiCheck[i] }
    val lib32 = systemAbiSet.withIndex().any { (i, abi) -> abi in abiSet32 && libAbiCheck[i] }
    return booleanArrayOf(hasKotlin, !lib32 || lib64) + libNameCheck
}

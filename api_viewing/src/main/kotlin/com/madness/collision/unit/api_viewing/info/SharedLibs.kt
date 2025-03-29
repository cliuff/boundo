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

import java.io.File
import java.util.zip.ZipFile

object SharedLibs {
    fun getNativeLibAbiSet(file: File): Set<String> {
        return getApkNativeLibAbiSet(file)
    }

    fun getNativeLibs(file: File): List<Triple<String, Long, Long>> {
        return getApkNativeLibs(file)
    }
}

private fun getApkNativeLibAbiSet(file: File): Set<String> {
    // Android ABIs, native code in app packages: /lib/<abi>/lib<name>.so
    val libEntryRegex = """lib/([\w-]+)/lib.+\.so""".toRegex()
    val abiSet = mutableSetOf<String>()
    runCatching {
        ZipFile(file).use { zip ->
            for (entry in zip.entries()) {
                if (!entry.name.startsWith("lib/")) continue
                val match = libEntryRegex.matchEntire(entry.name)
                if (match != null) abiSet.add(match.groupValues[1])
            }
        }
    }.onFailure(Throwable::printStackTrace)
    return abiSet
}

private fun getApkNativeLibs(file: File): List<Triple<String, Long, Long>> {
    // Android ABIs, native code in app packages: /lib/<abi>/lib<name>.so
    val libEntryRegex = """lib/[\w-]+/lib.+\.so""".toRegex()
    val libList = arrayListOf<Triple<String, Long, Long>>()
    runCatching {
        ZipFile(file).use { zip ->
            for (entry in zip.entries()) {
                if (!entry.name.startsWith("lib/")) continue
                if (entry.name.matches(libEntryRegex)) {
                    val item = Triple(entry.name, entry.compressedSize, entry.size)
                    libList.add(item)
                }
            }
        }
    }.onFailure(Throwable::printStackTrace)
    return libList
}

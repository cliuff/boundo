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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.SharedLibraryInfo
import com.madness.collision.misc.PackageCompat
import com.madness.collision.util.os.OsUtils
import java.io.File
import java.util.zip.ZipFile

object SharedLibs {
    fun getNativeLibAbiSet(file: File): Set<String> {
        return getApkNativeLibAbiSet(file)
    }

    fun getNativeLibs(file: File): List<Triple<String, Long, Long>> {
        return getApkNativeLibs(file)
    }

    fun getAppSharedLibs(appInfo: ApplicationInfo, pkgMgr: PackageManager): Map<String, String> {
        return getAppSharedLibFiles(appInfo, pkgMgr)
    }

    fun getSystemSharedLibs(context: Context): Map<String, String?> {
        return getSharedLibInfos(context)
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

/**
 * @param appInfo acquire with [PackageManager.GET_SHARED_LIBRARY_FILES] flag.
 * @return a map of lib name to file path.
 */
private fun getAppSharedLibFiles(appInfo: ApplicationInfo, pkgMgr: PackageManager): Map<String, String> {
    val linkedLibPaths = appInfo.sharedLibraryFiles.orEmpty()
    return linkedLibPaths.associate { path ->
        val filename = File(path).name
        // lib's file name w/o extension as key (except native ones)
        val extIndex = filename.lastIndexOf('.')
        if (extIndex > 0) {
            val extension = filename.substring(extIndex + 1)
            val name = when (extension) {
                // native lib name preserves extension
                "so" -> filename
                // apk libs, e.g. trichrome library
                "apk" -> {
                    runCatching { PackageCompat.getArchivePackage(pkgMgr, path) }
                        .onFailure(Throwable::printStackTrace)
                        .fold({ it?.packageName ?: filename }, { filename })
                }
                // TODO use sharedLibraryInfos instead to determine lib name.
                //  AI Service Engine app (com.oplus.aiunit), ColorOS 14.0
                //  filename: vendor.oplus.hardware.cryptoeng-V1.0-java.jar
                //  lib name: vendor-oplus-hardware-cryptoeng-V1.0
                "jar" -> filename.substring(0, extIndex)
                else -> filename.substring(0, extIndex)
            }
            name to path
        } else {
            filename to path
        }
    }
}

/**
 * @return a map of lib name to its nullable type.
 */
private fun getSharedLibInfos(context: Context): Map<String, String?> {
    val systemLibNames = context.packageManager.systemSharedLibraryNames.orEmpty()
    if (OsUtils.dissatisfy(OsUtils.O)) return systemLibNames.associateWith { null }

    val deviceLibs = context.packageManager.getSharedLibraries(0)
    val devLibSet = deviceLibs.mapTo(HashSet(deviceLibs.size), SharedLibraryInfo::getName)
    val xLibNames = systemLibNames.filterNot(devLibSet::contains)
    return buildMap(deviceLibs.size + xLibNames.size) {
        for (lib in deviceLibs) {
            val type = when (lib.type) {
                SharedLibraryInfo.TYPE_BUILTIN -> "builtin"
                SharedLibraryInfo.TYPE_DYNAMIC -> "dynamic"
                SharedLibraryInfo.TYPE_STATIC -> "static"
                SharedLibraryInfo.TYPE_SDK_PACKAGE -> "sdk"
                else -> null
            }
            put(lib.name, type)
        }
        for (name in xLibNames) {
            put(name, null)
        }
    }
}

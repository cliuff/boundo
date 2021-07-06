/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.util

import android.content.res.Resources
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApkUtil {

    const val NATIVE_LIB_SUPPORT_SIZE = 8

    fun getNativeLibSupport(path: String): BooleanArray {
        return getNativeLibSupport(File(path))
    }

    fun getNativeLibSupport(file: File): BooleanArray {
        val libFlutter = "libflutter.so"
        val libReactNative = "libreactnativejni.so"
        val libXamarin = "libxamarin-app.so"

        val libDirs = arrayOf(
            arrayOf("lib/armeabi-v7a/", "lib/armeabi/",), arrayOf("lib/arm64-v8a/"),
            arrayOf("lib/x86/"), arrayOf("lib/x86_64/")
        )
        val libDirCheck = BooleanArray(4) { false }

        var hasFlutter = false
        var hasReactNative = false
        var hasXamarin = false
        val itemKotlin = "kotlin/kotlin.kotlin_builtins"
        var hasKotlin = false

        iterateFile(file) { entry ->
            val name = entry.name
            var isAnyLibDirDetected = false
            var isAllLibDirDetected = true
            for (i in libDirCheck.indices) {
                if (libDirCheck[i].not()) {
                    libDirCheck[i] = entry.isDirectory.not() && libDirs[i].any { name.startsWith(it) }
                }
                isAnyLibDirDetected = isAnyLibDirDetected || libDirCheck[i]
                isAllLibDirDetected = isAllLibDirDetected && libDirCheck[i]
            }
            if (isAnyLibDirDetected) {
                if (!hasFlutter) hasFlutter = name.endsWith(libFlutter)
                if (!hasReactNative) hasReactNative = name.endsWith(libReactNative)
                if (!hasXamarin) hasXamarin = name.endsWith(libXamarin)
            }
            if (!hasKotlin) hasKotlin = name == itemKotlin
            !(isAllLibDirDetected && hasFlutter && hasReactNative && hasXamarin && hasKotlin)
        }?.let {
            it.printStackTrace()
            return BooleanArray(NATIVE_LIB_SUPPORT_SIZE) { false }
        }
        return booleanArrayOf(
            libDirCheck[0], libDirCheck[1], libDirCheck[2], libDirCheck[3],
            hasFlutter, hasReactNative, hasXamarin, hasKotlin
        )
    }

    fun getResourceEntries(resources: Resources, resId: Int, path: String): Pair<String, List<String>> {
        return getResourceEntries(resources, resId, File(path))
    }

    /**
     * R.mipmap.ic_launcher
     *
     * res/mipmap-anydpi-v26/ic_launcher.xml
     * res/mipmap-xxxhdpi/ic_launcher.png
     */
    fun getResourceEntries(resources: Resources, resId: Int, file: File): Pair<String, List<String>> {
        val type = resources.getResourceTypeName(resId)
        val targetEntry = resources.getResourceEntryName(resId)
        val dirPrefix = "res/$type"
        val dirPattern = "${dirPrefix}(-[a-z\\d]+)*/"
        val resultList = mutableListOf<String>()
        readFile(file) { zip ->
            val iterator = zip.entries().iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val name = entry.name
                if (!name.startsWith(dirPrefix)) continue
                val namePattern = "$dirPattern$targetEntry\\.((?!\\.).)+".toRegex()
                if (!name.matches(namePattern)) continue
                resultList.add(name)
            }
        }?.printStackTrace()
        return "R.$type.$targetEntry" to resultList
    }

    fun getItem(file: File, name: String): Boolean {
        var hasTheItem = false
        readFile(file) {
            hasTheItem = it.getEntry(name) != null
        }?.let {
            it.printStackTrace()
            return false
        }
        return hasTheItem
    }

    fun readFile(file: File, operation: (ZipFile) -> Unit): Throwable? {
        if (file.exists().not()) return RuntimeException("File ${file.path} does not exist")
        try {
            ZipFile(file).use(operation)
        } catch (e: Throwable) {
            return e
        }
        return null
    }

    fun iterateFile(file: File, operation: (ZipEntry) -> Boolean): Throwable? = readFile(file) { zip ->
        val iterator = zip.entries().iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (!operation.invoke(e)) break
        }
    }
}
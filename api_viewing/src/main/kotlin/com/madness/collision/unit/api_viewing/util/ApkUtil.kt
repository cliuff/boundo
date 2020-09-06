/*
 * Copyright 2020 Clifford Liu
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

import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

object ApkUtil {

    const val NATIVE_LIB_SUPPORT_SIZE = 8

    fun getNativeLibSupport(path: String): BooleanArray {
        return getNativeLibSupport(File(path))
    }

    fun getNativeLibSupport(file: File): BooleanArray {
        val libFlutter = "libflutter.so"
        val libReactNative = "libreactnativejni.so"
        val libXamarin = "libxamarin-app.so"
        val dirArm = "lib/armeabi-v7a/"
        val dirArmeabi = "lib/armeabi/"
        val dirArm64 = "lib/arm64-v8a/"
        val dirX86 = "lib/x86/"
        val dirX8664 = "lib/x86_64/"
        var hasArm = false
        var hasArm64 = false
        var hasX86 = false
        var hasX8664 = false
        var hasFlutter = false
        var hasReactNative = false
        var hasXamarin = false
        val itemKotlin = "kotlin/kotlin.kotlin_builtins"
        var hasKotlin = false
        iterateFile(file) { entry ->
            val name = entry.name
            if (!hasArm) hasArm = name.startsWith(dirArm) || name.startsWith(dirArmeabi)
            if (!hasArm64) hasArm64 = name.startsWith(dirArm64)
            if (!hasX86) hasX86 = name.startsWith(dirX86)
            if (!hasX8664) hasX8664 = name.startsWith(dirX8664)
            if (hasArm || hasArm64 || hasX86 || hasX8664) {
                if (!hasFlutter) hasFlutter = name.endsWith(libFlutter)
                if (!hasReactNative) hasReactNative = name.endsWith(libReactNative)
                if (!hasXamarin) hasXamarin = name.endsWith(libXamarin)
            }
            if (!hasKotlin) hasKotlin = name == itemKotlin
            !(hasArm && hasArm64 && hasX86 && hasX8664 && hasFlutter && hasReactNative && hasXamarin && hasKotlin)
        }?.let {
            it.printStackTrace()
            return BooleanArray(NATIVE_LIB_SUPPORT_SIZE) { false }
        }
        return booleanArrayOf(hasArm, hasArm64, hasX86, hasX8664, hasFlutter, hasReactNative, hasXamarin, hasKotlin)
    }

    fun getItem(file: File, name: String): Boolean {
        var hasTheItem = false
        iterateFile(file) { entry ->
            hasTheItem = entry.name == name
            !hasTheItem
        }?.let {
            it.printStackTrace()
            return false
        }
        return hasTheItem
    }

    fun iterateFile(file: File, operation: (JarEntry) -> Boolean): Throwable? {
        try {
            JarFile(file).use { jar ->
                val iterator = jar.entries().iterator()
                while (iterator.hasNext()) {
                    val e = iterator.next()
                    if (!operation.invoke(e)) break
                }
            }
        } catch (e: Throwable) {
            return e
        }
        return null
    }
}
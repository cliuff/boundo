package com.madness.collision.util

import java.io.File
import java.util.jar.JarFile

object ApkUtil {

    fun getNativeLibSupport(path: String): BooleanArray = getNativeLibSupport(File(path))

    fun getNativeLibSupport(file: File): BooleanArray {
        return try {
            JarFile(file).run {
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
                val iterator = entries().iterator()
                while (iterator.hasNext()){
                    val e = iterator.next()
                    val name = e.name
                    if (!hasArm) hasArm = name.startsWith(dirArm) || name.startsWith(dirArmeabi)
                    if (!hasArm64) hasArm64 = name.startsWith(dirArm64)
                    if (!hasX86) hasX86 = name.startsWith(dirX86)
                    if (!hasX8664) hasX8664 = name.startsWith(dirX8664)
                    if (hasArm || hasArm64 || hasX86 || hasX8664) {
                        if (!hasFlutter) hasFlutter = name.endsWith(libFlutter)
                        if (!hasReactNative) hasReactNative = name.endsWith(libReactNative)
                        if (!hasXamarin) hasXamarin = name.endsWith(libXamarin)
                    }
                    if (hasArm && hasArm64 && hasX86 && hasX8664) break
                }
                booleanArrayOf(hasArm, hasArm64, hasX86, hasX8664, hasFlutter, hasReactNative, hasXamarin)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            booleanArrayOf(false, false, false, false, false, false, false)
        }
    }
}

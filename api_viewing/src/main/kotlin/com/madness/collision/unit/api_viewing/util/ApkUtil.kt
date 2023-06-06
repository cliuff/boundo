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
import android.util.Log
import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.traverse.ApkTraverse
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

    fun getNativeLibs(file: File): List<Triple<String, Long, Long>> {
        val libDirs = listOf("lib/armeabi-v7a/", "lib/armeabi/", "lib/arm64-v8a/", "lib/x86/", "lib/x86_64/")
        val libList = arrayListOf<Triple<String, Long, Long>>()
        iterateFile(file) { entry ->
            if (entry.isDirectory.not() && libDirs.any { entry.name.startsWith(it) }) {
                val item = Triple(entry.name, entry.compressedSize, entry.size)
                libList.add(item)
            }
            true
        }
        return libList
    }

    fun getResourceEntries(resources: Resources, resId: Int, path: String): Pair<String, List<String>> {
        return getResourceEntries(resources, resId, File(path))
    }

    /**
     * R.mipmap.ic_launcher
     */
    fun getResourceEntryName(resources: Resources, resId: Int): Pair<String, String> {
        val type = resources.getResourceTypeName(resId)
        val targetEntry = resources.getResourceEntryName(resId)
        return type to targetEntry
    }

    /**
     * R.mipmap.ic_launcher
     *
     * res/mipmap-anydpi-v26/ic_launcher.xml
     * res/mipmap-xxxhdpi/ic_launcher.png
     */
    fun getResourceEntries(resources: Resources, resId: Int, file: File): Pair<String, List<String>> {
        val (type, targetEntry) = getResourceEntryName(resources, resId)
        val dirPrefix = "res/$type"
        val dirPattern = "${dirPrefix}(-[a-z\\d]+)*/"
        val namePattern = "$dirPattern$targetEntry\\.((?!\\.).)+".toRegex()
        val resultList = mutableListOf<String>()
        readFile(file) { zip ->
            val iterator = zip.entries().iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val name = entry.name
                if (!name.startsWith(dirPrefix)) continue
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

    fun getThirdPartyPkg(path: String, ownPkg: String): List<String> {
        return getThirdPartyPkg(File(path), ownPkg)
    }

    fun getThirdPartyPkg(file: File, ownPkg: String, removeInternals: Boolean = true): List<String> {
        return loadThirdPartyPkg(file, ownPkg) { pkgList ->
            val pkgFilter = ThirdPartyPkgFilter(ownPkg, removeInternals)
            val rewriteReverser = R8RewriteReverser()
            sequence { pkgList.forEach { yieldAll(it.iterator()) } }
                .filterNotNull().mapNotNull(pkgFilter::map).map(rewriteReverser::map)
                .toMutableSet().toList()  // Sequence.distinct() seems inefficient
        } ?: emptyList()
    }

    data class PkgPartitions(val filtered: List<String>, val self: List<String>, val minimized: List<String>)

    fun getThirdPartyPkgPartitions(path: String, ownPkg: String): PkgPartitions {
        return getThirdPartyPkgPartitions(File(path), ownPkg)
    }

    fun getThirdPartyPkgPartitions(file: File, ownPkg: String, removeInternals: Boolean = true): PkgPartitions {
        return loadThirdPartyPkg(file, ownPkg) { pkgList ->
            val pkgFilter = ThirdPartyPkgFilter(ownPkg, removeInternals)
            val rewriteReverser = R8RewriteReverser()
            val transforms = mutableSetOf<String>()
            val pkgSequence = sequence { pkgList.forEach { yieldAll(it.iterator()) } }
                .filterNotNull().map(rewriteReverser::map)
            val filtered = ArrayList<String>()
            val self = ArrayList<String>()
            val out = ArrayList<String>()
            pkgSequence.forEach { pkg ->
                val fOwn = pkgFilter.mapOwnPkg(pkg)
                if (fOwn != pkg) {
                    self.add(pkg)
                } else {
                    val f = pkgFilter.mapObfuscated(pkg)
                    if (f != null && f != pkg) transforms.add(f)
                    (if (f == pkg) filtered else out).add(pkg)
                }
            }
            val first = if (transforms.isNotEmpty()) filtered + transforms else filtered
            PkgPartitions(first.distinct(), self.distinct(), out.distinct())
        } ?: PkgPartitions(emptyList(), emptyList(), emptyList())
    }

    private fun <T> loadThirdPartyPkg(file: File, ownPkg: String, block: (List<List<String?>>) -> T): T? {
        return try {
            ApkFile(file).use { apk ->
                val dexPackageList = mutableListOf<List<String?>>()
                ApkTraverse.transformDexFiles(apk,
                    { dexClass -> if (dexClass.isPublic) dexClass.packageName else null },
                    { classList -> dexPackageList.add(classList) })
                block(dexPackageList)
            }
        } catch (e: Throwable) { // may produce OutOfMemoryError
            val eMsg = e::class.simpleName + ": " + e.message
            val cause = e.cause?.message?.let { " BY $it" } ?: ""
            val fileName = file.path.decentApkFileName
            Log.w("av.util.ApkUtils", "$eMsg$cause ($ownPkg, $fileName)")
            null
        }
    }

    class ThirdPartyPkgFilter(private val ownPkg: String, private val removeInternals: Boolean) {
        private val regexOwnPkg = """$ownPkg\..+""".toRegex()
        // more than one section: end with one character or plus a digit
        private val regexObfuscatedEnding = """.*\.\w\d?""".toRegex()
        // only one section: no more than two characters, or one character plus a digit
//        private val regexObfuscated1 = """\w[\w\d]?""".toRegex()
        // only one section: without any dot
        private val regexObfuscatedSingleSection = """[^.]+""".toRegex()
        private val pKReflect = "kotlin.reflect.jvm.internal"
        private val obfuscatedExceptions = hashSetOf(
            "androidx.legacy.v4",
            "java.com.android.tools.r8",
            "kotlin",
            "okhttp3",
            "okio",
            "retrofit2",
        )

        fun map(target: String): String? {
            val mapOwn = mapOwnPkg(target)
            if (mapOwn != target) return mapOwn
            val mapObfuscated = mapObfuscated(target)
            if (mapObfuscated != target) return mapObfuscated
            return target
        }

        fun mapOwnPkg(target: String): String? {
            if (target == ownPkg) return null // packages of its own
            if (target.matches(regexOwnPkg)) return null // packages of its own
            return target
        }

        fun mapObfuscated(target: String): String? {
            if (target in obfuscatedExceptions) return target
            if (target.matches(regexObfuscatedSingleSection)) return null // the obfuscated
//            if (target.matches(regexObfuscated1)) return null // the obfuscated
            if (target.matches(regexObfuscatedEnding)) return null // the obfuscated
            if (removeInternals && target.startsWith(pKReflect)) return pKReflect
            return target
        }
    }

    class R8RewriteReverser {
        private val regexR8Rewrite = """(j\$)((?:\..+)*)""".toRegex()

        fun map(target: String): String {
            // return self if starts with j
            if (target.firstOrNull() != 'j') return target
            // convert R8 prefix rewrite of Java 8 APIs
            return target.replace(regexR8Rewrite) { s -> "java" + s.groupValues[2] }
        }
    }

    fun checkPkg(path: String, packageName: String): Boolean {
        return checkPkg(File(path), packageName)
    }

    fun checkPkg(file: File, packageName: String): Boolean {
        return try {
            ApkFile(file).use { apk ->
                var isFound = false
                ApkTraverse.traverseDexFiles(apk) { _, data ->
                    isFound = data.packageName.startsWith(packageName)
                    isFound.not()
                }
                isFound
            }
        } catch (e: Throwable) { // may produce OutOfMemoryError
            val eMsg = e::class.simpleName + ": " + e.message
            val cause = e.cause?.message?.let { " BY $it" } ?: ""
            val fileName = file.path.decentApkFileName
            Log.w("av.util.ApkUtils", "$eMsg$cause (check $packageName in $fileName)")
            false
        }
    }

    private val String.decentApkFileName: String
        get() {
            // index of last separator
            val iM1 = lastIndexOf(File.separatorChar)
            // index of second last separator
            val iM2 = lastIndexOf(File.separatorChar, startIndex = kotlin.math.max(iM1, 1) - 1)
            return substring(iM2 + 1)
        }
}
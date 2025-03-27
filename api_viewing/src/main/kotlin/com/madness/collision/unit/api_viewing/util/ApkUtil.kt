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
import com.madness.collision.unit.api_viewing.info.DexResolver
import com.madness.collision.unit.api_viewing.info.R8RewriteReverser
import com.madness.collision.unit.api_viewing.info.ThirdPartyPkgFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.TreeSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApkUtil {

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
        val pkgFilter = ThirdPartyPkgFilter(ownPkg, removeInternals)
        val rewriteReverser = R8RewriteReverser()
        return DexResolver.loadDexLib(file.path) { pkgFilter.map(it)?.let(rewriteReverser::map) }
            .logApkError(file.path, ownPkg).getOrNull().orEmpty()
    }

    data class PkgPartitions(val filtered: List<String>, val self: List<String>, val minimized: List<String>)

    fun getThirdPartyPkgPartitions(path: String, ownPkg: String): PkgPartitions {
        return getThirdPartyPkgPartitions(File(path), ownPkg)
    }

    suspend fun getThirdPartyPkgPartitions(pathList: List<String>, ownPkg: String): PkgPartitions {
        val files = pathList.mapNotNull { p -> File(p).takeIf { it.exists() && it.canRead() } }
        // fast path: avoid TreeSet.addAll() invocation
        when {
            files.isEmpty() -> return PkgPartitions(emptyList(), emptyList(), emptyList())
            files.size == 1 -> return getThirdPartyPkgPartitions(files[0], ownPkg)
        }
        return coroutineScope {
            // use tree set to eliminate duplicates and sort package names from several apks
            val pkgSet = List(3) { TreeSet<String>() }
            // async() makes 2x+ speed boost for Taobao
            files.map { file -> async(Dispatchers.IO) { getThirdPartyPkgPartitions(file, ownPkg) } }
                .asFlow().map { it.await() }
                .collect { (a, b, c) -> pkgSet[0].addAll(a); pkgSet[1].addAll(b); pkgSet[2].addAll(c) }
            PkgPartitions(pkgSet[0].toList(), pkgSet[1].toList(), pkgSet[2].toList())
        }
    }

    fun getThirdPartyPkgPartitions(file: File, ownPkg: String, removeInternals: Boolean = true): PkgPartitions {
        val pkgFilter = ThirdPartyPkgFilter(ownPkg, removeInternals)
        val rewriteReverser = R8RewriteReverser()
        val pkgList = DexResolver.loadDexLib(file.path, rewriteReverser::map)
            .logApkError(file.path, ownPkg).getOrNull()
            ?: return PkgPartitions(emptyList(), emptyList(), emptyList())
        val transforms = mutableSetOf<String>()
        val filtered = ArrayList<String>()
        val self = ArrayList<String>()
        val out = ArrayList<String>()
        pkgList.forEach { pkg ->
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
        return PkgPartitions(first.distinct(), self.distinct(), out.distinct())
    }

    private fun <T> Result<T>.logApkError(path: String, ownPkg: String) = onFailure { e ->
        val eMsg = e::class.simpleName + ": " + e.message
        val cause = e.cause?.message?.let { " BY $it" } ?: ""
        val fileName = path.decentApkFileName
        Log.w("av.util.ApkUtils", "$eMsg$cause ($ownPkg, $fileName)")
    }

    fun checkPkg(path: String, packageName: String): Boolean {
        return checkPkg(File(path), packageName)
    }

    fun checkPkg(file: File, packageName: String): Boolean {
        return DexResolver.findPackage(file.path, packageName)
            .onFailure { e ->
                val eMsg = e::class.simpleName + ": " + e.message
                val cause = e.cause?.message?.let { " BY $it" } ?: ""
                val fileName = file.path.decentApkFileName
                Log.w("av.util.ApkUtils", "$eMsg$cause (check $packageName in $fileName)")
            }
            .getOrDefault(false)
    }

    fun checkPkg(path: String, vararg packageName: String): BooleanArray {
        return DexResolver.findPackages(path, *packageName)
            .onFailure { e ->
                val eMsg = e::class.simpleName + ": " + e.message
                val cause = e.cause?.message?.let { " BY $it" } ?: ""
                val fileName = path.decentApkFileName
                Log.w("av.util.ApkUtils", "$eMsg$cause (check ${packageName.joinToString()} in $fileName)")
            }
            .getOrDefault(BooleanArray(packageName.size))
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
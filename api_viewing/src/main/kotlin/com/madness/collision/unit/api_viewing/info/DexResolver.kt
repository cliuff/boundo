/*
 * Copyright 2023 Clifford Liu
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

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.ZipDexContainer
import java.io.File
import java.util.LinkedList
import java.util.TreeSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Check file extension first to avoid reading every entry's stream,
 * which takes an insanely significant amount of time.
 * The only exception found is assets/mini.db from the base.apk of
 * MX Player Pro (com.mxtech.videoplayer.pro) 1.68.4/2001002074,
 * which contains a single class [Help_a] with an empty static method [h].
 *
 * microbenchmark results, ExtensionDexContainer vs. ZipDexContainer:
 *   reduction in execution time: over 50% on Pixel 4, over 70% on Pixel 3;
 *   reduction in allocations   : over 70% on Pixel 4, over 85% on Pixel 3;
 */
open class ExtensionDexContainer(file: File, opcodes: Opcodes) : ZipDexContainer(file, opcodes) {
    override fun isDex(zipFile: ZipFile, zipEntry: ZipEntry): Boolean {
        return zipEntry.name.endsWith(".dex") && super.isDex(zipFile, zipEntry)
    }
}

/** see [DexFileFactory.loadDexContainer] */
object DexContainerFactory {
    /**
     * Opcodes are the instruction set of Dalvik/ART virtual machine,
     * Dalvik executable (DEX) is compiled by D8 dexer,
     * and its version is determined by min SDK.
     */
    fun load(apkPath: String, minSdk: Int = -1): EnumDexContainer {
        val file = File(apkPath)
        if (file.exists().not()) throw RuntimeException("file does not exist")
        val opcodes = if (minSdk >= 0) Opcodes.forApi(minSdk) else Opcodes.getDefault()
        return EnumDexContainer(file, opcodes).takeIf { it.isZipFile }
            ?: throw RuntimeException("not a zip file")
    }
}

object DexResolver {
    // microbenchmark results, smali-dexlib2 vs. apk-parser:
    //   reduction in execution time: over 20% on Pixel 4, Pixel 3
    //   reduction in allocations   : over 55% on Pixel 4, Pixel 3
    fun loadDexLib(apkPath: String) = kotlin.runCatching {
        val pkgSet = TreeSet<String>()
        DexContainerFactory.load(apkPath).dexEntrySeq.forEach { entry ->
            entry.dexFile.classes.mapNotNullTo(pkgSet) { mapToPackage(it.type) }
        }
        pkgSet.toList()
    }

    fun loadDexLib(apkPath: String, convert: (String) -> String?) = kotlin.runCatching {
        // many class types will share the same package name,
        // use a tree set to eliminate duplicates and sort them
        val pkgSet = TreeSet<String>()
        DexContainerFactory.load(apkPath).dexEntrySeq.forEach { entry ->
            entry.dexFile.classes.mapNotNullTo(pkgSet) { mapToPackage(it.type) }
        }
        // apply data conversion on the distinct set to avoid duplicated operations,
        // use the tree set again to get distinct and sorted data set after conversion
        val convPkgs = LinkedList<String>()
        val iterator = pkgSet.iterator()
        while (iterator.hasNext()) {
            val pkg = iterator.next()
            when (val conv = convert(pkg)) {
                pkg -> Unit
                null -> iterator.remove()
                else -> { iterator.remove(); convPkgs.add(conv) }
            }
        }
        pkgSet.addAll(convPkgs)
        pkgSet.toList()
    }

    fun findPackage(apkPath: String, targetPkg: String) = kotlin.runCatching {
        DexContainerFactory.load(apkPath).dexEntrySeq.forEach { entry ->
            for (classDef in entry.dexFile.classes) {
                val pkg = mapToPackage(classDef.type) ?: continue
                if (pkg.startsWith(targetPkg)) return@runCatching true
            }
        }
        false
    }

    fun findPackages(apkPath: String, vararg targetPkg: String) = kotlin.runCatching {
        val result = BooleanArray(targetPkg.size)
        DexContainerFactory.load(apkPath).dexEntrySeq.forEach { entry ->
            var complete: Boolean
            for (classDef in entry.dexFile.classes) {
                val pkg = mapToPackage(classDef.type) ?: continue
                complete = true
                for (i in targetPkg.indices) {
                    if (result[i]) continue
                    if (pkg.startsWith(targetPkg[i])) result[i] = true
                    else complete = false
                }
                if (complete) return@runCatching result
            }
        }
        result
    }

    // Landroid/app/AppComponentFactory; -> android.app
    private fun mapToPackage(type: String): String? {
        val pkg = kotlin.run {
            if (type.startsWith('L') && type.endsWith(';')) {
                val endIndex = type.lastIndexOf('/')
                if (endIndex > 1) {
                    type.substring(1, endIndex).replace('/', '.')
                } else {
                    type.substring(1, type.length - 1)
                }
            } else {
                type
            }
        }
        return pkg.takeUnless { it.isBlank() }
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

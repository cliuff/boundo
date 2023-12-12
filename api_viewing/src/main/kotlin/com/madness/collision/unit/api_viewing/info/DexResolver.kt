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

import android.os.Build
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.ZipDexContainer
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.dexlib2.iface.MultiDexContainer.DexEntry
import java.io.File
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
class ExtensionDexContainer(file: File, opcodes: Opcodes) : ZipDexContainer(file, opcodes) {
    override fun isDex(zipFile: ZipFile, zipEntry: ZipEntry): Boolean {
        return zipEntry.name.endsWith(".dex") && super.isDex(zipFile, zipEntry)
    }
}

/** see [DexFileFactory.loadDexContainer] */
object DexContainerFactory {
    fun load(apkPath: String): MultiDexContainer<DexBackedDexFile> {
        val file = File(apkPath)
        if (file.exists().not()) throw RuntimeException("file does not exist")
        val opcodes = Opcodes.forApi(Build.VERSION.SDK_INT)
        return ExtensionDexContainer(file, opcodes).takeIf { it.isZipFile }
            ?: throw RuntimeException("not a zip file")
    }
}

inline fun <T : DexFile> MultiDexContainer<T>.forEachDexEntry(action: (entry: DexEntry<T>) -> Unit) {
    dexEntryNames.forEach { getEntry(it)?.let(action) }
}

object DexResolver {
    // microbenchmark results, smali-dexlib2 vs. apk-parser:
    //   reduction in execution time: over 20% on Pixel 4, Pixel 3
    //   reduction in allocations   : over 55% on Pixel 4, Pixel 3
    fun loadDexLib(apkPath: String) = kotlin.runCatching {
        val pkgSet = TreeSet<String>()
        DexContainerFactory.load(apkPath).forEachDexEntry { entry ->
            entry.dexFile.classes.mapNotNullTo(pkgSet) { mapToPackage(it.type) }
        }
        pkgSet.toList()
    }

    fun findPackage(apkPath: String, targetPkg: String) = kotlin.runCatching {
        DexContainerFactory.load(apkPath).forEachDexEntry { entry ->
            for (classDef in entry.dexFile.classes) {
                val pkg = mapToPackage(classDef.type) ?: continue
                if (pkg.startsWith(targetPkg)) return@runCatching true
            }
        }
        false
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
/*
 * Copyright 2026 Clifford Liu
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

package io.cliuff.boundo.art.apk.dex

import com.android.tools.smali.dexlib2.Opcodes
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import java.io.File

private fun loadAsyncDexContainer(apkPath: String, minSdk: Int = -1): AsyncDexContainer {
    val file = File(apkPath)
    if (file.exists().not()) throw RuntimeException("file does not exist")
    val opcodes = if (minSdk >= 0) Opcodes.forApi(minSdk) else Opcodes.getDefault()
    return AsyncDexContainer(file, opcodes)
}

class AsyncDexLibSuperFinder {
    suspend fun resolve(apks: List<String>, names: Set<String>): Set<String> {
        if (apks.isEmpty()) return names
        if (names.isEmpty()) return emptySet()
        val typeNames = names.mapTo(HashSet(names.size)) { n ->
            "L" + n.replace('.', '/') + ";"
        }
        val superclasses = ArrayList<String>(names.size)
        apk@ for (apkPath in apks) {
            // limit async dex processing to reduce memory usage
            val dexEntryTransformer = LimitDexEntryTransformer(3)
            // enumerate dex entries in an apk
            loadAsyncDexContainer(apkPath)
                .getDexFileFlow(dexEntryTransformer)
                .takeWhile { superclasses.size != names.size }
                .onEach { dexFile ->
                    klass@ for (classDef in dexFile.classes) {
                        if (classDef.type in typeNames) {
                            val superOrThis = classDef.superclass?.let(DexTypeDesc::toName)
                                ?: (DexTypeDesc.toName(classDef.type) ?: continue@klass)
                            superclasses.add(superOrThis)
                        }
                        if (superclasses.size == names.size) break@klass
                    }
                }
                .catch { it.printStackTrace() }
                .collect()
            if (superclasses.size == names.size) break@apk
        }
        return names + superclasses
    }
}

internal object DexTypeDesc {
    // Landroid/app/AppComponentFactory; -> android.app.AppComponentFactory
    fun toName(type: String): String? {
        val name = kotlin.run {
            if (type.startsWith('L') && type.endsWith(';')) {
                type.substring(1, type.length - 1).replace('/', '.')
            } else {
                type
            }
        }
        return name.takeUnless { it.isBlank() }
    }
}

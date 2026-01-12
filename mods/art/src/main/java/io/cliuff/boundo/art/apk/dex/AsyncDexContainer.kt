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
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.util.DexUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import java.io.File
import java.io.InputStream

class AsyncDexContainer(private val file: File, private val opcodes: Opcodes) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDexFileFlow(transformer: DexEntryTransformer? = null): Flow<DexFile> =
        DexIterator.getBytesFlow(file.path, transformer)
            .flatMapMerge { bytes -> getDexFiles(bytes, opcodes).asFlow() }
}

internal object DexFileVerifier {
    fun testHeader(inputStream: InputStream): Boolean {
        return isValidDex(inputStream)
    }
}

class OffsetHeaderDexFile : DexBackedDexFile {
    // expose protected constructor
    constructor(opcodes: Opcodes?, buf: ByteArray, offset: Int, verifyMagic: Boolean, headerOffset: Int) :
            super(opcodes, buf, offset, verifyMagic, headerOffset)
}

// There might be several dex files in zip entry since DEX v41.
private fun getDexFiles(bytes: ByteArray, opcodes: Opcodes): List<DexBackedDexFile> {
    var dexList: ArrayList<OffsetHeaderDexFile>? = null
    var offset = 0
    while (offset < bytes.size) {
        val dex = OffsetHeaderDexFile(opcodes, bytes, 0, true, offset)
        if (dexList == null) {
            // early return single dex
            if (dex.fileSize >= bytes.size) return listOf(dex)
            dexList = ArrayList()
        }
        offset += dex.fileSize
        dexList += dex
    }
    return dexList.orEmpty()
}

private fun isValidDex(inputStream: InputStream): Boolean {
    return try {
        DexUtil.verifyDexHeader(inputStream)
        true
    } catch (e: DexBackedDexFile.NotADexFile) {
        e.printStackTrace()
        false
    } catch (e: DexUtil.InvalidFile) {
        e.printStackTrace()
        false
    } catch (e: DexUtil.UnsupportedFile) {
        e.printStackTrace()
        false
    }
}

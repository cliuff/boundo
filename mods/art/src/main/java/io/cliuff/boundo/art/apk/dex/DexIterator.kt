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

import com.android.tools.smali.util.InputStreamUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object DexIterator {
    fun getBytesSequence(path: String): Sequence<ByteArray> =
        getDexBytesSequence(path)

    fun getBytesFlow(path: String, transformer: DexEntryTransformer? = null): Flow<ByteArray> =
        getDexBytesFlow(path, transformer)
}

private fun ZipFile.dexEntries(): Sequence<ZipEntry> =
    entries().asSequence()
        .filter { it.name.endsWith(".dex") }

private fun getDexBytesSequence(path: String): Sequence<ByteArray> {
    val file = File(path)
    if (path.isBlank() || !file.exists() || !file.canRead()) return emptySequence()

    return sequence {
        val zip = try {
            ZipFile(file)
        } catch (e: ZipException) {
            throw e
        } catch (e: IOException) {
            throw e
        }
        zip.use { zip ->
            for (entry in zip.dexEntries()) {
                val inStream = try {
                    zip.getInputStream(entry)
                } catch (e: IOException) {
                    e.printStackTrace()
                    continue
                }
                inStream.use { inStream ->
                    val bufferedStream = inStream.buffered()
                    if (isValidDex(bufferedStream)) {
//                            val bytes = bufferedStream.readBytes()
                        val bytes = InputStreamUtil.toByteArray(bufferedStream)
                        yield(bytes)
                    }
                }
            }
        }
    }
}

interface DexEntryTransformer {
    /** Suspend or skip the processing of entry. */
    suspend fun apply(entry: ZipEntry): ZipEntry?
    suspend fun postProcessing(entry: ZipEntry)
}

private fun getDexBytesFlow(path: String, transformer: DexEntryTransformer? = null): Flow<ByteArray> {
    val file = File(path)
    if (path.isBlank() || !file.exists() || !file.canRead()) return emptyFlow()

    return channelFlow {
        val zip = try {
            ZipFile(file)
        } catch (e: ZipException) {
            throw e
        } catch (e: IOException) {
            throw e
        }
        zip.use { zip ->
            val jobs = ArrayList<Job>()
            for (entry in zip.dexEntries()) {
                ensureActive() // cooperative
                // suspend or skip entry
                val processEntry = if (transformer != null) {
                    val e = transformer.apply(entry)
                    if (e == null) continue
                    e
                } else {
                    entry
                }
                try {
                    ensureActive() // cooperative
                    // get entry input stream synchronously
                    val inStream = try {
                        zip.getInputStream(processEntry)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        continue
                    }
                    // process entry input stream asynchronously
                    jobs += launch {
                        inStream.use { _ ->
                            ensureActive() // cooperative
                            val bufferedStream = inStream.buffered()
                            if (isValidDex(bufferedStream)) {
                                yield() // cooperative
                                val bytes = InputStreamUtil.toByteArray(bufferedStream)
                                send(bytes)
                            }
                        }
                    }
                } finally {
                    // notify transformer of post-processing
                    transformer?.postProcessing(processEntry)
                    ensureActive() // cooperative
                }
            }
            // wait for jobs to close file
            jobs.joinAll()
        }
    }
}

class LimitDexEntryTransformer(asyncDexLimit: Int) : DexEntryTransformer {
    private val semaphore = Semaphore(asyncDexLimit)
    private val asyncDexNames = HashSet<String>()
    private val dexMutex = Mutex()

    override suspend fun apply(entry: ZipEntry): ZipEntry? {
        if (dexMutex.withLock { asyncDexNames.add(entry.name) }) {
            // suspend until a permit is released in postProcessing
            semaphore.acquire()
        }
        return entry
    }

    override suspend fun postProcessing(entry: ZipEntry) {
        if (dexMutex.withLock { asyncDexNames.remove(entry.name) }) {
            semaphore.release()
        }
    }
}

private fun isValidDex(inputStream: InputStream): Boolean =
    DexFileVerifier.testHeader(inputStream)

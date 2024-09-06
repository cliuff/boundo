package com.madness.collision.unit.api_viewing.info

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.ZipDexContainer
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer.DexEntry
import java.io.Closeable
import java.io.File
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Enumerate DEX entries one by one.
 * For smali-dexlib2 3.0.4+, DEX entries are processed all at once.
 * Loading all DEX entries' bytes causes significant memory consumption, even OOM (e.g. QQ app).
 */
class EnumDexContainer(private val file: File, opcodes: Opcodes) : EnumNodeDexContainer(file, opcodes) {
    private var nodeDexCounter: DexCounter = dexCounter

    /** Lazily enumerated DEX entries. */
    val dexEntrySeq: Sequence<DexEntry<out DexFile>> = sequence {
        val zipFile = zipFile
        zipFile.asCloseable().use { _ ->
            // enumerate dex entries on enum nodes
            val entryEnum = zipFile.entries()
            while (entryEnum.hasMoreElements()) {
                try {
                    // entries are cached once generated, use a new object to get more
                    val enumNode = EnumNodeDexContainer(file, opcodes, zipFile)
                    nodeDexCounter = enumNode.dexCounter
                    val ret = enumNode.dexEntryNames
                    dexCounter += enumNode.dexCounter
                    // getEntry on node instead of EnumDexContainer, who does not have entry cache
                    yieldAll(ret.mapNotNull(enumNode::getEntry))
                } catch (e: OutOfMemoryError) {
                    val msg = "Enumerated ${dexCounter.dexSize} bytes of ${dexCounter.dexCount} DEX"
                    OutOfMemoryError(msg).printStackTrace()
                    throw e
                }
            }
        }
    }

    override fun getEnumZipFile(): EnumZipFile {
        return EnumZipFile(file) hasNext@{
            if (!nodeDexCounter.isLoadingEntries) return@hasNext true
            // one dex entry at a time
            nodeDexCounter.dexCount <= 0
        }
    }
}

open class EnumNodeDexContainer(
    private val file: File,
    opcodes: Opcodes,
    private var enumZipFile: EnumZipFile? = null,
    val dexCounter: DexCounter = DexCounter(),
) : ExtensionDexContainer(file, opcodes) {

    override fun getDexEntryNames(): MutableList<String> {
        return dexCounter.use { super.getDexEntryNames() }
    }

    override fun isDex(zipFile: ZipFile, zipEntry: ZipEntry): Boolean {
        return super.isDex(zipFile, zipEntry).also { isDex ->
            if (isDex) dexCounter.increment(zipEntry.size)
        }
    }

    override fun getZipFile(): EnumZipFile {
        return enumZipFile ?: getEnumZipFile().also { enumZipFile = it }
    }

    /** Create new [EnumZipFile] instance for use in [getZipFile]. */
    protected open fun getEnumZipFile(): EnumZipFile {
        return EnumZipFile(file) { true }
    }
}

class EnumZipFile(file: File, private val hasNextEntry: () -> Boolean) : ZipFile(file) {
    private var entryEnum: Enumeration<ZipEntry>? = null

    /** Cached value is returned to be used across [EnumDexContainer]s. */
    override fun entries(): Enumeration<out ZipEntry> {
        entryEnum?.let { return it }
        val enum = super.entries()
        return object : Enumeration<ZipEntry> {
            override fun hasMoreElements() = enum.hasMoreElements() && hasNextEntry()
            override fun nextElement() = enum.nextElement()
        }.also { entryEnum = it }
    }

    /** Cleared to prevent early close in [ZipDexContainer.getDexEntryNames] by super class. */
    override fun close() {
    }

    fun asCloseable(): Closeable = Closeable { super.close() }
}

class DexCounter {
    private var entLoad: Int = 0
    val isLoadingEntries: Boolean get() = entLoad > 0
    var dexCount: Int = 0; private set
    var dexSize: Long = 0L; private set

    fun <T> use(block: () -> T): T {
        dexCount = 0
        dexSize = 0
        entLoad++
        val ret = block()
        entLoad = 0
        return ret
    }

    fun increment(dexSize: Long) {
        if (isLoadingEntries) {
            dexCount++
            this.dexSize += dexSize
        }
    }

    operator fun plusAssign(counter: DexCounter) {
        dexCount += counter.dexCount
        dexSize += counter.dexSize
    }
}

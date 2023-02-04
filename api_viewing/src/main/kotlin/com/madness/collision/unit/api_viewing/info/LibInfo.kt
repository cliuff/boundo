/*
 * Copyright 2022 Clifford Liu
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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import com.absinthe.rulesbundle.*
import com.madness.collision.misc.PackageCompat
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.util.StringUtils
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.appContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

// PackageComponent
typealias PackComponent = ValueOwnerComp
sealed interface ValueCompOwner { val comp: ValueComponent }
sealed interface ValueOwnerComp : ValueCompOwner
sealed interface ValueComponent : ValueOwnerComp {
    val value: String  // Activity/Service name, .so file name
    override val comp: ValueComponent get() = this
    class Simple(override val value: String) : ValueComponent
    // Activity/Service/Receiver/Provider
    class AppComp(override val value: String, val isEnabled: Boolean) : ValueComponent
    class NativeLib(override val value: String, val entries: List<NativeLibEntry>) : ValueComponent
}

sealed class MarkedComponent(mark: LibMark) : ValueOwnerComp, LibMark by mark
// merged components of the same markedLabel
class MarkedMergingComp<C : ValueComponent>(mark: LibMark, val components: List<C>) : MarkedComponent(mark) {
    override val comp: ValueComponent = components[0]
    fun <V : ValueComponent> castComponents(): List<V> = components as List<V>
}
class MarkedValueComp<C : ValueComponent>(mark: LibMark, override val comp: C) : MarkedComponent(mark) {
    fun <V : ValueComponent> castComp(): V = comp as V
}

interface LibMark {
    val markedLabel: String
    val markedIconResId: Int?
    val isMarkedIconMono: Boolean
}

class LibMarkImpl(
    override val markedLabel: String,
    override val markedIconResId: Int?,
    override val isMarkedIconMono: Boolean
) : LibMark

typealias NativeLibEntry = Pair<String, Triple<String, Long, Long>>

// ComponentSection
enum class CompSection {
    Marked, Normal, MinimizedSelf, Minimized
}

// PackageComponentType
enum class PackCompType {
    Activity, Service, Receiver, Provider, DexPackage, NativeLibrary
}

private val PackCompType.libType
    get() = when (this) {
        PackCompType.Activity -> ACTIVITY
        PackCompType.Service -> SERVICE
        PackCompType.Receiver -> RECEIVER
        PackCompType.Provider -> PROVIDER
        PackCompType.DexPackage -> DEX
        PackCompType.NativeLibrary -> NATIVE
    }

class CompSectionCollection : SectionMapCollection<CompSection, PackComponent>()

class PackCompCollection : SectionMapCollection<PackCompType, CompSectionCollection>() {
    enum class State { None, Loaded }
    private val typeState: MutableMap<PackCompType, State> = mutableMapOf()

    fun getTypeState(type: PackCompType): State {
        return typeState[type] ?: State.None
    }

    fun setTypeState(type: PackCompType, state: State) {
        typeState[type] = state
    }
}

suspend fun PackCompCollection.loadType(context: Context, app: ApiViewingApp, type: PackCompType) =
    LibInfoRetriever.loadType(context, app, this, type)

object LibInfoRetriever {
    private val compTypeLoadMutex: MutableMap<Pair<PackCompCollection, LoadType>, Mutex> = hashMapOf()

    suspend fun loadType(context: Context, app: ApiViewingApp, collection: PackCompCollection, type: PackCompType) {
        val mutexKey = collection to type.loadType
        if (collection.getTypeState(type) == PackCompCollection.State.Loaded) return
        val mutex = compTypeLoadMutex[mutexKey] ?: Mutex()
        mutex.withLock {
            compTypeLoadMutex[mutexKey] = mutex
            if (collection.getTypeState(type) == PackCompCollection.State.None) {
                loadTypeActual(context, app, collection, type)
            }
            compTypeLoadMutex.remove(mutexKey, mutex)
        }
    }

    private val PackCompType.loadType: LoadType
        get() = when (this) {
            PackCompType.Activity, PackCompType.Service,
            PackCompType.Receiver, PackCompType.Provider -> LoadType.AndroidComponents
            PackCompType.DexPackage -> LoadType.DexPackages
            PackCompType.NativeLibrary -> LoadType.NativeLibraries
        }

    private enum class LoadType {
        AndroidComponents, DexPackages, NativeLibraries
    }

    private fun loadTypeActual(context: Context, app: ApiViewingApp, collection: PackCompCollection, type: PackCompType) {
        Log.d("LibInfoRetriever", "loadTypeActual/${app.packageName}/$type")
        when (type.loadType) {
            LoadType.AndroidComponents -> {
                getComponents(context, app).forEach { (compType, items) ->
                    collection.finishLoad(compType, items)
                }
            }
            LoadType.DexPackages -> {
                val (normal, self, minimized) = resolveDexPackages(app)
                collection.finishLoad(type, normal, self, minimized)
            }
            LoadType.NativeLibraries ->
                collection.finishLoad(type, getNativeLibs(app))
        }
    }

    private fun PackCompCollection.finishLoad(
        type: PackCompType, compList: Collection<ValueOwnerComp>,
        minimizedSelfList: Collection<ValueOwnerComp>? = null,
        minimizedList: Collection<ValueOwnerComp>? = null) {
        val markedList = compList.filterIsInstance<MarkedValueComp<*>>()
        // merge items of the same label
        val mergedMarkedList = markedList.groupBy { it.markedLabel }.map { (_, list) ->
            if (list.size == 1) return@map list[0]
            val sortedList = list.sortedBy { it.comp.value }
            val values = sortedList.map { it.comp }
            MarkedMergingComp(sortedList[0], values)
        }
        val sectioned = CompSectionCollection().apply {
            // sort Marked section by markedLabel with utils
            val comparator = compareBy<MarkedComponent, String>(StringUtils.comparator) { it.markedLabel }
            set(CompSection.Marked, mergedMarkedList.sortedWith(comparator))
            // sort Normal section by component name
            set(CompSection.Normal, compList.sortedBy { it.comp.value })
            if (minimizedSelfList != null) set(CompSection.MinimizedSelf, minimizedSelfList.sortedBy { it.comp.value })
            if (minimizedList != null) set(CompSection.Minimized, minimizedList.sortedBy { it.comp.value })
        }
        this[type] = listOf(sectioned)
        setTypeState(type, PackCompCollection.State.Loaded)
    }

    @Suppress("deprecation")
    private val flagGetDisabledLegacy = PackageManager.GET_DISABLED_COMPONENTS

    private fun getPack(context: Context, app: ApiViewingApp): PackageInfo? {
        val flagGetDisabled = when {
            OsUtils.satisfy(OsUtils.N) -> PackageManager.MATCH_DISABLED_COMPONENTS
            else -> flagGetDisabledLegacy
        }
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS or flagGetDisabled
        return try {
            val packMan = context.packageManager
            when {
                app.isArchive -> PackageCompat.getArchivePackage(packMan, app.appPackage.basePath, flags)
                else -> PackageCompat.getInstalledPackage(packMan, app.packageName, flags)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getMarkedItem(compName: String, compType: Int): ValueOwnerComp {
        val rule = kotlin.run rule@{
            RuleRepository.rules?.get(compName)?.let { return@rule it }
            val rules = RuleRepository.regexRules ?: return@rule null
            for ((pattern, r) in rules) {
                if (r.type != compType) continue
                if (pattern.matcher(compName).matches()) return@rule r
            }
            null
        }
        rule ?: return ValueComponent.Simple(compName)
        val label = mapLocalizedLabelId(rule)?.let { appContext.getString(it) } ?: rule.label
        val iconId = IconResMap.getIconRes(rule.iconIndex)
        val isIconMono = IconResMap.isSingleColorIcon(rule.iconIndex)
        val mark = LibMarkImpl(label, iconId, isIconMono)
        return MarkedValueComp(mark, ValueComponent.Simple(compName))
    }

    private fun mapLocalizedLabelId(rule: RuleEntity) = when (rule.id) {
        102, 104 -> R.string.av_lib_rule_cpp_shared
        247, 297, 302, 303, 305, 307, 320, 329, 362, 366, 556, 723 -> R.string.av_lib_rule_alipay
        else -> null
    }

    private fun getComponents(context: Context, app: ApiViewingApp): Map<PackCompType, Collection<ValueOwnerComp>> {
        val typeList = listOf(
            PackCompType.Activity, PackCompType.Service,
            PackCompType.Receiver, PackCompType.Provider
        )
        val pack = getPack(context, app) ?: return typeList.associateWith { emptyList() }
        val compList = pack.run { listOf(activities, services, receivers, providers).map { it.orEmpty() } }
        return typeList.zip(compList).associate { (compType, compArray) ->
            val libType = compType.libType
            compType to compArray.map { compInfo ->
                val comp = ValueComponent.AppComp(compInfo.name, compInfo.enabled)
                val mark = getMarkedItem(compInfo.name, libType)
                if (mark !is LibMark) return@map comp
                MarkedValueComp(mark, comp)
            }
        }
    }

    private fun resolveDexPackages(app: ApiViewingApp): Triple<Collection<ValueOwnerComp>, Collection<ValueOwnerComp>, Collection<ValueOwnerComp>> {
        val dexPackages = app.appPackage.apkPaths.map {
            ApkUtil.getThirdPartyPkgPartitions(it, app.packageName)
        }
        val a = dexPackages.flatMap { (a, _, _) -> a.map { getMarkedItem(it, DEX) } }
        val b = dexPackages.flatMap { (_, b, _) -> b.map { getMarkedItem(it, DEX) } }
        val c = dexPackages.flatMap { (_, _, c) -> c.map { getMarkedItem(it, DEX) } }
        return Triple(a, b, c)
    }

    private fun getNativeLibs(app: ApiViewingApp): Collection<ValueOwnerComp> {
        val libEntries = app.appPackage.apkPaths.flatMap { ApkUtil.getNativeLibs(File(it)) }
        return libEntries
            .map { entry -> File(entry.first).name to entry }
            .groupBy { it.first }
            .map comp@{ (libName, libEntries) ->
                val sortedEntries = libEntries.sortedBy { (_, en) -> en.first }
                val lib = ValueComponent.NativeLib(libName, sortedEntries)
                val marked = getMarkedItem(libName, NATIVE)
                if (marked !is LibMark) return@comp lib
                MarkedValueComp(marked, lib)
            }
    }
}

object TwoDimenPositions {
    fun getCurrentIndex(index: Int, indexMap: Map<Int, Int>, lastIndex: Int): Int {
        var i = lastIndex
        var indexInc = 0
        while (true) {
            val typeIndex = indexMap[i] ?: break
            if (index == typeIndex) break
            if (indexInc == 0) indexInc = if (index > typeIndex) 1 else -1
            when {
                index > typeIndex && indexInc > 0 -> i += 1
                index < typeIndex && indexInc < 0 -> i -= 1
                else -> {
                    if (indexInc > 0) i -= 1
                    break
                }
            }
            if (i < 0 || i > indexMap.size - 1) break
        }
        return i.coerceIn(0, indexMap.size - 1)
    }
}

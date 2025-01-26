/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.unit.api_viewing.apps

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.madness.collision.util.StringUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PackageLabelProvider {
    /** Compare package names by its label. */
    val pkgComparator: Comparator<String>
    fun getLabel(pkgName: String): String?
    fun getLabelOrPkg(pkgName: String): String
}


open class PkgLabelProviderImpl(open val pkgLabels: Map<String, String>) : PackageLabelProvider {
    override val pkgComparator: Comparator<String>
        get() = compareBy(StringUtils.comparator, ::getLabelOrPkg).thenBy { pkg -> pkg }

    constructor() : this(emptyMap())

    override fun getLabel(pkgName: String): String? {
        return pkgLabels[pkgName]
    }

    override fun getLabelOrPkg(pkgName: String): String {
        val label = pkgLabels[pkgName]
        return when {
            label == null -> pkgName
            label.isEmpty() -> pkgName
            label.isBlank() -> pkgName
            else -> label
        }
    }
}

class AppPkgLabelProvider : PkgLabelProviderImpl() {
    override val pkgLabels: Map<String, String> by ::mutLabels
    private var mutLabels: Map<String, String> = emptyMap()
    private val pkgLabelMutex = Mutex()

    suspend fun retrieveLabels(pkgInfoProvider: PackageInfoProvider, pkgMgr: PackageManager) {
        retrieveLabels(pkgInfoProvider.getAll(), pkgMgr)
    }

    suspend fun retrieveLabels(pkgs: List<PackageInfo>, pkgMgr: PackageManager) {
        val labelMap = pkgLabels
        val loadPkgs = when {
            labelMap.isEmpty() -> pkgs
            else -> pkgs.filterNot { labelMap.containsKey(it.packageName) }
        }
        if (loadPkgs.isEmpty()) return
        val newLabels = loadLabels(loadPkgs, pkgMgr)
        pkgLabelMutex.withLock { mutLabels += newLabels }
    }

    private suspend fun loadLabels(pkgs: List<PackageInfo>, pkgMgr: PackageManager): Map<String, String> {
        if (pkgs.isEmpty()) return emptyMap()
        val labels = getLabelsAsync(pkgs, pkgMgr)
        return buildMap(labels.size) {
            for (i in pkgs.indices) {
                put(pkgs[i].packageName, labels[i] ?: continue)
            }
        }
    }
}

// todo cache
// async on every item may have negative impact on performance depending on the device
private suspend fun getLabelsAsync(pkgs: List<PackageInfo>, pkgMgr: PackageManager): List<String?> {
    if (pkgs.isEmpty()) return emptyList()
    return coroutineScope {
        val labels = Array(pkgs.size) { i ->
            async {
                pkgs[i].applicationInfo?.let { itemInfo ->
                    kotlin.runCatching { itemInfo.loadLabel(pkgMgr).toString() }.getOrNull()
                }
            }
        }
        awaitAll(*labels)
    }
}

private fun getLabels(pkgs: List<PackageInfo>, pkgMgr: PackageManager): List<String?> {
    if (pkgs.isEmpty()) return emptyList()
    return pkgs.map { p ->
        p.applicationInfo?.let { itemInfo ->
            runCatching { itemInfo.loadLabel(pkgMgr).toString() }.getOrNull()
        }
    }
}

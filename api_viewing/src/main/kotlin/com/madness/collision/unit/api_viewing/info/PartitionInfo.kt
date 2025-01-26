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

package com.madness.collision.unit.api_viewing.info

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.madness.collision.unit.api_viewing.env.SystemPartition

object PartitionInfo {
    fun getPkgPartitions(pkgs: List<PackageInfo>): Map<String, String> {
        if (pkgs.isEmpty()) return emptyMap()

        val appDirs = arrayOf("app", "priv-app", "overlay", "framework")
        val sysPrefix = SystemPartition.enum.associate { p -> p.path to appDirs.map { "${p.path}/$it/" } }
        val additional = mapOf(
            "/apex" to null, "/data" to "/data/app/",
            "/product/data-app" to null, "/cust" to "/cust/app/",  // Xiaomi HyperOS
            "/product_h/region_comm" to null, "/hw_product/region_comm" to null,  // Honor EMUI
            "/system/delapp" to null, "/data/preload" to null,  // EMUI, OriginOS
        )
        val sanitize = { part: String ->
            when (part) {
                "/data/preload" -> "/data"
                "/product/data-app" -> "/product"
                "/product_h/region_comm" -> "/product_h"
                "/hw_product/region_comm" -> "/hw_product"
                else -> part
            }
        }

        val pkgPrefix = sysPrefix + additional.mapValues { (k, v) -> listOf(v ?: "$k/") }
        val keyLength = pkgPrefix.keys.minOf(String::length); assert(keyLength > 1)
        val partKeyGroups = pkgPrefix.keys.groupBy { it.substring(0, keyLength) }
        val partKeys = partKeyGroups.keys

        return buildMap(pkgs.size) {
            for (pkg in pkgs) {
                val appInfo = pkg.applicationInfo ?: continue
                // for system apps not on /system partition
                if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    val isUpd = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                    val part = getPartFlag(appInfo, isUpd)
                    if (part != null) {
                        put(pkg.packageName, part)
                        continue
                    }
                }
                // fallback method for apps on /system, /data and /apex partitions
                val pkgSrcDir: String = appInfo.publicSourceDir
                val pkgKey = pkgSrcDir.substring(0, keyLength.coerceAtMost(pkgSrcDir.length))
                if (pkgKey !in partKeys) continue
                // matching multiple partitions for one key
                for (matchPart in partKeyGroups.getValue(pkgKey)) {
                    // multiple paths for a single partition
                    if (pkgPrefix.getValue(matchPart).any(pkgSrcDir::startsWith)) {
                        put(pkg.packageName, sanitize(matchPart))
                        break
                    }
                }
            }
        }
    }

    private fun getPartFlag(appInfo: ApplicationInfo, updated: Boolean): String? {
        val privFlags = PkgInfo.getPrivateFlags(appInfo)
        return when {
            // updated pkg is undetectable by fallback method
            privFlags == null -> if (updated) "/*" else null
            privFlags and (1 shl 21) != 0 -> SystemPartition.SystemExt.path
            privFlags and (1 shl 19) != 0 -> SystemPartition.Product.path
            privFlags and (1 shl 17) != 0 -> SystemPartition.OEM.path
            privFlags and (1 shl 30) != 0 -> SystemPartition.ODM.path
            privFlags and (1 shl 18) != 0 -> SystemPartition.Vendor.path
            else -> if (updated) "/system/*" else null
        }
    }
}

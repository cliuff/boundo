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

package com.madness.collision.misc

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.madness.collision.util.os.OsUtils

object PackageCompat {
    @Suppress("deprecation")
    private fun PackageManager.getInstalledPackagesLegacy(flags: Int) =
        getInstalledPackages(flags)

    @Suppress("deprecation")
    private fun PackageManager.getPackageInfoLegacy(pkgName: String, flags: Int) =
        getPackageInfo(pkgName, flags)

    @Suppress("deprecation")
    private fun PackageManager.getPackageArchiveInfoLegacy(path: String, flags: Int) =
        getPackageArchiveInfo(path, flags)

    @Suppress("deprecation")
    private fun PackageManager.getApplicationInfoLegacy(path: String, flags: Int) =
        getApplicationInfo(path, flags)

    fun getAllPackages(packMan: PackageManager, flags: Int = 0): List<PackageInfo> =
        when {
            OsUtils.satisfy(OsUtils.T) -> packMan.getInstalledPackages(PackageFlags.Package(flags))
            else -> packMan.getInstalledPackagesLegacy(flags)
        }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getInstalledPackage(packMan: PackageManager, packageName: String, flags: Int = 0): PackageInfo? =
        when {
            OsUtils.satisfy(OsUtils.T) -> packMan.getPackageInfo(packageName, PackageFlags.Package(flags))
            else -> packMan.getPackageInfoLegacy(packageName, flags)
        }

    fun getArchivePackage(packMan: PackageManager, path: String, flags: Int = 0): PackageInfo? =
        when {
            OsUtils.satisfy(OsUtils.T) -> packMan.getPackageArchiveInfo(path, PackageFlags.Package(flags))
            else -> packMan.getPackageArchiveInfoLegacy(path, flags)
        }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getApplication(packMan: PackageManager, packageName: String, flags: Int = 0): ApplicationInfo =
        when {
            OsUtils.satisfy(OsUtils.T) -> packMan.getApplicationInfo(packageName, PackageFlags.Application(flags))
            else -> packMan.getApplicationInfoLegacy(packageName, flags)
        }
}
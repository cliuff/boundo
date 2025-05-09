/*
 * Copyright 2024 Clifford Liu
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

import android.content.Context
import android.content.pm.PackageInfo
import android.os.SystemClock
import com.madness.collision.util.os.OsUtils
import java.lang.ref.WeakReference

interface PackageInfoProvider {
    fun getAll(): List<PackageInfo>
}

private typealias PkgListRef = WeakReference<List<PackageInfo>>

class PlatformAppProvider(private val context: Context) : PackageInfoProvider {
    private var pkgListCache: Pair<Long, PkgListRef> = -1L to WeakReference(null)

    @Synchronized
    override fun getAll(): List<PackageInfo> {
        val (time, pkgListRef) = pkgListCache
        pkgListRef.get()
            ?.takeIf { SystemClock.uptimeMillis() - time <= 5000 }
            ?.let { return it }

        val fetcher = PlatformAppsFetcher(context)
        var pkgs = fetcher.withSession(includeApex = false).getRawList()
        // match archived apps the second pass, to avoid potential info loss in returned results
        if (OsUtils.satisfy(OsUtils.V)) {
            val archived = fetcher.withSession(includeArchived = true).getRawList()
                .filter { pkg -> pkg.archiveTimeMillis > 0 || pkg.applicationInfo?.isArchived == true }
            if (archived.isNotEmpty()) pkgs = pkgs + archived
        }
        return pkgs.also { pkgListCache = SystemClock.uptimeMillis() to WeakReference(it) }
    }
}

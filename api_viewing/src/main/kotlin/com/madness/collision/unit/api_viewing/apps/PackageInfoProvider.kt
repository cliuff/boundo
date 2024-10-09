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
        return PlatformAppsFetcher(context).withSession(includeApex = false).getRawList()
            .also { pkgListCache = SystemClock.uptimeMillis() to WeakReference(it) }
    }
}

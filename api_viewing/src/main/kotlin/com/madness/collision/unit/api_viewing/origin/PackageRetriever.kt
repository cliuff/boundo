/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.origin

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import com.madness.collision.misc.PackageCompat
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.os.OsUtils

/**
 * Retrieve packages from user's device
 */
internal class PackageRetriever(private val context: Context) : OriginRetriever<PackageInfo> {

    override fun get(predicate: ((PackageInfo) -> Boolean)?): List<PackageInfo> {
        if (OsUtils.satisfy(OsUtils.R)) {
            val permission = Manifest.permission.QUERY_ALL_PACKAGES
            val isGranted = PermissionUtils.check(context, arrayOf(permission)).isEmpty()
            if (!isGranted) Log.d("av.origin.pack", "$permission not granted")
        }
        val list = PackageCompat.getAllPackages(context.packageManager)
        return if (predicate != null) list.filter(predicate) else list
    }
}

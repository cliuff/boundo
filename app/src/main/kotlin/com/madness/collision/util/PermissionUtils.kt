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

package com.madness.collision.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.madness.collision.BuildConfig
import com.madness.collision.util.os.OsUtils

object PermissionUtils {

    fun check(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasUsageAccess(context: Context) = isUsageAccessPermitted(context)

    fun isUsageAccessPermitted(context: Context): Boolean {
        val opsManager = context.getSystemService<AppOpsManager>() ?: return false
        val op = AppOpsManager.OPSTR_GET_USAGE_STATS
        val f = if (OsUtils.satisfy(OsUtils.Q)) AppOpsManager::unsafeCheckOpNoThrow else ::checkOpLegacy
        return f(opsManager, op, Process.myUid(), BuildConfig.APPLICATION_ID) == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("deprecation")
    private fun checkOpLegacy(appOpsManager: AppOpsManager, op: String, uid: Int, packageName: String): Int {
        return appOpsManager.checkOpNoThrow(op, uid, packageName)
    }
}

val Context.hasUsageAccess: Boolean get() = PermissionUtils.hasUsageAccess(this)

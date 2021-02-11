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

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.EasyAccess

/**
 * Retrieve packages from user's device
 */
internal interface OriginRetriever<T> {

    companion object {
        fun getPredicate(unit: Int): ((PackageInfo) -> Boolean)? = when (unit) {
            ApiUnit.USER -> {
                if (EasyAccess.shouldIncludeDisabled) { packageInfo ->
                    (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                } else { packageInfo ->
                    val app = packageInfo.applicationInfo
                    // users without root privilege can only disable system apps
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && app.enabled
                }
            }
            ApiUnit.SYS -> {
                if (EasyAccess.shouldIncludeDisabled) { packageInfo ->
                    (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } else { packageInfo ->
                    val app = packageInfo.applicationInfo
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && app.enabled
                }
            }
            ApiUnit.ALL_APPS -> {
                if (EasyAccess.shouldIncludeDisabled) null
                else { packageInfo ->
                    packageInfo.applicationInfo.enabled
                }
            }
            else -> null
        }
    }

    /**
     * Get by [predicate]
     */
    fun get(predicate: ((PackageInfo) -> Boolean)?): List<T>

    /**
     * Get all
     */
    fun get(): List<T> = get(null)

    /**
     * Get by [unit]
     */
    fun get(unit: Int): List<T> {
        return get(getPredicate(unit))
    }

    val user: List<T>
        get() = get(ApiUnit.USER)
    val system: List<T>
        get() = get(ApiUnit.SYS)
    val all: List<T>
        get() = get(ApiUnit.ALL_APPS)
}

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

package com.madness.collision.unit.api_viewing.data

class ApiUnit : ArrayList<Int>() {
    companion object {
        const val NON = 0
        const val USER = 1
        const val SYS = 2
        const val ALL_APPS = 3
        const val APK = 4
        const val SELECTED = 5
        const val VOLUME = 6

        /**
         * special use to indicate displaying app from drag & drop
         */
        const val DISPLAY = 7

        fun ineffective(item: Int): Boolean = item !in 1..7
    }

    var apkPreload = false
    private val loading = ArrayList<Int>()

    /**
     * Not loading any unit
     */
    val isVacant: Boolean
        get() = loading.isEmpty()
    val isBusy: Boolean
        get() = !isVacant

    fun isLoading(item: Int): Boolean {
        when (item) {
            USER, SYS, APK -> {
                return loading.contains(item)
            }
            ALL_APPS -> {
                val loadedUser = contains(USER)
                val loadedSys = contains(SYS)
                if (loadedUser && !loadedSys) {
                    if (loading.contains(SYS)) return true
                } else if (!loadedUser && loadedSys) {
                    if (loading.contains(USER)) return true
                }
            }
        }
        return false
    }

    fun loading(item: Int) {
        when (item) {
            USER, SYS, APK -> {
                if (loading.contains(item).not()) loading.add(item)
            }
            ALL_APPS -> {
                if (!loading.contains(USER)) loading.add(USER)
                if (!loading.contains(SYS)) loading.add(SYS)
            }
        }
    }

    fun finish(item: Int) {
        when (item) {
            USER, SYS, APK -> {
                if (contains(item).not()) add(item)
                if (loading.contains(item)) loading.remove(item)
            }
            ALL_APPS -> {
                if (!contains(USER)) finish(USER)
                if (!contains(SYS)) finish(SYS)
            }
        }
    }

    fun shouldLoad(item: Int): Boolean = when (item) {
        USER -> !contains(USER) && !isLoading(USER)
        SYS -> !contains(SYS) && !isLoading(SYS)
        ALL_APPS -> !(contains(USER) && contains(SYS)) && !isLoading(ALL_APPS)
        APK -> !contains(APK) && !isLoading(APK)
        NON -> false
        else -> true
    }

    fun item2Load(): Int {
        if (shouldLoad(USER)) return USER
        if (shouldLoad(SYS)) return SYS
        if (shouldLoad(APK) && apkPreload) return APK
        return NON
    }

    fun unLoad(unit: Int) {
        when (unit) {
            USER -> remove(USER)
            SYS -> remove(SYS)
            ALL_APPS -> {
                if (contains(USER)) remove(USER)
                if (contains(SYS)) remove(SYS)
            }
            APK -> remove(APK)
        }
    }

    fun allLoaded(): Boolean = contains(USER) && contains(SYS) && contains(APK)
}
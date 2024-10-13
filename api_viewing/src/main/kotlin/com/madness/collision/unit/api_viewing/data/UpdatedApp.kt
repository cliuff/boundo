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

package com.madness.collision.unit.api_viewing.data

sealed interface UpdatedApp {
    val app: ApiViewingApp

    class General(override val app: ApiViewingApp) : UpdatedApp

    class Upgrade(
        override val app: ApiViewingApp,
        val versionName: Pair<String, String>,
        val versionCode: Pair<Long, Long>,
        val updateTime: Pair<Long, Long>,
        val targetApi: Pair<Int, Int>,
    ) : UpdatedApp {
        companion object {
            fun get(previous: ApiViewingApp, new: ApiViewingApp) = getUpgrade(previous, new)
        }
    }
}

private fun getUpgrade(previous: ApiViewingApp, new: ApiViewingApp): UpdatedApp.Upgrade? {
    if (previous.targetAPI == new.targetAPI) return null
    return UpdatedApp.Upgrade(
        app = new,
        versionName = previous.verName to new.verName,
        versionCode = previous.verCode to new.verCode,
        updateTime = previous.updateTime to new.updateTime,
        targetApi = previous.targetAPI to new.targetAPI,
    )
}

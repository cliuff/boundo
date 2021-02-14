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

package com.madness.collision.unit.api_viewing.upgrade

import com.madness.collision.unit.api_viewing.data.ApiViewingApp

class Upgrade(val new: ApiViewingApp) {

    companion object {
        fun get(previous: ApiViewingApp, new: ApiViewingApp): Upgrade? {
            if (previous.targetAPI == new.targetAPI) return null
            return Upgrade(new).apply {
                versionName = previous.verName to new.verName
                versionCode = previous.verCode to new.verCode
                updateTime = previous.updateTime to new.updateTime
                targetApi = previous.targetAPI to new.targetAPI
            }
        }
    }

    lateinit var versionName: Pair<String, String>
    lateinit var versionCode: Pair<Long, Long>
    lateinit var updateTime: Pair<Long, Long>
    lateinit var targetApi: Pair<Int, Int>
}

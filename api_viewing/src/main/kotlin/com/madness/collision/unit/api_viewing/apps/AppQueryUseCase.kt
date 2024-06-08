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

import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.util.ui.appLocale

class AppQueryUseCase {
    fun filterInMemoryList(appList: List<ApiViewingApp>, query: String): List<ApiViewingApp> {
        if (appList.isEmpty()) return emptyList()
        if (query.isBlank()) return appList

        // Check store links
        Utils.checkStoreLink(query)?.let { pkg ->
            return listOfNotNull(appList.find { it.packageName == pkg })
        }

        val locale = appLocale
        val compInput = query.lowercase(locale)
        return appList.filter { info ->
            // match app name
            val appName = info.name.replace(" ", "").lowercase(locale)
            if (appName.contains(compInput)) return@filter true
            // match package name
            if (info.packageName.lowercase(locale).contains(compInput)) return@filter true
            // match Android API or version
            val ver = info.run {
                if (EasyAccess.isViewingTarget) VerInfo(targetAPI, targetSDK, targetSDKLetter)
                else VerInfo(minAPI, minSDK, minSDKLetter)
            }
            query == ver.apiText || ver.sdk.startsWith(query)
        }
    }
}
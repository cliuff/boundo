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

package com.madness.collision.unit.api_viewing.main

import android.content.Context
import com.madness.collision.unit.api_viewing.ApiViewingViewModel
import com.madness.collision.unit.api_viewing.data.ApiUnit

internal fun ApiViewingViewModel.loadAppItems(context: Context) {
    while (true) {
        val item = loadedItems.item2Load().takeUnless { it == ApiUnit.NON } ?: break
        loadAppItemList(context, item)
    }
}

internal fun ApiViewingViewModel.loadAppItemList(context: Context, item: Int) {
    val viewModel = this
    when (item) {
        ApiUnit.USER -> {
            loadedItems.loading(item)
            viewModel.addUserApps(context)
            loadedItems.finish(item)
        }
        ApiUnit.SYS -> {
            loadedItems.loading(item)
            viewModel.addSystemApps(context)
            loadedItems.finish(item)
        }
        ApiUnit.ALL_APPS -> {
            val bUser = loadedItems.shouldLoad(ApiUnit.USER)
            val bSys = loadedItems.shouldLoad(ApiUnit.SYS)
            loadedItems.loading(item)  // placed after loadedItems.shouldLoad
            if (bUser && bSys){
                viewModel.addAllApps(context)
            } else if (bUser){
                viewModel.addUserApps(context)
            } else if (bSys){
                viewModel.addSystemApps(context)
            }
            loadedItems.finish(item)
        }
    }
}

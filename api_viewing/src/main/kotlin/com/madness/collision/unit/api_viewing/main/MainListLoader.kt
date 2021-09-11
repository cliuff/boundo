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
import android.os.Environment
import androidx.lifecycle.LifecycleOwner
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.madness.collision.unit.api_viewing.ApiViewingViewModel
import com.madness.collision.unit.api_viewing.MyUnit
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.databinding.FragmentApiBinding
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.util.os.OsUtils
import java.io.File

internal class MainListLoader(
    private val host: MyUnit,
    // external data dependencies
    private val dataConfig: MainDataConfig,
) : LifecycleOwner by host {

    // external context dependencies
    lateinit var context: Context
    lateinit var viewBinding: FragmentApiBinding
    lateinit var viewModel: ApiViewingViewModel  // AndroidViewModel
    lateinit var listFragment: AppListFragment
    lateinit var apkRetriever: ApkRetriever

    private var sortItem: Int by dataConfig::sortItem
    private val refreshLayout: SwipeRefreshLayout
        get() = viewBinding.apiSwipeRefresh
    private val loadedItems: ApiUnit
        get() = viewModel.loadedItems

    /**
     * Load items defined in [ApiUnit] (i.e. excluding various launch modes).
     */
    fun loadSortedStandardList(item: Int, sortEfficiently: Boolean, fg: Boolean, appItemLoader: (item: Int) -> Unit): Int? {
        if (!loadedItems.shouldLoad(item)) {
            if (!sortEfficiently && fg) {
                viewModel.sortApps(sortItem)
                listFragment.updateList(viewModel.apps4Cache, refreshLayout)
            }
            return null
        }
        appItemLoader(item)
        if (fg) viewModel.sortApps(sortItem)
        val item2Load = loadedItems.item2Load()
        if (item2Load == ApiUnit.NON) return null
        return item2Load
    }

    fun loadAppItemList(context: Context, item: Int) {
        when (item) {
            ApiUnit.USER -> {
                loadedItems.loading(item)
                viewModel.addUserApps(context)
            }
            ApiUnit.SYS -> {
                loadedItems.loading(item)
                viewModel.addSystemApps(context)
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
            }
        }
        loadedItems.finish(item)
    }

    fun loadDeviceApks(context: Context, block: (apk: Any) -> Unit) {
        if (OsUtils.satisfy(OsUtils.Q)) {
            // user selected primary storage to scan APKs
            host.accessiblePrimaryExternal(context) { treeUri ->
                apkRetriever.fromUri(treeUri, block)
            }
        } else {
            try {
                val externalStorage = getExternalStorageDirectory()
                apkRetriever.fromFileFolder(externalStorage) { block(it.path) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getExternalStorageDirectory(): File {
        return Environment.getExternalStorageDirectory()
    }
}

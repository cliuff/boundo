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

package com.madness.collision.unit.api_viewing.ui.list

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.databinding.AvLegacyAppListBinding
import com.madness.collision.unit.api_viewing.list.AppListFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun LegacyAppList(
    appList: List<ApiViewingApp>,
    options: AppListOptions,
    loadedCats: Set<ListSrcCat>,
    headerState: ListHeaderState,
    paddingValues: PaddingValues,
) {
    val scrollListener = remember { AppListOnScrollListener() }
    LaunchedEffect(appList, headerState) {
        snapshotFlow { appList.size }.onEach(headerState::statsSize::set).launchIn(this)
        snapshotFlow { scrollListener.absScrollY }.onEach(headerState::updateOffsetY).launchIn(this)
    }
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))) {
        AppListView {
            getAdapter().topCover = headerState.headerHeight
            getAdapter().setSortMethod(options.listOrder.code)
            updateList(appList)
            getRecyclerView().removeOnScrollListener(scrollListener)
            getRecyclerView().addOnScrollListener(scrollListener)
        }

        AppListSwitchHeader(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            options = options,
            loadedSrc = loadedCats,
            headerState = headerState,
        )
    }
}

@Composable
private fun AppListView(update: AppListFragment.() -> Unit) {
    var listFragment: AppListFragment? by remember { mutableStateOf(null) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val con = AvLegacyAppListBinding.inflate(LayoutInflater.from(ctx)).root
            con.also { listFragment = con.getFragment() }
        },
        update = { fContainer ->
            update(requireNotNull(listFragment))
        },
    )
}

private class AppListOnScrollListener : RecyclerView.OnScrollListener() {
    var absScrollY by mutableIntStateOf(0)
        private set
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        absScrollY = (absScrollY + dy).coerceAtLeast(0)
    }
}

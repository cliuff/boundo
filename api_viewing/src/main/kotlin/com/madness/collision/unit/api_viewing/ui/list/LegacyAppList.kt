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

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import androidx.recyclerview.widget.RecyclerView
import coil.compose.rememberAsyncImagePainter
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.util.F
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import kotlin.math.roundToInt

@Composable
fun LegacyAppList(
    appList: List<ApiViewingApp>,
    appListPrefs: Int,
    options: AppListOptions,
    appSrcState: AppSrcState,
    headerState: ListHeaderState,
    paddingValues: PaddingValues,
) {
    val scrollListener = remember { AppListOnScrollListener() }
    LaunchedEffect(appList, headerState) {
        snapshotFlow { appList.size }.onEach(headerState::statsSize::set).launchIn(this)
        snapshotFlow { scrollListener.absScrollY }.onEach(headerState::updateOffsetY).launchIn(this)
    }
    Box() {
        val hazeState = remember { HazeState() }
        if (!LocalInspectionMode.current) {
            val context = LocalContext.current
            val imgFile by produceState(null as File?) img@{
                val f = F.createFile(F.valFilePubExterior(context), "Art_ListHeader.jpg")
                if (f.exists()) return@img kotlin.run { value = f }
                val sealIndex = Utils.getAndroidLetterByAPI(Build.VERSION.SDK_INT)
                value = SealMaker.getBlurredCacheFile(sealIndex) ?: kotlin.run {
                    val itemWidth = 45.dp.value * context.resources.displayMetrics.density
                    SealMaker.getBlurredFile(context, sealIndex, itemWidth.roundToInt())
                }
            }
            Image(
                modifier = Modifier.fillMaxWidth().haze(hazeState),
                painter = rememberAsyncImagePainter(imgFile),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        val contentInsetTop = paddingValues.calculateTopPadding() + 110.dp
        val density = LocalDensity.current
        val contentHeight by remember {
            derivedStateOf {
                (headerState.headerHeight / density.density - contentInsetTop.value)
                    .coerceAtLeast(0f).dp
            }
        }
        Box(modifier = Modifier
            .padding(top = contentInsetTop)
            .offset { IntOffset(0, headerState.headerOffsetY) }) {

            val headerOverlapSize = 30.dp
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight + headerOverlapSize)
                .hazeChild(hazeState))

            // backdrop for app list, overlaps on the blurred background
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(top = contentHeight)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.background))
        }

        var lastAppList: List<ApiViewingApp> by remember { mutableStateOf(emptyList()) }
        var lastAppListPrefs by remember { mutableIntStateOf(0) }
        val backdropPadding = with(LocalDensity.current) { 20.dp.toPx().roundToInt() }
        var listFragment: AppListFragment? by remember { mutableStateOf(null) }
        AndroidFragment<AppListFragment>(modifier = Modifier.fillMaxSize()) { listFragment = it }
        LaunchedEffect(listFragment, appList, appListPrefs) {
            listFragment?.run {
                getAdapter().topCover = headerState.headerHeight + backdropPadding
                getAdapter().setSortMethod(options.listOrder.code)
                if (appList !== lastAppList || appListPrefs != lastAppListPrefs) {
                    lastAppListPrefs = appListPrefs
                    lastAppList = appList
                    updateList(appList)
                }
                getRecyclerView().removeOnScrollListener(scrollListener)
                getRecyclerView().addOnScrollListener(scrollListener)
            }
        }

        // header must be on top of fragment to be able to interact with
        AppListSwitchHeader(
            modifier = Modifier.padding(top = contentInsetTop),
            options = options,
            appSrcState = appSrcState,
            headerState = headerState,
        )
    }
}

private class AppListOnScrollListener : RecyclerView.OnScrollListener() {
    var absScrollY by mutableIntStateOf(0)
        private set
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        absScrollY = (absScrollY + dy).coerceAtLeast(0)
    }
}

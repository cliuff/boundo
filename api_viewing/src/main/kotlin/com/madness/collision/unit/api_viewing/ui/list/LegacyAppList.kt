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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import androidx.recyclerview.widget.RecyclerView
import coil.compose.rememberAsyncImagePainter
import com.madness.collision.chief.lang.runIf
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.list.AppListFragment
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.F
import com.madness.collision.util.mainApplication
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val backdropHeight = maxHeight
        val hazeState = remember { HazeState() }
        if (!LocalInspectionMode.current) {
            val context = LocalContext.current
            val imgFile by produceState(null as File?) img@{
                val f = F.createFile(F.valFilePubExterior(context), "Art_ListHeader.jpg")
                if (f.exists()) return@img kotlin.run { value = f }
                val sealIndex = Utils.getDevCodenameLetter()
                    ?: Utils.getAndroidLetterByAPI(Build.VERSION.SDK_INT)
                value = SealMaker.getBlurredCacheFile(sealIndex) ?: kotlin.run {
                    val itemWidth = 45.dp.value * context.resources.displayMetrics.density
                    SealMaker.getBlurredFile(context, sealIndex, itemWidth.roundToInt())
                }
            }
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (headerState.headerOffsetY * 0.5f).roundToInt()) }
                    .haze(hazeState),
                painter = rememberAsyncImagePainter(imgFile),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        val contentInsetTop = paddingValues.calculateTopPadding() + 110.dp
        val density = LocalDensity.current
        val contentHeight by remember(headerState.headerHeight) {
            derivedStateOf {
                (headerState.headerHeight / density.density - contentInsetTop.value)
                    .coerceAtLeast(0f).dp
            }
        }
        val contentInsetTopPx = with(density) { contentInsetTop.roundToPx() }
        Box(modifier = Modifier
            // apply contentInsetTop to offset instead of padding to break height constraint
            .offset { IntOffset(0, headerState.headerOffsetY + contentInsetTopPx) }) {

            val headerOverlapSize = 30.dp
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight + headerOverlapSize)
                .hazeChild(hazeState))

            // remove corners for asymmetric paddings (e.g. landscape with 3-button nav)
            val flatBackdrop = LocalLayoutDirection.current.let { di ->
                paddingValues.run { calculateStartPadding(di) != calculateEndPadding(di) }
            }
            val backdropColor = when (LocalInspectionMode.current) {
                true -> if (isSystemInDarkTheme()) Color.Black else Color.White
                false -> if (mainApplication.isDarkTheme) Color.Black else Color.White
            }
            // backdrop for app list, overlaps on the blurred background
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = contentHeight)
                .height(backdropHeight)
                .runIf({ !flatBackdrop },
                    { clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) })
                .background(backdropColor))
        }

        // some shade to make status bar visible
        Column() {
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(
                    0f to Color(0x10000000), 1f to Color(0x09000000)))
                .height(paddingValues.calculateTopPadding()))
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(
                    0f to Color(0x09000000), 1f to Color(0x00000000)))
                .height(30.dp))
        }

        var lastAppList: List<ApiViewingApp> by remember { mutableStateOf(emptyList()) }
        var lastAppListPrefs by remember { mutableIntStateOf(0) }
        val backdropPadding = with(LocalDensity.current) { 20.dp.roundToPx() }
        val bottomPadding = with(density) { paddingValues.calculateBottomPadding().roundToPx() }

        val contentPadding = paddingValues.run {
            PaddingValues(
                start = calculateStartPadding(LocalLayoutDirection.current),
                end = calculateEndPadding(LocalLayoutDirection.current),
            )
        }
        var listFragment: AppListFragment? by remember { mutableStateOf(null) }
        AndroidFragment<AppListFragment>(
            modifier = Modifier.fillMaxSize().padding(contentPadding)) { listFragment = it }
        LaunchedEffect(listFragment, headerState.headerHeight, paddingValues) {
            listFragment?.run {
                getAdapter().topCover = headerState.headerHeight + backdropPadding
                getAdapter().bottomCover = asBottomMargin(bottomPadding)
                getAdapter().notifyItemChanged(0)
            }
        }
        LaunchedEffect(listFragment, appList, appListPrefs) {
            listFragment?.run {
                getAdapter().topCover = headerState.headerHeight + backdropPadding
                getAdapter().bottomCover = asBottomMargin(bottomPadding)
                getAdapter().setSortMethod(options.listOrder.code)
                if (appList !== lastAppList || appListPrefs != lastAppListPrefs) {
                    lastAppListPrefs = appListPrefs
                    lastAppList = appList
                    // reset the position and offset to make them match before updating app list
                    scrollToTop()
                    scrollListener.resetScrollY()
                    updateList(appList)
                }
                getRecyclerView().removeOnScrollListener(scrollListener)
                getRecyclerView().addOnScrollListener(scrollListener)
            }
        }

        val showCatSwitcher by remember(appSrcState.loadedCats) {
            derivedStateOf { appSrcState.loadedCats.singleOrNull() != ListSrcCat.Platform }
        }
        LaunchedEffect(showCatSwitcher) {
            // reset the position and offset to make them match on switcher show/hide
            listFragment?.scrollToTop()
            scrollListener.resetScrollY()
        }

        // header must be on top of fragment to be able to interact with
        AppListSwitchHeader(
            modifier = Modifier.padding(top = contentInsetTop).padding(contentPadding),
            options = options,
            appSrcState = appSrcState,
            headerState = headerState,
        )
    }
}

private class AppListOnScrollListener : RecyclerView.OnScrollListener() {
    var absScrollY by mutableIntStateOf(0)
        private set

    fun resetScrollY() { absScrollY = 0 }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        absScrollY = (absScrollY + dy).coerceAtLeast(0)
    }
}

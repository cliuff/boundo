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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.ui.comp.AppItemPrefs
import com.madness.collision.unit.api_viewing.ui.comp.LocalAppItemPrefs
import com.madness.collision.unit.api_viewing.ui.list.comp.ListHeaderBackdrop
import com.madness.collision.unit.api_viewing.ui.list.comp.ListHeaderImage
import com.madness.collision.unit.api_viewing.ui.list.comp.ListHeaderVerticalShade
import com.madness.collision.unit.api_viewing.ui.upd.CompactAppItemStyle
import com.madness.collision.unit.api_viewing.ui.upd.DefaultAppItemStyle
import com.madness.collision.unit.api_viewing.ui.upd.LocalAppItemStyle
import com.madness.collision.util.mainApplication
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListGrid(
    appList: List<ApiViewingApp>,
    onClickApp: (ApiViewingApp) -> Unit,
    getMaxSpan: () -> Int,
    listConfig: AppListConfig,
    options: AppListOptions,
    appSrcState: AppSrcState,
    headerState: ListHeaderState,
    appBarState: TopAppBarState,
    scrollState: LazyGridState = rememberLazyGridState(),
    paddingValues: PaddingValues = PaddingValues(),
) {
    LaunchedEffect(appList, headerState) {
        snapshotFlow { appList.size }.onEach(headerState::statsSize::set).launchIn(this)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val headerSpace = (maxHeight / 3)
            // reserve 300dp safe space for content
            .coerceAtMost(maxHeight - 300.dp)
            .coerceIn(paddingValues.calculateTopPadding(), 200.dp)
        var headerBackdropHeightPx by remember { mutableIntStateOf(0) }

        val backdropColor = when (LocalInspectionMode.current) {
            true -> if (isSystemInDarkTheme()) Color.Black else Color.White
            false -> if (mainApplication.isDarkTheme) Color.Black else Color.White
        }

        val hazeState = remember { HazeState() }

        if (!LocalInspectionMode.current) {
            val relativeOffsetY by remember(appBarState) {
                derivedStateOf {
                    if (scrollState.firstVisibleItemIndex == 0) {
                        // use first item offset when scrolled to top
                        (scrollState.firstVisibleItemScrollOffset * -0.5f).roundToInt()
                    } else {
                        (appBarState.contentOffset * 0.5f).roundToInt()
                    }
                }
            }

            val density = LocalDensity.current
            val headerHeight by remember {
                derivedStateOf {
                    // use default image height when backdrop not rendered yet
                    if (headerBackdropHeightPx <= 0) return@derivedStateOf Dp.Unspecified
                    val backdropDp = (headerBackdropHeightPx + relativeOffsetY) / density.density
                    (headerSpace + backdropDp.dp).coerceAtLeast(0.dp)
                }
            }

            ListHeaderImage(
                modifier = Modifier
                    .offset { IntOffset(0, relativeOffsetY) }
                    .hazeSource(hazeState),
                height = headerHeight,
                backdropColor = backdropColor,
            )
        }

        val horizontalPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
        )
        val lazyPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
            top = headerSpace,
            bottom = paddingValues.calculateBottomPadding(),
        )
        val horizontalPaddingSum = LocalLayoutDirection.current.let { di ->
            paddingValues.run { calculateLeftPadding(di) + calculateRightPadding(di) }
        }

        val maxSpan by remember(scrollState) {
            derivedStateOf {
                scrollState.layoutInfo.maxSpan.coerceAtLeast(1)
            }
        }
        val appItemStyle = when {
            maxSpan > 1 -> DefaultAppItemStyle
            maxWidth - horizontalPaddingSum >= 360.dp -> DefaultAppItemStyle
            else -> CompactAppItemStyle
        }
        val appItemPrefs = remember(options.apiMode, listConfig.itemPrefs) {
            AppItemPrefs(
                apiMode = options.apiMode,
                tagPrefs = listConfig.itemPrefs,
            )
        }

        CompositionLocalProvider(
            LocalAppItemStyle provides appItemStyle,
            LocalAppItemPrefs provides appItemPrefs,
        ) {
            // remove corners for asymmetric paddings (e.g. landscape with 3-button nav)
            val flatBackdrop = LocalLayoutDirection.current.let { di ->
                paddingValues.run { calculateStartPadding(di) != calculateEndPadding(di) }
            }
            LazyAppGrid(
                columnCount = remember { getMaxSpan() },
                apps = appList,
                onClickApp = onClickApp,
                backdropColor = backdropColor,
                scrollState = scrollState,
                contentPadding = lazyPadding,
                header = {
                    ListHeaderBackdrop(
                        modifier = Modifier
                            .onSizeChanged { headerBackdropHeightPx = it.height }
                            .padding(horizontalPadding),
                        color = backdropColor,
                        cornerSize = if (flatBackdrop) 0.dp else 20.dp,
                        hazeState = hazeState,
                    ) {
                        AppListSwitchHeader(
                            modifier = Modifier.padding(horizontalPadding),
                            options = options,
                            appSrcState = appSrcState,
                            headerState = headerState,
                        )
                    }
                },
            )
        }

        // some shade to make status bar visible
        ListHeaderVerticalShade(size = paddingValues.calculateTopPadding())

        var topScrollInit by remember { mutableIntStateOf(0) }

        LaunchedEffect(appList) {
            if (topScrollInit++ > 0) {
                scrollState.scrollToItem(0)
                // reset nested scrolling to match
                appBarState.contentOffset = 0f
            }
        }
    }
}

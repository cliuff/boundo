/*
 * Copyright 2025 Clifford Liu
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.info.AppInfo
import com.madness.collision.unit.api_viewing.info.ExpIcon
import com.madness.collision.unit.api_viewing.ui.comp.AppItem
import com.madness.collision.unit.api_viewing.ui.comp.LocalAppItemPrefs
import com.madness.collision.unit.api_viewing.ui.comp.toGuiArt
import com.madness.collision.unit.api_viewing.ui.upd.AppTagGroup
import com.madness.collision.unit.api_viewing.ui.upd.EmptyTagGroup
import kotlinx.coroutines.flow.map

@Composable
fun LazyAppGrid(
    columnCount: Int,
    apps: List<ApiViewingApp>,
    onClickApp: (ApiViewingApp) -> Unit,
    backdropColor: Color,
    header: @Composable () -> Unit,
    switcher: @Composable () -> Unit,
    scrollState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val paddingStart = contentPadding.calculateStartPadding(LocalLayoutDirection.current)
    val paddingEnd = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
    val verticalPadding = PaddingValues(
        top = contentPadding.calculateTopPadding(),
        bottom = contentPadding.calculateBottomPadding(),
    )
    val maxSpan by remember(scrollState) {
        derivedStateOf {
            scrollState.layoutInfo.maxSpan.coerceAtLeast(1)
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = scrollState,
        contentPadding = verticalPadding,
    ) {
        item(key = "list.header", span = { GridItemSpan(maxLineSpan) }, contentType = "Header") {
            header()
        }

        item(key = "list.switcher", span = { GridItemSpan(maxLineSpan) }, contentType = "Switcher") {
            Box(modifier = Modifier.animateItem().background(backdropColor).padding(bottom = 12.dp)) {
                switcher()
            }
        }

        val (itemPadStart, itemPadEnd) =
            3.dp + paddingStart.coerceAtLeast(7.dp) to 3.dp + paddingEnd.coerceAtLeast(7.dp)
        itemsIndexed(
            apps,
            key = { _, app -> app.appPackage.basePath },
            contentType = { _, _ -> "App" }) { i, app ->
            val idxInLine = if (maxSpan <= 1) 0 else i % maxSpan
            AppListItem(
                modifier = Modifier
                    .animateItem()
                    .background(backdropColor)
                    .padding(
                        start = if (idxInLine == 0) itemPadStart else 0.dp,
                        end = if (idxInLine == maxSpan - 1) itemPadEnd else 0.dp)
                    .padding(horizontal = 5.dp, vertical = 0.dp)
                    .padding(bottom = 10.dp),
                app = app,
                onClickApp = onClickApp,
            )
        }
    }
}

@Composable
private fun AppListItem(
    app: ApiViewingApp,
    onClickApp: (ApiViewingApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val itemPrefs = LocalAppItemPrefs.current
    val art = remember(app) { app.toGuiArt(context) }

    val tagGroupFlow = remember(app, itemPrefs.tagPrefs) {
        AppInfo.getExpTags(app, context).map { tags ->
            val (ic, tx) = tags.partition { t -> t.icon !is ExpIcon.Text }
            AppTagGroup(ic, tx)
        }
    }
    val tagGroup by tagGroupFlow.collectAsStateWithLifecycle(EmptyTagGroup)

    AppItem(
        modifier = modifier,
        art = art,
        tagGroup = tagGroup,
        onClick = { onClickApp(app) },
    )
}

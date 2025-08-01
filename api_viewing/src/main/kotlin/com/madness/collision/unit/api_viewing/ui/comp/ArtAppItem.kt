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

package com.madness.collision.unit.api_viewing.ui.comp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madness.collision.chief.app.stateOf
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.unit.api_viewing.ui.list.AppApiMode
import com.madness.collision.unit.api_viewing.ui.upd.AppItemStyle
import com.madness.collision.unit.api_viewing.ui.upd.AppTagGroup
import com.madness.collision.unit.api_viewing.ui.upd.LocalAppItemStyle
import com.madness.collision.unit.api_viewing.ui.upd.item.GuiArt.Identity

@Immutable
internal data class GuiArtApp(
    val identity: Identity,
    val compileApiInfo: VerInfo,
    val targetApiInfo: VerInfo,
    val minApiInfo: VerInfo,
    val updateTime: Long,
)

@Stable
val DefaultAppItemPrefs =
    AppItemPrefs(
        apiMode = AppApiMode.Target,
        tagPrefs = 0,
    )

@Immutable
data class AppItemPrefs(
    val apiMode: AppApiMode,
    val tagPrefs: Int,
)

val LocalAppItemPrefs = compositionLocalOf { DefaultAppItemPrefs }

@Composable
internal fun AppItem(
    art: GuiArtApp,
    tagGroup: AppTagGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prefs: AppItemPrefs = LocalAppItemPrefs.current,
    style: AppItemStyle = LocalAppItemStyle.current,
) {
    val context = LocalContext.current
    val apiInfo = remember(art, prefs.apiMode) {
        when (prefs.apiMode) {
            AppApiMode.Compile -> art.compileApiInfo
            AppApiMode.Target -> art.targetApiInfo
            AppApiMode.Minimum -> art.minApiInfo
        }
    }
    val (backColor, accentColor) = remember(apiInfo) {
        SealMaker.getItemColorBack(context, apiInfo.api) to
                SealMaker.getItemColorAccent(context, apiInfo.api)
    }

    val (initTime, relTimeFlow) = remember(art.updateTime) {
        ArtMapper.getRelativeTimeUpdates(art.updateTime)
    }
    val relativeTime by relTimeFlow?.collectAsStateWithLifecycle(initTime)
        ?: remember(initTime) { stateOf(initTime) }

    com.madness.collision.unit.api_viewing.ui.upd.AppItem(
        modifier = modifier,
        name = art.identity.label,
        time = relativeTime,
        apiText = apiInfo.displaySdk,
        sealLetter = apiInfo.letterOrDev,
        iconInfo = art.identity.iconPkgInfo,
        tagGroup = tagGroup,
        cardColor = Color(backColor),
        apiColor = Color(accentColor),
        style = style,
        onClick = onClick,
    )
}

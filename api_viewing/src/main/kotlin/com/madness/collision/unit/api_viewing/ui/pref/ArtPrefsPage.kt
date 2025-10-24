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

package com.madness.collision.unit.api_viewing.ui.pref

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.layout.scaffoldWindowInsets
import com.madness.collision.main.showPage
import com.madness.collision.ui.comp.ClassicTopAppBar
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.ui.theme.PreviewAppTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.showTagsPrefPopup
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.madness.collision.R as MainR

@Composable
fun ArtPrefsPage() {
    val (topBarInsets, _, contentInsets) =
        scaffoldWindowInsets(shareCutout = 12.dp, shareStatusBar = 5.dp, shareWaterfall = 8.dp)
    Scaffold(
        topBar = {
            ClassicTopAppBar(
                title = { Text(text = stringResource(MainR.string.apiViewer)) },
                windowInsets = topBarInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        containerColor = MetaAppTheme.colorScheme.surfaceNeutral,
        contentWindowInsets = contentInsets,
    ) { innerPadding ->
        ArtPrefsContent(contentPadding = innerPadding)
    }
}

@Immutable
data class ArtPrefsUiState(
    val isTextProcessingEnabled: Boolean,
)

@Stable
interface ArtPrefsEventHandler {
    fun showTagsPref()
    fun setTextProcessingEnabled(enabled: Boolean)
}

@Composable
fun rememberArtPrefsEventHandler(): ArtPrefsEventHandler {
    val viewModel = viewModel<ArtPrefsViewModel>()
    val context = LocalContext.current
    return remember<ArtPrefsEventHandler> {
        object : ArtPrefsEventHandler {
            override fun showTagsPref() = showTagsPrefPopup(context)
            override fun setTextProcessingEnabled(enabled: Boolean) =
                viewModel.setTextProcessingEnabled(enabled)
        }
    }
}

@Composable
fun ArtPrefsContent(
    modifier: Modifier = Modifier,
    eventHandler: ArtPrefsEventHandler = rememberArtPrefsEventHandler(),
    scrollState: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val viewModel = viewModel<ArtPrefsViewModel>()
    LaunchedEffect(Unit) { viewModel.init() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArtPrefs(
        isTextProcessingEnabled = uiState.isTextProcessingEnabled,
        eventHandler = eventHandler,
        modifier = modifier,
        scrollState = scrollState,
        contentPadding = contentPadding,
    )
}

@Composable
private fun ArtPrefs(
    isTextProcessingEnabled: Boolean,
    eventHandler: ArtPrefsEventHandler,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(contentPadding)
    ) {
        val itemColors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

        // "List displaying" category
        PreferenceCategory(title = stringResource(R.string.av_pref_displaying))
        Card(onClick = eventHandler::showTagsPref, shape = AbsoluteSmoothCornerShape(16.dp, 60)) {
            PreferenceItem(title = stringResource(R.string.av_settings_tags), colors = itemColors)
        }

        // "More" category
        PreferenceCategory(title = stringResource(MainR.string.main_more))
        val context = LocalContext.current
        Card(
            onClick = { context.showPage<DiffHistoryFragment>() },
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = 16.dp, cornerRadiusTR = 16.dp,
                cornerRadiusBL = 4.dp, cornerRadiusBR = 4.dp),
        ) {
            PreferenceItem(title = stringResource(R.string.av_settings_diff), colors = itemColors)
        }

        Spacer(modifier = Modifier.height(2.dp))
        Card(
            enabled = isTextProcessingModEnabled(),
            onClick = { eventHandler.setTextProcessingEnabled(!isTextProcessingEnabled) },
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = 4.dp, cornerRadiusTR = 4.dp,
                cornerRadiusBL = 16.dp, cornerRadiusBR = 16.dp),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.av_settings_text_process)) },
                supportingContent = { Text(stringResource(R.string.av_settings_text_process_desc)) },
                trailingContent = {
                    Switch(
                        enabled = isTextProcessingModEnabled(),
                        checked = isTextProcessingEnabled,
                        onCheckedChange = {
                            eventHandler.setTextProcessingEnabled(!isTextProcessingEnabled)
                        }
                    )
                },
                colors = itemColors,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun isTextProcessingModEnabled() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

@Composable
private fun PreferenceTrailingIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
        contentDescription = null,
    )
}

@Composable
private fun PreferenceItem(title: String, colors: ListItemColors) {
    ListItem(
        headlineContent = { Text(modifier = Modifier.padding(vertical = 10.dp), text = title) },
        trailingContent = { PreferenceTrailingIcon() },
        colors = colors,
    )
}

@Composable
private fun PreferenceCategory(title: String) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

internal fun PseudoArtPrefsEventHandler(): ArtPrefsEventHandler =
    object : ArtPrefsEventHandler {
        override fun showTagsPref() = Unit
        override fun setTextProcessingEnabled(enabled: Boolean) = Unit
    }

@Composable
@PreviewCombinedColorLayout
private fun ArtPrefsContentPreview() {
    PreviewAppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            ArtPrefs(
                isTextProcessingEnabled = true,
                eventHandler = PseudoArtPrefsEventHandler(),
            )
        }
    }
}

/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.madness.collision.R
import com.madness.collision.chief.app.LocalPageNavController
import com.madness.collision.chief.app.asInsets
import com.madness.collision.chief.lang.runIf
import com.madness.collision.chief.layout.scaffoldWindowInsets
import com.madness.collision.main.DevOptions
import com.madness.collision.ui.comp.ClassicTopAppBar
import com.madness.collision.ui.settings.LanguagesContent
import com.madness.collision.ui.settings.StylesContent
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.ui.theme.PreviewAppTheme
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import kotlinx.coroutines.delay

@Composable
fun SettingsPage() {
    val (topBarInsets, _, contentInsets) =
        scaffoldWindowInsets(shareCutout = 12.dp, shareStatusBar = 5.dp, shareWaterfall = 8.dp)
    Scaffold(
        topBar = {
            ClassicTopAppBar(
                title = { Text(stringResource(R.string.Main_ToolBar_title_Settings)) },
                windowInsets = topBarInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        containerColor = MetaAppTheme.colorScheme.surfaceNeutral,
        contentWindowInsets = contentInsets,
    ) { innerPadding ->
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
        if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            BoxWithConstraints {
                val navWidth = (maxWidth / 3).coerceAtMost(240.dp)
                SideBySideLayout(navPaneWidth = navWidth, contentPadding = innerPadding)
            }
        } else {
            SinglePaneLayout(contentPadding = innerPadding)
        }
    }
}

private enum class NavDest {
    Styles, Languages, ApiUnit, About
}

@Composable
private fun SinglePaneLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val navController = LocalPageNavController.current
    SettingsNavPanel(
        modifier = modifier,
        selectedDest = null,
        contentPadding = contentPadding,
        onSelectDest = { dest ->
            when (dest) {
                NavDest.Styles -> navController.navigateTo(SettingsRouteId.Styles.asRoute())
                NavDest.Languages -> navController.navigateTo(SettingsRouteId.Languages.asRoute())
                NavDest.About -> navController.navigateTo(SettingsRouteId.About.asRoute())
                NavDest.ApiUnit -> navController.navigateTo(AccessAV.getPrefsRoute())
            }
        },
    )
}

@Composable
private fun SideBySideLayout(
    navPaneWidth: Dp,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val (navDest, setNavDest) = rememberSaveable {
        mutableStateOf(NavDest.Styles)
    }
    val navPanePadding = contentPadding.asInsets()
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
        .asPaddingValues()
    val contentPanePadding = contentPadding.asInsets()
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
        .asPaddingValues()

    Row(modifier = modifier) {
        SettingsNavPanel(
            modifier = Modifier.width(navPaneWidth),
            selectedDest = navDest,
            onSelectDest = setNavDest,
            contentPadding = navPanePadding,
        )

        VerticalDivider(thickness = 0.5.dp)
        when (navDest) {
            NavDest.Styles -> StylesContent(contentPadding = contentPanePadding)
            NavDest.Languages -> LanguagesContent(contentPadding = contentPanePadding)
            NavDest.About -> AboutContent(contentPadding = contentPanePadding)
            NavDest.ApiUnit -> AccessAV.Prefs(contentPadding = contentPanePadding)
        }
    }
}

@Composable
private fun getSettingsNavOptions() =
    NavDest.entries.map { dest ->
        when (dest) {
            NavDest.Styles -> Icons.Filled.Palette to stringResource(R.string.settings_exterior)
            NavDest.Languages -> Icons.Filled.Language to stringResource(R.string.Settings_Button_SwitchLanguage)
            NavDest.ApiUnit -> Icons.Filled.Android to stringResource(R.string.apiViewer)
            NavDest.About -> Icons.Filled.Info to stringResource(R.string.Main_TextView_Advice_Text)
        }
    }

@Composable
private fun SettingsNavPanel(
    selectedDest: NavDest?,
    onSelectDest: (NavDest) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val options = getSettingsNavOptions()
    val itemShape = RoundedCornerShape(4.dp)
    val itemModifier = when (selectedDest != null) {
        true -> Modifier.padding(horizontal = 8.dp, vertical = 4.dp).clip(itemShape)
        false -> Modifier
    }
    val itemPadding = when (selectedDest != null) {
        true -> PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        false -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(top = 6.dp, bottom = 10.dp)
        ) {
            NavDest.entries.forEachIndexed { index, navDest ->
                val (icon, label) = options[index]
                if (index != 0) {
                    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 70.dp),
                        color = dividerColor,
                        thickness = 0.5.dp,
                    )
                }
                SettingsItem(
                    modifier = itemModifier
                        .runIf({ navDest == selectedDest }) {
                            background(MaterialTheme.colorScheme.surface, itemShape)
                        },
                    icon = icon,
                    label = label,
                    isTertiary = index == 0,
                    onClick = { onSelectDest(navDest) },
                    contentPadding = itemPadding,
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTertiary: Boolean = false,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val colorScheme = MaterialTheme.colorScheme
    val iconTint: Color
    val iconContainerColor: Color
    if (isTertiary) {
        var iconTintA by remember(key1 = label) {
            mutableStateOf(colorScheme.onPrimaryContainer)
        }
        var iconContainerColorA by remember(key1 = label) {
            mutableStateOf(colorScheme.primaryContainer)
        }
        LaunchedEffect(key1 = label) {
            delay(200)
            iconTintA = colorScheme.onTertiaryContainer
            iconContainerColorA = colorScheme.tertiaryContainer
        }
        iconTint = animateColorAsState(targetValue = iconTintA).value
        iconContainerColor = animateColorAsState(targetValue = iconContainerColorA).value
    } else {
        iconTint = colorScheme.onPrimaryContainer
        iconContainerColor = colorScheme.primaryContainer
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .run {
                if (!isTertiary) return@run clickable(onClick = onClick)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                combinedClickable(
                    onLongClick = { DevOptions(scope).show(context) },
                    onClick = onClick
                )
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(iconContainerColor)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
@PreviewCombinedColorLayout
private fun SettingsPagePreview() {
    PreviewAppTheme {
        Surface(color = MetaAppTheme.colorScheme.surfaceNeutral) {
            SettingsNavPanel(selectedDest = NavDest.Styles, onSelectDest = {})
        }
    }
}

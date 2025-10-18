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

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.madness.collision.R
import com.madness.collision.settings.instant.InstantFragment
import com.madness.collision.main.DevOptions
import com.madness.collision.main.MainViewModel
import com.madness.collision.main.showPage
import com.madness.collision.pref.PrefExterior
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.Unit as ModUnit
import com.madness.collision.util.Page
import com.madness.collision.util.dev.DarkPreview
import com.madness.collision.util.dev.LayoutDirectionPreviews
import kotlinx.coroutines.delay

@Composable
fun SettingsPage(
    mainViewModel: MainViewModel,
    paddingValues: PaddingValues,
    showLanguages: () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        BoxWithConstraints {
            val navWidth = (maxWidth / 3).coerceAtMost(240.dp)
            SideBySideLayout(navPaneWidth = navWidth, showLanguages = showLanguages, contentPadding = paddingValues)
        }
    } else {
        SinglePaneLayout(showLanguages = showLanguages, contentPadding = paddingValues)
    }
}

private enum class NavDest {
    Styles, Languages, About, Instant, ApiUnit
}

@Composable
private fun SinglePaneLayout(
    showLanguages: () -> Unit,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    SettingsNavPanel(
        mainViewModel = viewModel(activity!!),
        paddingValues = contentPadding,
        onSelectDest = { dest ->
            when (dest) {
                NavDest.Styles ->
                    context.showPage<Page> {
                        putString("fragmentClass", PrefExterior::class.qualifiedName)
                        putInt("titleId", R.string.settings_exterior)
                    }
                NavDest.Languages -> showLanguages()
                NavDest.About -> context.showPage<AdviceFragment>()
                NavDest.Instant -> context.showPage<InstantFragment>()
                NavDest.ApiUnit ->
                    ModUnit.getBridge(ModUnit.UNIT_NAME_API_VIEWING)
                        ?.getSettings()?.let(context::showPage)
            }
        },
    )
}

@Composable
private fun SideBySideLayout(
    navPaneWidth: Dp,
    showLanguages: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val (navDest, setNavDest) = rememberSaveable {
        mutableStateOf<NavDest?>(null)
    }
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(navPaneWidth)
                .padding(contentPadding)
        ) {
            val activity = LocalActivity.current as? ComponentActivity
            SettingsNavPanel(mainViewModel = viewModel(activity!!), onSelectDest = setNavDest)
        }

        VerticalDivider(thickness = 0.5.dp)

        var lastDest by rememberSaveable { mutableStateOf<NavDest?>(null) }
        LaunchedEffect(navDest) { if (navDest != NavDest.Languages) lastDest = navDest }
        if (navDest == NavDest.Languages) LaunchedEffect(Unit) { showLanguages() }

        when (lastDest) {
            NavDest.Styles -> {
                val n = PrefExterior::class.qualifiedName
                val args = bundleOf("fragmentClass" to n, "titleId" to R.string.settings_exterior)
                AndroidFragment<Page>(arguments = args)
            }
            NavDest.Languages -> Unit
            NavDest.About -> AndroidFragment<AdviceFragment>()
            NavDest.Instant -> AndroidFragment<InstantFragment>()
            NavDest.ApiUnit -> {
                val n = "com.madness.collision.unit.api_viewing.PrefAv"
                val args = bundleOf("fragmentClass" to n, "titleId" to R.string.apiViewer)
                AndroidFragment<Page>(arguments = args)
            }
            null -> Unit
        }
    }
}

@Composable
private fun SettingsNavPanel(
    mainViewModel: MainViewModel,
    onSelectDest: (NavDest) -> Unit,
    paddingValues: PaddingValues = PaddingValues.Zero,
) {
    val context = LocalContext.current
    val options = remember {
        val builtIn = listOf(
            Triple(R.string.settings_exterior, R.drawable.ic_palette_24) {
                onSelectDest(NavDest.Styles)
            },
            Triple(R.string.Settings_Button_SwitchLanguage, R.drawable.ic_language_24) {
                onSelectDest(NavDest.Languages)
            },
            Triple(R.string.Main_TextView_Advice_Text, R.drawable.ic_info_24) {
                onSelectDest(NavDest.About)
            },
            Triple(R.string.Main_TextView_Launcher, R.drawable.ic_flash_24) {
                onSelectDest(NavDest.Instant)
            },
        )
        val unitOptions = getUnitOptions(mainViewModel, context)
            .map { (l, i, _) -> Triple(l, i) { onSelectDest(NavDest.ApiUnit) } }
        builtIn + unitOptions
    }
    Settings(options = options, paddingValues = paddingValues)
}

@Composable
private fun Settings(options: List<Triple<Int, Int, () -> Unit>>, paddingValues: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(top = 8.dp, bottom = 10.dp)
        ) {
            options.forEachIndexed { index, (label, icon, onClick) ->
                if (index != 0) {
                    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    Divider(
                        modifier = Modifier.padding(start = 70.dp),
                        color = dividerColor,
                        thickness = 0.5.dp,
                    )
                }
                SettingsItem(
                    icon = icon,
                    label = stringResource(id = label),
                    isTertiary = index == 0,
                    onClick = onClick
                )
            }
        }
    }
}

private fun getUnitOptions(mainViewModel: MainViewModel, context: Context): List<Triple<Int, Int, () -> Unit>> {
    return DescRetriever(context).retrieveInstalled().mapNotNull map@{ state ->
        val unit = state.unitName
        val unitBridge = com.madness.collision.unit.Unit.getBridge(unit) ?: return@map null  // continue
        val settingsPage = unitBridge.getSettings() ?: return@map null  // continue
        val unitDesc = state.description
        Triple(unitDesc.nameResId, unitDesc.iconResId) {
            mainViewModel.displayFragment(settingsPage)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsItem(icon: Int, label: String, isTertiary: Boolean = false, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val iconTint: Color
    val iconContainerColor: Color
    if (isTertiary) {
        var iconTintA by remember(key1 = label) {
            mutableStateOf(colorScheme.primary)
        }
        var iconContainerColorA by remember(key1 = label) {
            mutableStateOf(colorScheme.primaryContainer)
        }
        LaunchedEffect(key1 = label) {
            delay(200)
            iconTintA = colorScheme.tertiary
            iconContainerColorA = colorScheme.tertiaryContainer
        }
        iconTint = animateColorAsState(targetValue = iconTintA).value
        iconContainerColor = animateColorAsState(targetValue = iconContainerColorA).value
    } else {
        iconTint = colorScheme.primary
        iconContainerColor = colorScheme.primaryContainer
    }
    Row(
        modifier = Modifier
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
            .padding(horizontal = 24.dp, vertical = 10.dp),
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
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconTint,
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SettingsPreview() {
    val options = remember {
        listOf(
            Triple(R.string.settings_exterior, R.drawable.ic_palette_24) { },
            Triple(R.string.Settings_Button_SwitchLanguage, R.drawable.ic_language_24) { },
            Triple(R.string.Main_TextView_Advice_Text, R.drawable.ic_info_24) { },
            Triple(R.string.Main_TextView_Launcher, R.drawable.ic_flash_24) { },
            Triple(R.string.apiViewer, R.drawable.ic_android_24) { },
        )
    }
    Settings(options = options, paddingValues = PaddingValues())
}

@LayoutDirectionPreviews
@Composable
private fun SettingsPagePreview() {
    MaterialTheme {
        SettingsPreview()
    }
}

@DarkPreview
@Composable
private fun SettingsPageDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SettingsPreview()
    }
}

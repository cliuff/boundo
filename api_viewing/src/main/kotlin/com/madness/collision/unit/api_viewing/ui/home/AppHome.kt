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

package com.madness.collision.unit.api_viewing.ui.home

import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.List
import androidx.compose.material.icons.twotone.Category
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.FragmentState
import androidx.fragment.compose.rememberFragmentState
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.chief.layout.LocalWindowInsets
import com.madness.collision.chief.layout.scaffoldWindowInsets
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AppHomeFragment : ComposeFragment() {
    private var windowInsetsValue: WindowInsetsCompat by mutableStateOf(WindowInsetsCompat.CONSUMED)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // create empty view for legacy NoUpdatesMode
        if (arguments?.getInt("mode") == 1) return View(inflater.context)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            // return the insets as they are, as we only query not consume
            insets.also { windowInsetsValue = it }
        }
    }

    private fun setStatusBarDarkIcon(isDark: Boolean) {
        val view = view ?: return
        val window = activity?.window ?: return
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isDark
    }

    @Composable
    override fun ComposeContent() {
        // enables platform insets query from compose (e.g. display cutout calculation)
        CompositionLocalProvider(LocalWindowInsets provides windowInsetsValue) {
            MaterialTheme(colorScheme = rememberColorScheme()) {
                AppHomePage(onStatusBarDarkIconChange = ::setStatusBarDarkIcon)
            }
        }
    }
}

@Stable
interface AppHomeNav {
    /** Status bar icons color of nav pages. */
    val statusBarDarkIcon: StateFlow<Boolean>
    fun setNavPage(index: Int)
    fun navBack()
}

@Composable
fun AppHomePage(onStatusBarDarkIconChange: (Boolean) -> Unit) {
    var selNavIndex by rememberSaveable { mutableIntStateOf(0) }
    val (homeNav, setHomeNav) = remember { mutableStateOf<AppHomeNav?>(null) }
    if (selNavIndex != 0 && homeNav != null) {
        BackHandler { selNavIndex = 0; homeNav.setNavPage(0) }
    }
    LaunchedEffect(homeNav) statusBar@{
        val nav = homeNav ?: return@statusBar
        nav.statusBarDarkIcon
            .onEach(onStatusBarDarkIconChange)
            .launchIn(this)
    }
    val (_, bottomBarInsets, contentInsets, sideBarInsets) = scaffoldWindowInsets(
        shareCutout = 6.dp, shareStatusBar = 8.dp, shareWaterfall = 8.dp, shareSideCutout = 8.dp)
    BoxWithConstraints {
        // window size classes, expanded: rail, medium/compact: rail in landscape
        // prefer nav rail in split screen mode (50dp bonus size)
        val useNavRail = maxWidth >= 840.dp || maxWidth + 50.dp >= maxHeight
        // use special scaffold to retain a single nav fragment across different layouts
        AppHomeScaffold(
            modifier = Modifier.fillMaxSize(),
            sideBar = {
                if (useNavRail) {
                    HomeNavigationRail(
                        selectedIndex = selNavIndex,
                        onSelectItem = { i -> selNavIndex = i; homeNav?.setNavPage(i) },
                        windowInsets = sideBarInsets
                            .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                    )
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    HomeNavigationBar(
                        selectedIndex = selNavIndex,
                        onSelectItem = { i -> selNavIndex = i; homeNav?.setNavPage(i) },
                        windowInsets = bottomBarInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                }
            },
            contentWindowInsets = contentInsets,
            content = { contentPadding ->
                // delay one frame to get correct padding with window insets
                var show by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { show = true }
                if (show) {
                    DisposableEffect(Unit) { onDispose { setHomeNav(null) } }
                    HomeNavFragment(selNavIndex, contentPadding = contentPadding, onUpdate = setHomeNav)
                }
            }
        )
    }
}

fun PaddingValues.toRectF(direction: LayoutDirection): RectF {
    return RectF(
        calculateLeftPadding(direction).value,
        calculateTopPadding().value,
        calculateRightPadding(direction).value,
        calculateBottomPadding().value)
}

@Composable
fun HomeNavFragment(
    selectedPageIndex: Int,
    modifier: Modifier = Modifier,
    initArguments: Bundle = Bundle.EMPTY,
    fragmentState: FragmentState = rememberFragmentState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onUpdate: (AppHomeNavFragment) -> Unit,
) {
    val paddingRect = contentPadding.toRectF(LocalLayoutDirection.current)
    var navFragment: AppHomeNavFragment? by remember { mutableStateOf(null) }
    AndroidFragment<AppHomeNavFragment>(
        modifier = modifier,
        fragmentState = fragmentState,
        arguments = remember {
            bundleOf(
                AppHomeNavFragment.ARG_NAV_PAGE to selectedPageIndex,
                AppHomeNavFragment.ARG_NAV_ARGUMENTS to initArguments,
                AppHomeNavFragment.ARG_CONTENT_PADDING to paddingRect,
            )
        },
        onUpdate = { navFgm ->
            onUpdate(navFgm)
            navFragment = navFgm
        }
    )
    LaunchedEffect(navFragment, contentPadding) {
        navFragment?.setContentPadding(contentPadding)
    }
}

@Composable
private fun HomeNavigationBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelectItem: (Int) -> Unit,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    NavigationBar(modifier = modifier, windowInsets = windowInsets) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onSelectItem(0) },
            icon = { Icon(Icons.TwoTone.Home, contentDescription = null) }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onSelectItem(1) },
            icon = { Icon(Icons.AutoMirrored.TwoTone.List, contentDescription = null) }
        )
        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = { onSelectItem(2) },
            icon = { Icon(Icons.TwoTone.Category, contentDescription = null) }
        )
    }
}

@Composable
private fun HomeNavigationRail(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelectItem: (Int) -> Unit,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
) {
    NavigationRail(modifier = modifier, windowInsets = windowInsets) {
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = selectedIndex == 0,
            onClick = { onSelectItem(0) },
            icon = { Icon(Icons.TwoTone.Home, contentDescription = null) }
        )
        NavigationRailItem(
            selected = selectedIndex == 1,
            onClick = { onSelectItem(1) },
            icon = { Icon(Icons.AutoMirrored.TwoTone.List, contentDescription = null) }
        )
        NavigationRailItem(
            selected = selectedIndex == 2,
            onClick = { onSelectItem(2) },
            icon = { Icon(Icons.TwoTone.Category, contentDescription = null) }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@PreviewCombinedColorLayout
@Composable
private fun AppHomePreview() {
    MaterialTheme {
        AppHomeScaffold(
            bottomBar = { HomeNavigationBar(selectedIndex = 0, onSelectItem = {}) },
            content = { _ -> },
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun AppHomeNavRailPreview() {
    MaterialTheme {
        AppHomeScaffold(
            sideBar = { HomeNavigationRail(selectedIndex = 0, onSelectItem = {}) },
            content = { _ -> },
        )
    }
}

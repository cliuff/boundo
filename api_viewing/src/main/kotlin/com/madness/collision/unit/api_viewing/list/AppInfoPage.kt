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

package com.madness.collision.unit.api_viewing.list

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.data.*
import com.madness.collision.unit.api_viewing.env.GooglePlayAppInfoOwner
import com.madness.collision.unit.api_viewing.info.AppInfo
import com.madness.collision.unit.api_viewing.info.ExpressedTag
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater
import com.madness.collision.unit.api_viewing.ui.comp.sealFileOf
import com.madness.collision.unit.api_viewing.ui.info.AppDetailsContent
import com.madness.collision.unit.api_viewing.ui.info.AppSwitcher
import com.madness.collision.unit.api_viewing.ui.info.AppSwitcherHandler
import com.madness.collision.unit.api_viewing.ui.info.LibPage
import com.madness.collision.unit.api_viewing.ui.info.TagDetailsList
import com.madness.collision.util.dev.DarkPreview
import com.madness.collision.util.dev.StandardPreview
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.roundToInt

@Composable
fun AppInfoPage(
    app: ApiViewingApp,
    mainViewModel: MainViewModel,
    hostFragment: DialogFragment,
    shareIcon: () -> Unit,
    shareApk: () -> Unit,
) {
    CompositionLocalProvider(LocalApp provides app) {
        AppInfoPage(mainViewModel, hostFragment, shareIcon, shareApk)
    }
}

// app is unlikely to change
private val LocalApp = staticCompositionLocalOf<ApiViewingApp> { error("App not provided") }

@Composable
private fun AppInfoPage(
    mainViewModel: MainViewModel,
    hostFragment: DialogFragment,
    shareIcon: () -> Unit,
    shareApk: () -> Unit,
) {
    val app = LocalApp.current
    val context = LocalContext.current
    val verInfo = remember { listOf(VerInfo.minDisplay(app), VerInfo.targetDisplay(app), VerInfo(app.compileAPI)) }
    val itemColor = remember { SealMaker.getItemColorBack(context, app.targetAPI) }
    val updateTime = remember {
        run {
            if (app.isArchive) return@run null
            val currentTime = System.currentTimeMillis()
            DateUtils.getRelativeTimeSpanString(app.updateTime, currentTime, DateUtils.MINUTE_IN_MILLIS)
        }?.toString()
    }
    val getClick = { tag: AppTagInfo ->
        when (tag.id) {
            AppTagInfo.ID_APP_INSTALLER_PLAY -> {
                {
                    safely { GooglePlayAppInfoOwner.showAppInfo(app.packageName, context) } ?: Unit
                }
            }
            AppTagInfo.ID_APP_ADAPTIVE_ICON -> {
                {
                    hostFragment.dismiss()
                    val f = AppIconFragment.newInstance(
                        app.name, app.packageName, app.appPackage.basePath, app.isArchive)
                    mainViewModel.displayFragment(f)
                }
            }
            else -> null
        }
    }
    val isDark = mainApplication.isDarkTheme
    AppInfo(Color(itemColor), isDark, verInfo, updateTime, shareIcon, shareApk, getClick)
}

@Composable
private fun AppInfo(
    itemBackColor: Color,
    isDarkTheme: Boolean,
    verInfoList: List<VerInfo>,
    updateTime: String?,
    shareIcon: () -> Unit,
    shareApk: () -> Unit,
    getClick: (AppTagInfo) -> (() -> Unit)?,
) {
    val cardColor = remember { if (isDarkTheme) Color(0xff0c0c0c) else Color.White }
    AppInfoWithHeader(itemBackColor, cardColor, isDarkTheme) {
        BoxWithConstraints {
            val margin = remember {
                if (maxWidth >= 600.dp) 30.dp else if (maxWidth >= 360.dp) 24.dp else 12.dp
            }
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            // use coefficient to invert slide animation offset for RTL layout
            val offsetCoeff = if (isRtl) -1 else 1
            var pageIndex by remember { mutableIntStateOf(0) }
            var predictiveProgress by remember { mutableFloatStateOf(-1f) }
            AnimatedVisibility(
                visible = pageIndex == 0,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { offsetCoeff * -it / 2 }),
                exit = fadeOut(),
            ) {
                val scope = rememberCoroutineScope()
                FrontAppInfo(cardColor, margin, verInfoList, updateTime,
                    { scope.launch { pageIndex = 1 } }, getClick)
            }
            AnimatedVisibility(
                visible = pageIndex == 1,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { offsetCoeff * it / 2 }),
                exit = fadeOut(),
            ) {
                Box(modifier = Modifier.offset {
                    if (predictiveProgress <= 0) return@offset IntOffset.Zero
                    val px = 30.dp.toPx() * predictiveProgress
                    IntOffset(px.roundToInt(), 0)
                }) {
                    // reset progress when the exit anim finishes and page removed from composition
                    DisposableEffect(Unit) { onDispose { predictiveProgress = -1f } }
                    DetailedAppInfo(cardColor, margin, shareIcon, shareApk)
                }
            }
            if (pageIndex == 1) {
                if (OsUtils.satisfy(OsUtils.U)) {
                    PredictiveBackHandler { events ->
                        events.onEach { event -> predictiveProgress = event.progress }
                            .catch { predictiveProgress = -1f }  // canceled
                            .onCompletion { e -> if (e == null) pageIndex = 0 }
                            .collect()
                    }
                } else {
                    BackHandler { pageIndex = 0 }
                }
            }
        }
    }
}

@Composable
private fun AppInfoWithHeader(
    itemBackColor: Color,
    cardColor: Color,
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val backGradient = remember {
        val backColor = if (isDarkTheme) Color.Black else Color(0xfffdfdfd)
        Brush.verticalGradient(
            0.0f to itemBackColor,
            0.1f to itemBackColor,
            0.5f to backColor,
            1.0f to backColor,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(brush = backGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints {
                val sizeToken = remember { maxWidth >= 360.dp }
                val (mTop, mBottom) = if (sizeToken) (30.dp to 18.dp) else (18.dp to 14.dp)
                AppHeaderContent(
                    cardColor,
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .padding(top = mTop, bottom = mBottom),
                )
            }
            Box(modifier = Modifier.weight(1f, fill = false)) {
                content()
            }
        }
    }
}

@Composable
private fun DetailedAppInfo(
    cardColor: Color,
    horizontalMargin: Dp,
    shareIcon: () -> Unit,
    shareApk: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.padding(horizontal = horizontalMargin),
            elevation = CardDefaults.elevatedCardElevation(),
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = 20.dp, smoothnessAsPercentTL = 100,
                cornerRadiusTR = 20.dp, smoothnessAsPercentTR = 100),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        ) {
            ExtendedAppInfo(LocalApp.current, shareIcon, shareApk)
        }
    }
}

@Composable
private fun FrontAppInfo(
    cardColor: Color,
    horizontalMargin: Dp,
    verInfoList: List<VerInfo>,
    updateTime: String?,
    clickDetails: () -> Unit,
    getClick: (AppTagInfo) -> (() -> Unit)?,
) {
    val app = LocalApp.current
    val context = LocalContext.current
    val splitApks = remember { AppInfo.getApkSizeList(app.appPackage, context) }
    Column {
        Card(
            modifier = Modifier.padding(horizontal = horizontalMargin),
            elevation = CardDefaults.elevatedCardElevation(),
            shape = AbsoluteSmoothCornerShape(20.dp, 100),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        ) {
            AppDetailsContent(app, verInfoList, splitApks[0].second, updateTime, clickDetails)
        }
        Spacer(modifier = Modifier.height(18.dp))
        Card(
            modifier = Modifier.padding(horizontal = horizontalMargin),
            elevation = CardDefaults.elevatedCardElevation(),
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = 20.dp, smoothnessAsPercentTL = 100,
                cornerRadiusTR = 20.dp, smoothnessAsPercentTR = 100),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        ) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                // ensure enough space for tab to scroll to
                val extraHeight = remember { max(maxHeight - 150.dp, 80.dp) }
                NestedScrollParent {
                    TagDetailsContent(getClick, splitApks, PaddingValues(bottom = extraHeight))
                }
            }
        }
    }
}

@Composable
fun NestedScrollContent(content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    AndroidView(
        factory = { context ->
            val composeView = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MaterialTheme(colorScheme = colorScheme, content = content)
                }
            }
            NestedScrollView(context).apply { addView(composeView) }
        },
    )
}

/**
 * Wrap [content] in a [CoordinatorLayout],
 * which is a [NestedScrollingParent3][androidx.core.view.NestedScrollingParent3]
 */
@Composable
fun NestedScrollParent(content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    AndroidView(
        factory = { context ->
            val composeView = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MaterialTheme(colorScheme = colorScheme, content = content)
                }
            }
            CoordinatorLayout(context).apply { addView(composeView) }
        },
    )
}

val LocalAppSwitcherHandler = staticCompositionLocalOf<AppSwitcherHandler> {
    error("Handler not provided")
}

@Composable
private fun AppHeaderContent(cardColor: Color, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val context = LocalContext.current
    val switcherHandler = LocalAppSwitcherHandler.current
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        AppSwitcher(modifier = Modifier.fillMaxWidth(), handler = switcherHandler)
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            val pkgInfo = remember { AppPackageInfo(context, app) }
            if (pkgInfo.uid > 0 || app.isArchive) {
                Image(
                    modifier = Modifier.size(40.dp),
                    painter = rememberAsyncImagePainter(pkgInfo),
                    contentDescription = null,
                )
            } else {
                Box(modifier = Modifier
                    .clip(CircleShape)
                    .size(40.dp)
                    .background(cardColor.copy(alpha = 0.8f)))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = app.name,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                lineHeight = 19.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val sealVerInfo = remember { VerInfo.targetDisplay(app) }
            val seal by sealFileOf(sealVerInfo.letterOrDev)
            if (seal != null) {
                Spacer(modifier = Modifier.width(6.dp))
                AsyncImage(
                    model = seal,
                    modifier = Modifier.size(40.dp),
                    contentDescription = null,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TagDetailsContent(
    getClick: (AppTagInfo) -> (() -> Unit)?,
    splitApks: List<Pair<String, String?>>,
    contentPadding: PaddingValues,
) {
    val app = LocalApp.current
    val context = LocalContext.current
    var lists: List<ExpressedTag>? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            AppInfo.expressTags(app, context) { lists = it }
        }
    }
    val list = lists
    if (list != null) {
        LibPage(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
            app = app,
            contentPadding = contentPadding,
        ) {
            item {
                Column {
                    TagDetailsList(app, list, getClick, splitApks)
                }
            }
        }
    } else {
        Box(Modifier)
    }
}

@Composable
private fun AppInfoPreview() {
    val list = remember {
        val t = AppTagManager.tags
        val tags = arrayOf(
            AppTagInfo.ID_APP_INSTALLER_PLAY,
            AppTagInfo.ID_APP_ADAPTIVE_ICON,
            AppTagInfo.ID_TECH_X_COMPOSE,
            AppTagInfo.ID_TECH_FLUTTER,
            AppTagInfo.ID_PKG_AAB,
        ).map { t.getValue(it) }
        val names = arrayOf(
            "Google Play", "Adaptive icon", "Jetpack Compose", "Flutter", "Android App Bundle")
        val infoList = names.map {
            AppTagInflater.TagInfo(name = it, icon = AppTagInflater.TagInfo.Icon(), rank = "0")
        }
        infoList.mapIndexed { i, info ->
            val label = infoList[i].name ?: "Unknown"
            val desc = "$label tag description $i"
            ExpressedTag(tags[i], label, info, desc, i > 1 && i % 2 == 0)
        }
    }
    val color = if (isSystemInDarkTheme()) Color(0xFF424942) else Color(0xffdefbde)
    WithAppPreview {
        val app = LocalApp.current
        val verInfo = remember {
            listOf(VerInfo.minDisplay(app), VerInfo.targetDisplay(app), VerInfo(app.compileAPI))
        }
        AppInfo(color, isSystemInDarkTheme(), verInfo,
            "6 days ago", shareIcon = { }, shareApk = { }, getClick = { null })
    }
}

@Composable
private fun WithAppPreview(content: @Composable () -> Unit) {
    val app = remember {
        ApiViewingApp("com.madness.collision").apply {
            name = "Boundo"
            verName = "8.4.6"
            minAPI = 10000
            targetAPI = 10000
            compileAPI = 10000
            appPackage = AppPackage(listOf("config.en.apk", "config.zh.apk", "config.es.apk"))
        }
    }
    CompositionLocalProvider(LocalApp provides app, content = content)
}

@Composable
private fun DetailedAppInfoPreview() {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val cardColor = remember { if (isDarkTheme) Color(0xff0c0c0c) else Color.White }
    WithAppPreview {
        val app = LocalApp.current
        val itemColor = remember { SealManager.getItemColorBack(context, app.targetAPI) }
        AppInfoWithHeader(Color(itemColor), cardColor, isDarkTheme) {
            DetailedAppInfo(cardColor, 30.dp, { }, { })
        }
    }
}

@StandardPreview
@Composable
private fun AppInfoPagePreview() {
    MaterialTheme {
        AppInfoPreview()
    }
}

@DarkPreview
@Composable
private fun AppInfoPageDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        AppInfoPreview()
    }
}

@StandardPreview
@Composable
private fun AppInfoPageDetailedPreview() {
    MaterialTheme {
        DetailedAppInfoPreview()
    }
}

@DarkPreview
@Composable
private fun AppInfoPageDetailedDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        DetailedAppInfoPreview()
    }
}

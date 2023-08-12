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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.SizeMode
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.util.dev.DarkPreview
import com.madness.collision.util.dev.LayoutDirectionPreviews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.pow

@Composable
fun AppIconPage(mainViewModel: MainViewModel, env: AppIconEnv) {
    val viewModel: AppIconViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val contentInsetTop by mainViewModel.contentWidthTop.observeAsState(0)
    val contentInsetBottom by mainViewModel.contentWidthBottom.observeAsState(0)
    CompositionLocalProvider(
        LocalContentInsets provides (contentInsetTop to contentInsetBottom)
    ) {
        val (appState, launcherState) = uiState
        val list = remember(appState, launcherState) full@{
            val appIcons = run app@{
                if (appState !is IconLoadingState.Result) return@app List(3) { null }
                (0..2).map { appState.value.getOrNull(it) }
            }
            if (launcherState !is IconLoadingState.Result) return@full appIcons
            appIcons + launcherState.value
        }
        if (list.isNotEmpty()) {
            AppIconSet(list, env)
        }
    }
}

private val LocalContentInsets = compositionLocalOf { 0 to 0 }
private val LocalContentMargin = compositionLocalOf { PaddingValues() }

class AppIconEnv(val iconExportPrefix: String)
private val LocalAppIconEnv = compositionLocalOf<AppIconEnv> { error("No env specified") }

@Composable
private fun Int.toDp() = with(LocalDensity.current) { toDp() }

@Composable
private fun AppIconSet(list: List<IconInfo?>, env: AppIconEnv) {
    val (insetTop, insetBottom) = LocalContentInsets.current
    CompositionLocalProvider(
        LocalContentMargin provides PaddingValues(horizontal = 20.dp),
        LocalAppIconEnv provides env,
    ) {
        // Twitter has nearly 30 adaptive icons, which is too much to handle for a Column
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = insetTop.toDp() + 20.dp, bottom = insetBottom.toDp() + 40.dp),
        ) {
            itemsIndexed(list, key = key@{ i, info ->
                val id = info?.resId ?: return@key i
                val indexDigits = i.toString().length
                val times = 10.toDouble().pow(indexDigits)
                id * times + i
            }) { index, iconInfo ->
                if (iconInfo != null) {
                    IconInfoSection(index, iconInfo)
                }
            }
        }
    }
}

@Composable
private fun IconInfoSection(i: Int, iconInfo: IconInfo) {
    val (_, icon, resName) = iconInfo
    if (i > 0) Spacer(modifier = Modifier.height(24.dp))
    Column(modifier = Modifier.padding(LocalContentMargin.current)) {
        FlowRow(
            mainAxisSize = SizeMode.Expand,
            crossAxisAlignment = FlowCrossAxisAlignment.Center,
        ) {
            val items = remember { iconInfo.entry.all }
            items.forEachIndexed { index, item ->
                val fullName = item.fullName
                if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                var popItemName by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .clip(AbsoluteSmoothCornerShape(3.dp, 80))
                        .let { if (fullName != null) it.clickable { popItemName = !popItemName } else it }
                ) {
                    Text(
                        item.name,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (fullName != null) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 2.dp).width(8.dp),
                        )
                        if (popItemName) {
                            val density = LocalDensity.current
                            val offset = remember { with(density) { 20.dp.roundToPx() } }
                            Popup(
                                offset = IntOffset(0, offset),
                                onDismissRequest = { popItemName = false },
                            ) {
                                PopupCard(popItemName) {
                                    Text(
                                        fullName,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 30.dp),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 14.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(AbsoluteSmoothCornerShape(3.dp, 80))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    val iconSource = when (item.source) {
                        IconInfo.Source.APK -> "APK"
                        IconInfo.Source.API -> stringResource(R.string.av_ic_info_src_api)
                    }
                    Text(
                        iconSource,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        fontSize = 7.sp,
                        lineHeight = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (resName != null) {
            Text(
                resName,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    when (icon) {
        is IconInfo.AdaptiveIcon -> AdaptiveIcon(iconInfo, icon)
        is IconInfo.NormalIcon -> NonAdaptiveIcon(iconInfo, icon)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PopupCard(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        BoxWithConstraints {
            val widthLimit = remember { maxWidth - 40.dp }
            Card(
                modifier = Modifier.widthIn(max = widthLimit),
                shape = AbsoluteSmoothCornerShape(20.dp, 80),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                content = content,
            )
        }
    }
}

@Composable
private fun NonAdaptiveIcon(iconInfo: IconInfo, normalIcon: IconInfo.NormalIcon) {
    val icon = normalIcon.drawable
    val env = LocalAppIconEnv.current
    val viewModel: AppIconViewModel = viewModel()
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(LocalContentMargin.current)
    ) {
        val exportName = env.iconExportPrefix
        IconImage(icon, 60.dp, onClick = click@{
            val iconId = iconInfo.resId ?: return@click
            val event = AppIconEvent.ShowIconRes(iconId)
            viewModel.triggerEvent(event)
        }, onLongClick = action@{
            val event = AppIconEvent.ShareIcon(icon, exportName)
            viewModel.triggerEvent(event)
        })
    }
}

@Composable
private fun AdaptiveIcon(iconInfo: IconInfo, icon: IconInfo.AdaptiveIcon) {
    val icons = remember(icon) { icon.run { listOf(background, foreground, monochrome) } }
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(LocalContentMargin.current)
    ) {
        Column {
            AiContent(icons)
            Spacer(modifier = Modifier.height(10.dp))
            AiPreview(iconInfo, icon)
        }
    }
}

@Composable
private fun AiContent(icons: List<Drawable?>) {
    val env = LocalAppIconEnv.current
    val context = LocalContext.current
    val (labels, exportNames) = remember {
        val labels = listOf(R.string.av_ic_info_ai_back, R.string.av_ic_info_ai_fore,
            R.string.av_ic_info_ai_mono).map { context.getString(it) }
        val names = listOf("Back", "Fore", "Mono").map { env.iconExportPrefix + "-$it" }
        labels to names
    }
    val validIcons = remember(icons) {
        icons.zip(labels) { ic, label -> if (ic != null) (ic to label) else null }.filterNotNull()
    }
    val viewModel: AppIconViewModel = viewModel()
    Row {
        validIcons.forEachIndexed { i, (icon, label) ->
            if (i > 0) Spacer(modifier = Modifier.width(20.dp))
            val exportName = exportNames[i]
            key(label, icon) {
                var ic by remember { mutableStateOf<Drawable?>(null) }
                LaunchedEffect(icon) {
                    delay(40L * (i + 1))
                    ic = icon
                }
                LabeledIcon(label, ic) {
                    val event = AppIconEvent.ShareIcon(icon, exportName)
                    viewModel.triggerEvent(event)
                }
            }
        }
    }
}

@Composable
private fun LabeledIcon(
    label: String,
    icon: Drawable?,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconImage(icon, 60.dp, onClick, onLongClick)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.widthIn(max = 110.dp)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconImage(
    icon: Drawable?,
    size: Dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val viewModel: AppIconViewModel = viewModel()
    val isInspected = LocalInspectionMode.current
    val isDark = if (!isInspected) viewModel.darkUiState.collectAsState().value else isSystemInDarkTheme()
    val borderColor = remember(isDark) { if (isDark) Color(0xFF293742) else Color(0xFF3DDC84) }
    val sealColor = remember(isDark) { if (isDark) Color(0xFF14191B) else Color(0xFFE2EBE6) }
    val backBrush = remember(isDark) {
        val c = if (isDark) listOf(Color(0xFF11293C), Color(0xFF0C1822), Color(0xFF000000))
        else listOf(Color(0xFFC3EED6), Color(0xFFDDF3E7), Color(0xFFFFFFFF))
        Brush.verticalGradient(0f to c[0], 0.15f to c[1], 1f to c[2])
    }
    Card(
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, borderColor),
    ) {
        Box(
            modifier = Modifier
                .combinedClickable(onLongClick = onLongClick, onClick = onClick ?: {})
                .background(backBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Android,
                contentDescription = null,
                tint = sealColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .widthIn(min = size, max = size + 50.dp)
                    .heightIn(min = size, max = size + 12.dp)
                    .offset(x = (5).dp, y = 28.dp),
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 25.dp, vertical = 6.dp)
                    .size(size),
                contentAlignment = Alignment.Center,
            ) {
                if (LocalInspectionMode.current) {
                    Box(modifier = Modifier
                        .background(Color.Gray)
                        .size(size))
                } else if (icon != null) {
                    AsyncImage(
                        model = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .width(size)
                            .heightIn(max = size),
                    )
                }
            }
//            Icon(
//                imageVector = Icons.Outlined.Share,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
//                modifier = Modifier
//                    .align(Alignment.BottomEnd)
//                    .padding(end = 8.dp, bottom = 7.dp)
//                    .width(8.dp),
//            )
        }
    }
}

private fun Bitmap.toDrawable(context: Context): BitmapDrawable {
    return toDrawable(context.resources).apply { setBounds(0, 0, width, height) }
}

@Composable
private fun AiPreview(iconInfo: IconInfo, icon: IconInfo.AdaptiveIcon) {
    val context = LocalContext.current
    // serve as pipeline
    val previewDeque = remember { ArrayDeque<Pair<Int, Drawable?>>(4) }
    // serve as a notifier to launch job to process new items from pipeline
    var previewNotifier by remember { mutableStateOf(-1) }
    val notifierMutex = remember { Mutex() }
    LaunchedEffect(iconInfo) {
        val ic = icon.previews
        withContext(Dispatchers.IO) {
            // access sequentially to avoid bitmap not correctly loaded issue
            listOf(ic.bitmap, ic.rounded, ic.squircle, ic.round)
                .forEachIndexed { index, deferred ->
                    val item = index to deferred.toDrawable(context)
                    notifierMutex.withLock {
                        previewDeque.addLast(item)
                        previewNotifier++
                    }
                }
        }
    }
    var previews: List<Drawable?> by remember { mutableStateOf(List(4) { null }) }
    val previewItems = remember { arrayOfNulls<Drawable?>(4) }
    var lastPreviewChange = remember { -1L }
    val lastPreviewMutex = remember { Mutex() }
    LaunchedEffect(previewNotifier) {
        // Notifier will be updated too frequently so that the previous change was discarded already,
        // while the current change contains multiple items. So loop to get all items.
        // skip loop if previewNotifier == -1
        while (previewNotifier >= 0 && isActive) {
            // break when the queue is empty
            val (index, item) = notifierMutex.withLock { previewDeque.removeFirstOrNull() } ?: break
            // skip/continue when item is the same as updated
            if (previewItems[index] === item) continue
            val time = SystemClock.uptimeMillis()
            lastPreviewMutex.withLock {
                // delay 50ms for every item sequentially
                if (lastPreviewChange < 0 || time - lastPreviewChange < 50) delay(50)
                lastPreviewChange = time
                previewItems[index] = item
                previews = previewItems.toList()
            }
        }
    }
    val env = LocalAppIconEnv.current
    val (exportNames, labels) = remember {
        val names = listOf("Full", "Rounded", "Squircle", "Round")
            .map { env.iconExportPrefix + "-$it" }
        val labels = listOf(
            R.string.av_ic_info_preview_full, R.string.av_ic_info_preview_rect,
            R.string.av_ic_info_preview_squircle, R.string.av_ic_info_preview_round,
        ).map { context.getString(it) }
        names to labels
    }
    val viewModel: AppIconViewModel = viewModel()
    Row {
        previews.forEachIndexed { index, preview ->
            key(index, preview) {
                if (index > 0) Spacer(modifier = Modifier.width(20.dp))
                val exportName = exportNames[index]
                LabeledIcon(labels[index], preview, onClick = click@{
                    // show icon res only for the full preview
                    if (index != 0) return@click
                    val iconId = iconInfo.resId ?: return@click
                    val event = AppIconEvent.ShowIconRes(iconId)
                    viewModel.triggerEvent(event)
                }, onLongClick = action@{
                    preview ?: return@action
                    val event = AppIconEvent.ShareIcon(preview, exportName)
                    viewModel.triggerEvent(event)
                })
            }
        }
    }
}

@Composable
private fun AppIconSetPreview() {
    val env = AppIconEnv("")
    val list = List(3) { index ->
        val item = IconInfo.Item("Icon $index", null, IconInfo.Source.API)
        IconInfo(null, ColorDrawable(0xffffffff.toInt()), null, IconInfo.MonoEntry(item))
    }
    AppIconSet(list, env)
}

@LayoutDirectionPreviews
@Composable
private fun PagePreview() = MaterialTheme { AppIconSetPreview() }

@DarkPreview
@Composable
private fun PageDarkPreview() = MaterialTheme(colorScheme = darkColorScheme()) { AppIconSetPreview() }

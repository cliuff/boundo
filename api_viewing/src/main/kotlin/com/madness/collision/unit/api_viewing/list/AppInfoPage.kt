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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import coil.compose.rememberAsyncImagePainter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackage
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.R as AvR
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.get
import com.madness.collision.unit.api_viewing.tag.app.toExpressible
import com.madness.collision.unit.api_viewing.tag.inflater.AppTagInflater
import com.madness.collision.util.configure
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.*
import kotlinx.coroutines.*
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.name

class AppInfoFragment(private val appPkgName: String) : BottomSheetDialogFragment(), SystemBarMaintainerOwner {
    private val mainViewModel: MainViewModel by activityViewModels()
    private var infoApp: ApiViewingApp? = null
    private var _composeView: ComposeView? = null
    private val composeView: ComposeView get() = _composeView!!
    override val systemBarMaintainer: SystemBarMaintainer = DialogFragmentSystemBarMaintainer(this)

    companion object {
        const val TAG = "AppInfoFragment"
    }

    constructor(app: ApiViewingApp) : this(app.packageName) {
        infoApp = app
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BottomSheetDialog style, set enableEdgeToEdge to true
        // (and navigationBarColor set to transparent or translucent)
        // to disable automatic insets handling
        setStyle(STYLE_NORMAL, R.style.AppTheme_Pop)
    }

    override fun onStart() {
        super.onStart()
        val context = context ?: return
        val rootView = view ?: return
        BottomSheetBehavior.from(rootView.parent as View).configure(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _composeView = ComposeView(inflater.context)
        return composeView
    }

    override fun onDestroyView() {
        _composeView = null
        super.onDestroyView()
    }

    private fun setBackgroundColor(color: Int) {
        val sheetDialog = dialog as? BottomSheetDialog? ?: return
        val klass = BottomSheetBehavior::class.java
        try {
            val method = klass.getDeclaredMethod("getMaterialShapeDrawable")
            method.isAccessible = true
            val shape = method.invoke(sheetDialog.behavior) as? MaterialShapeDrawable?
            shape?.setTint(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        view.setOnApplyWindowInsetsListener { _, insets ->
            if (checkInsets(insets)) edgeToEdge(insets, false)
            WindowInsetsCompat.CONSUMED.toWindowInsets()!!
        }
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val colorScheme = if (OsUtils.satisfy(OsUtils.S)) {
            if (mainApplication.isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        composeView.setContent {
            AppInfoPageContent(colorScheme)
        }
    }

    @Composable
    private fun AppInfoPageContent(colorScheme: ColorScheme) {
        var app: ApiViewingApp? by remember { mutableStateOf(infoApp) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        if (app == null) {
            val lifecycleOwner = this
            SideEffect {
                scope.launch(Dispatchers.Default) {
                    app = DataMaintainer.get(context, lifecycleOwner).selectApp(appPkgName)
                }
            }
        }
        MaterialTheme(colorScheme = colorScheme) {
            app?.let { a ->
                SideEffect {
                    val color = SealManager.getItemColorBack(context, a.targetAPI)
                    setBackgroundColor(color)
                }
                // use parent instead of child fragment manager to show dialog after dismiss()
                val fMan = remember { parentFragmentManager }
                // provide dialog's onBackPressedDispatcher to implement custom back handler
                val providedBackDispatcher = remember dis@{
                    val dia = dialog ?: return@dis null
                    if (dia !is ComponentDialog) return@dis null
                    LocalOnBackPressedDispatcherOwner provides dia
                }
                val providedValues = arrayOf(providedBackDispatcher).filterNotNull().toTypedArray()
                CompositionLocalProvider(LocalApp provides a, *providedValues) {
                    AppInfoPage(mainViewModel, this, {
                        dismiss()
                        AppListService().actionIcon(context, a, fMan)
                    }, {
                        dismiss()
                        // calling dismiss() cancels composable scope
                        val apkScope = CoroutineScope(Job())
                        AppListService().actionApk(context, a, apkScope, fMan)
                    })
                }
            }
        }
    }
}

// app is unlikely to change
private val LocalApp = staticCompositionLocalOf<ApiViewingApp> { error("App not provided") }

private fun getTagViewInfo(tag: AppTagInfo, res: AppTagInfo.Resources, context: Context) = run m@{
    if (tag.requisites?.all { it.checker(res) } == false) return@m null
    val express = tag.toExpressible().setRes(res).express()
    val viewInfo = AppTag.getTagViewInfo(tag, res, context) {
        it.label.run {
            normal ?: return@run null
            if (express) (full ?: normal) else normal
        }
    }
    viewInfo ?: return@m null
    val desc = run {
        val checkDesc = tag.desc?.checkResultDesc ?: return@run null
        checkDesc(res).get(context)?.toString()
    }
    (tag to viewInfo) to (express to desc)
}

private fun getExpressedTags(tags: List<AppTagInfo>, res: AppTagInfo.Resources, context: Context)
        : List<ExpressedTag> {
    val rawList = tags.mapNotNull { getTagViewInfo(it, res, context) }
    val (list, expressList) = rawList.unzip()
    // g-play is subset of package installer
    val playIndex = list.indexOfFirst { it.first.id == AppTagInfo.ID_APP_INSTALLER_PLAY }
    val removeIndex = if (expressList.getOrNull(playIndex)?.first != true) playIndex
    else list.indexOfFirst { it.first.id == AppTagInfo.ID_APP_INSTALLER }
    val infoList = list.filterIndexed { i, _ -> i != removeIndex }
    val ex = expressList.filterIndexed { i, _ -> i != removeIndex }
    val listService = AppListService()
    val pkgRegex = """[\w.]+""".toRegex()
    return infoList.zip(ex) { tagInfo, express ->
        val info = tagInfo.second
        val activated = express.first
        val label = run {
            if (activated && info.name?.matches(pkgRegex) == true) {
                return@run listService.getInstallerName(context, info.name)
            }
            info.name ?: info.nameResId?.let { context.getString(it) } ?: "Unknown"
        }
        ExpressedTag(tagInfo.first, label, tagInfo.second, express.second, express.first)
    }
}

@Composable
fun AppInfoPage(
    mainViewModel: MainViewModel,
    hostFragment: DialogFragment,
    shareIcon: () -> Unit,
    shareApk: () -> Unit,
) {
    val app = LocalApp.current
    val context = LocalContext.current
    val verInfo = remember { listOf(VerInfo.minDisplay(app), VerInfo.targetDisplay(app), VerInfo(app.compileAPI)) }
    val itemColor = remember { SealManager.getItemColorBack(context, app.targetAPI) }
    val itemWidth = with(LocalDensity.current) { 45.dp.roundToPx() }
    val bitmaps = remember {
        verInfo.map { SealManager.disposeSealBack(context, it.letter, itemWidth) }
    }
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
                    val intent = app.storePage(ApiViewingApp.packagePlayStore, direct = true)
                    safely { context.startActivity(intent) } ?: Unit
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
    AppInfo(Color(itemColor), isDark, verInfo, bitmaps, updateTime, shareIcon, shareApk, getClick)
}

@Composable
private fun AppInfo(
    itemBackColor: Color,
    isDarkTheme: Boolean,
    verInfoList: List<VerInfo>,
    bitmaps: List<Bitmap?>,
    updateTime: String?,
    shareIcon: () -> Unit,
    shareApk: () -> Unit,
    getClick: (AppTagInfo) -> (() -> Unit)?,
) {
    val cardColor = remember { if (isDarkTheme) Color(0xff0c0c0c) else Color.White }
    AppInfoWithHeader(itemBackColor, cardColor, isDarkTheme) {
        BoxWithConstraints {
            val margin = remember {
                if (maxWidth > 600.dp) 30.dp else if (maxWidth > 360.dp) 24.dp else 12.dp
            }
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            // use coefficient to invert slide animation offset for RTL layout
            val offsetCoeff = if (isRtl) -1 else 1
            var pageIndex by remember { mutableStateOf(0) }
            AnimatedVisibility(
                visible = pageIndex == 0,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { offsetCoeff * -it / 2 }),
                exit = fadeOut(),
            ) {
                val scope = rememberCoroutineScope()
                FrontAppInfo(cardColor, margin, verInfoList, bitmaps, updateTime,
                    { scope.launch { pageIndex = 1 } }, getClick)
            }
            AnimatedVisibility(
                visible = pageIndex == 1,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { offsetCoeff * it / 2 }),
                exit = fadeOut(),
            ) {
                DetailedAppInfo(cardColor, margin, shareIcon, shareApk)
            }
            if (pageIndex == 1) {
                BackHandler { pageIndex = 0 }
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
        Column(modifier = Modifier) {
            BoxWithConstraints {
                val sizeToken = remember { maxWidth > 360.dp }
                val margin = if (sizeToken) 40.dp else 30.dp
                val (mTop, mBottom) = if (sizeToken) (30.dp to 18.dp) else (18.dp to 14.dp)
                AppHeaderContent(
                    cardColor,
                    modifier = Modifier
                        .padding(horizontal = margin)
                        .padding(top = mTop, bottom = mBottom),
                )
            }
            content()
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

private fun getApkSizeList(pkg: AppPackage, context: Context): List<Pair<String, String?>> {
    val baseName = if (OsUtils.satisfy(OsUtils.O)) Path(pkg.basePath).name else File(pkg.basePath).name
    val parentPath = pkg.basePath.replaceFirst(baseName, "")
    val sizes = pkg.apkPaths.map { path ->
        val fileName = path.replaceFirst(parentPath, "")
        val file = File(path).takeIf { it.exists() } ?: return@map fileName to null
        fileName to file.length()
    }
    val totalBytes = sizes.sumOf { it.second ?: 0 }.takeIf { it > 0 }
    val totalSize = totalBytes?.let { Formatter.formatFileSize(context, it) }
    val itemSizeList = sizes.map apkSize@{ (name, bytes) ->
        bytes ?: return@apkSize name to null
        name to Formatter.formatFileSize(context, bytes)
    }
    return buildList(itemSizeList.size + 1) {
        add("" to totalSize)
        addAll(itemSizeList)
    }
}

@Composable
private fun FrontAppInfo(
    cardColor: Color,
    horizontalMargin: Dp,
    verInfoList: List<VerInfo>,
    bitmaps: List<Bitmap?>,
    updateTime: String?,
    clickDetails: () -> Unit,
    getClick: (AppTagInfo) -> (() -> Unit)?,
) {
    val app = LocalApp.current
    val context = LocalContext.current
    val splitApks = remember { getApkSizeList(app.appPackage, context) }
    Column {
        Card(
            modifier = Modifier.padding(horizontal = horizontalMargin),
            elevation = CardDefaults.elevatedCardElevation(),
            shape = AbsoluteSmoothCornerShape(20.dp, 100),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        ) {
            AppDetailsContent(verInfoList, bitmaps, splitApks[0].second, updateTime, clickDetails)
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
                val extraHeight = remember { maxHeight / 2 }
                NestedScrollContent {
                    Column {
                        TagDetailsContent(getClick, splitApks)
                        Spacer(Modifier.height(extraHeight))
                    }
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

@Composable
private fun AppHeaderContent(cardColor: Color, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val seal = remember { SealManager.seals[app.targetSDKLetter] }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val context = LocalContext.current
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
            if (seal != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Image(
                    modifier = Modifier.size(40.dp),
                    bitmap = remember { seal.asImageBitmap() },
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun AppDetailsContent(
    verInfoList: List<VerInfo>,
    bitmaps: List<Bitmap?>,
    totalSize: String?,
    updateTime: String?,
    clickDetails: () -> Unit,
) {
    Column(modifier = Modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 9.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sdkTitles = remember {
                arrayOf(
                    AvR.string.av_list_info_min_sdk,
                    R.string.apiSdkTarget,
                    AvR.string.av_list_info_compile_sdk,
                )
            }
            val context = LocalContext.current
            val isInspected = LocalInspectionMode.current
            val apis = remember {
                verInfoList.zip(sdkTitles) { ver, titleId ->
                    if (ver.api < OsUtils.A) return@zip null
                    val color = SealManager.getItemColorText(ver.api)
                    val title = context.getString(titleId)
                    val displayApi = if (isInspected) ver.api.toString() else ver.apiText
                    Triple("$title $displayApi", ver.displaySdk, Color(color))
                }.filterNotNull()
            }
            val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            apis.forEachIndexed { index, (label, sdk, color) ->
                if (index > 0) {
                    Divider(modifier = Modifier.size(width = 0.5.dp, height = 24.dp), color = dividerColor)
                }
                AppSdkItem(
                    modifier = Modifier.weight(1f),
                    label = label,
                    ver = sdk,
                    color = color,
                    bitmap = bitmaps[index]
                )
            }
        }
        Divider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            thickness = 0.5.dp,
        )
        Row(
            modifier = Modifier
                .clickable(onClick = clickDetails)
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val app = LocalApp.current
                if (updateTime != null) {
                    AppDetailsItem1(app.verName, totalSize, updateTime, Icons.Outlined.Info)
                } else {
                    val verSize = if (totalSize != null) "${app.verName} • $totalSize" else app.verName
                    AppDetailsItem(verSize, Icons.Outlined.Info)
                }
                AppDetailsItem(app.packageName, Icons.Outlined.Inventory2)
            }
            val direction = LocalLayoutDirection.current
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = if (direction == LayoutDirection.Rtl) Icons.Outlined.ChevronLeft else Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun AppSdkItem(modifier: Modifier = Modifier, label: String, ver: String, color: Color, bitmap: Bitmap?) {
    Column(
        modifier = modifier.padding(horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(30.dp),
                    bitmap = remember { bitmap.asImageBitmap() },
                    contentDescription = null,
                )
            } else {
                val c = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                Box(modifier = Modifier
                    .clip(CircleShape)
                    .size(30.dp)
                    .background(c))
            }
            Text(
                text = ver,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppDetailsItem(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppDetailsItem1(label: String, label0: String?, label1: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (label0 != null) {
            Text(
                text = " • ",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label0,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label1,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private class ExpressedTag(
    val intrinsic: AppTagInfo,
    val label: String,
    val info: AppTagInflater.TagInfo,
    val desc: String?,
    val activated: Boolean,
)

@Composable
private fun TagDetailsContent(
    getClick: (AppTagInfo) -> (() -> Unit)?,
    splitApks: List<Pair<String, String?>>,
) {
    val app = LocalApp.current
    val context = LocalContext.current
    var lists: List<ExpressedTag>? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        val allTags = AppTagManager.tags.values
        val emptyRes = AppTagInfo.Resources(context, app)
        // init tags and icons that do not have requisites or are satisfied first
        val selTags = allTags.filter { i ->
            i.requisites ?: return@filter true
            i.requisites.all { it.checker(emptyRes) }
        }
        val selTagIdSet = selTags.mapTo(HashSet(selTags.size)) { it.id }
        AppTag.ensureTagIcons(context) { it.id in selTagIdSet }
        lists = getExpressedTags(selTags, emptyRes, context).sortedBy { it.intrinsic.rank }

        val tags = AppTagManager.tags
        AppTag.ensureAllTagIcons(context)
        AppTag.ensureRequisitesForAllAsync(context, app) { _, ids, res ->
            val reqTags = ids.mapNotNull { id -> tags[id]?.takeIf { it.id !in selTagIdSet } }
            val l = getExpressedTags(reqTags, res, context) + lists.orEmpty()
            lists = l.sortedBy { it.intrinsic.rank }
        }
    }
    val list = lists
    if (list != null) {
        Column {
            TagDetailsContent(list, getClick, splitApks)
        }
    } else {
        Box(Modifier)
    }
}

@Composable
private fun TagDetailsContent(
    tags: List<ExpressedTag>,
    getClick: (AppTagInfo) -> (() -> Unit)?,
    splitApks: List<Pair<String, String?>>,
) {
    for (index in tags.indices) {
        val expressed = tags[index]
        val key = run {
            if (index == 0) return@run expressed.intrinsic.id
            expressed.intrinsic.id + tags[index - 1].intrinsic.id
        }
        key(key) {
            val info = expressed.info
            val activated = expressed.activated
            val isAi = expressed.intrinsic.id == AppTagInfo.ID_APP_ADAPTIVE_ICON
            val showDivider = remember show@{
                if (index == 0) return@show false
                if (activated || isAi) return@show true
                val lastTag = tags[index - 1]
                lastTag.activated || lastTag.intrinsic.id == AppTagInfo.ID_APP_ADAPTIVE_ICON
            }
            if (showDivider) Divider(
                modifier = Modifier.padding(start = 20.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                thickness = 0.5.dp,
            )
            if (activated || isAi) {
                var showAabDetails by remember { mutableStateOf(false) }
                val isAab = expressed.intrinsic.id == AppTagInfo.ID_PKG_AAB
                val direction = LocalLayoutDirection.current
                val chevron by remember(showAabDetails) {
                    derivedStateOf {
                        when (expressed.intrinsic.id) {
                            AppTagInfo.ID_APP_INSTALLER_PLAY -> Icons.Outlined.Launch
                            AppTagInfo.ID_APP_ADAPTIVE_ICON -> {
                                if (direction == LayoutDirection.Rtl) Icons.Outlined.ChevronLeft
                                else Icons.Outlined.ChevronRight
                            }
                            AppTagInfo.ID_PKG_AAB -> {
                                if (showAabDetails) Icons.Outlined.ExpandLess
                                else Icons.Outlined.ExpandMore
                            }
                            else -> null
                        }
                    }
                }
                val itemColor = MaterialTheme.colorScheme.onSurface
                    .copy(alpha = if (activated) 0.75f else 0.35f)
                val onClick = run {
                    if (!isAab) return@run expressed.intrinsic.let(getClick);
                    { showAabDetails = !showAabDetails }
                }
                val shrinkTop = index > 0 && !showDivider
                TagItem(
                    title = expressed.label,
                    desc = expressed.desc,
                    icon = info.icon?.bitmap,
                    chevron = chevron,
                    itemColor = itemColor,
                    isIconActivated = activated,
                    onClick = onClick,
                    modifier = Modifier
                        .let { if (index == 0) it.padding(top = 8.dp) else it }
                        .let { if (index == tags.lastIndex) it.padding(bottom = 8.dp) else it }
                        .padding(top = if (shrinkTop) 0.dp else 12.dp, bottom = 12.dp),
                )
                if (isAab) {
                    AnimatedVisibility(
                        visible = showAabDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        AppBundleDetails(splitApks)
                    }
                }
            } else {
                val shrinkTop = index > 0 && !showDivider
                TagItemDeactivated(
                    title = expressed.label,
                    desc = expressed.desc,
                    icon = info.icon?.bitmap,
                    modifier = Modifier
                        .let { if (index == 0) it.padding(top = 8.dp) else it }
                        .let { if (index == tags.lastIndex) it.padding(bottom = 8.dp) else it }
                        .padding(top = if (shrinkTop) 0.dp else 6.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun TagItem(
    title: String,
    desc: String?,
    icon: Bitmap?,
    chevron: ImageVector?,
    itemColor: Color,
    isIconActivated: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagItemIcon(icon = icon, activated = isIconActivated)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = itemColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp,
                )
            }
            if (desc != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    color = itemColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 11.sp,
                )
            }
        }
        if (chevron != null) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = chevron,
                contentDescription = null,
                tint = itemColor,
            )
        }
    }
}

@Composable
private fun TagItemDeactivated(
    title: String, desc: String?, icon: Bitmap?, modifier: Modifier = Modifier) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).then(modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagItemIcon(icon = icon, activated = false)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            if (desc != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = desc,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TagItemIcon(icon: Bitmap?, activated: Boolean) {
    if (icon != null) {
        val colorFilter = remember {
            if (activated) return@remember null
            val matrix = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(matrix)
        }
        Image(
            modifier = Modifier.size(16.dp),
            bitmap = remember { icon.asImageBitmap() },
            contentDescription = null,
            colorFilter = colorFilter,
        )
    } else {
        val colorScheme = MaterialTheme.colorScheme
        val tint = remember { colorScheme.onSurface.copy(alpha = if (activated) 0.75f else 0.4f) }
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.Label,
            contentDescription = null,
            tint = tint,
        )
    }
}

@Composable
private fun AppBundleDetails(list: List<Pair<String, String?>>) {
    Column(modifier = Modifier
        .padding(horizontal = 20.dp)
        .padding(bottom = 12.dp)) {
        val totalSize = list[0].second
        Text(
            text = stringResource(AvR.string.av_list_info_aab_total_size, totalSize ?: "0"),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        list.subList(1, list.size).forEach {
            AppBundleApkItem(it.first, it.second)
        }
    }
}

@Composable
private fun AppBundleApkItem(label: String, label1: String?) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (label1 != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
            AppTagInflater.TagInfo(name = it, icon = AppTagInflater.TagInfo.Icon(), rank = 0)
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
        AppInfo(color, isSystemInDarkTheme(), verInfo, List(3) { null },
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
            minSDKDisplay = "6"
            targetSDKDisplay = "12"
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

@Preview(showBackground = true)
@Composable
private fun AppInfoPagePreview() {
    MaterialTheme {
        AppInfoPreview()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppInfoPageDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        AppInfoPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun AppInfoPageDetailedPreview() {
    MaterialTheme {
        DetailedAppInfoPreview()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppInfoPageDetailedDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        DetailedAppInfoPreview()
    }
}

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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.info.AppInfo
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.X
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.AppIconPackageInfo

@Composable
fun ExtendedAppInfo(app: ApiViewingApp, shareIcon: () -> Unit, shareApk: () -> Unit) {
    CompositionLocalProvider(LocalApp provides app) {
        ExtendedAppInfo(shareIcon, shareApk)
    }
}

// app is unlikely to change
private val LocalApp = staticCompositionLocalOf<ApiViewingApp> { error("App not provided") }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ExtendedAppInfo(shareIcon: () -> Unit, shareApk: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        ExternalActions()
        if (app.isLaunchable) {
            InfoDivider()
            val launchIntent = remember { context.packageManager.getLaunchIntentForPackage(app.packageName) }
            val activityName = remember { launchIntent?.component?.className ?: "" }
            ComplexActionItem(
                stringResource(R.string.fileActionsOpen),
                Icons.Outlined.Launch,
                activityName,
                onClick = c@{ launchIntent?.let { context.startActivity(it) } },
                onLongClick = { X.copyText2Clipboard(context, activityName, R.string.text_copy_content) },
            )
        }
        InfoDivider()
        ActionItem(stringResource(R.string.avAdapterActionsIcon), Icons.Outlined.Landscape, onClick = shareIcon)
        InfoDivider()
        ActionItem(stringResource(R.string.avAdapterActionsApk), Icons.Outlined.Android, onClick = shareApk)
        InfoDivider()
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val extraHeight = remember { maxHeight / 2 }
            NestedScrollParent {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .nestedScroll(rememberNestedScrollInteropConnection()),
                ) {
                    AppDetails()
                    Spacer(Modifier.height(extraHeight))
                }
            }
        }
    }
}

@Composable
private fun InfoDivider() {
    Divider(
        modifier = Modifier.padding(start = 20.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        thickness = 0.5.dp,
    )
}

private var pkgDefaultSettings: String? = null
private val packageSettings: String
    get() = pkgDefaultSettings ?: packageAndroidSettings

private const val packageAndroidSettings = "com.android.settings"
private const val packageAppManager = "io.github.muntashirakon.AppManager"
private const val infoAppManager = "io.github.muntashirakon.AppManager.details.AppDetailsActivity"

@Composable
private fun ExternalActions() {
    val context = LocalContext.current
    pkgDefaultSettings = remember { pkgDefaultSettings ?: AppInfo.getDefaultSettings(context) }
    val appInfoOwners = remember { AppInfo.getAppInfoOwners(context) }
    val storePkgNames = remember {
        val extras = arrayOf(ApiViewingApp.packagePlayStore, ApiViewingApp.packageCoolApk)
        buildSet(extras.size + appInfoOwners.size + 2) {
            addAll(extras)
            addAll(appInfoOwners.keys)
            add(packageAppManager)
            add(packageSettings)
            remove(BuildConfig.APPLICATION_ID)
        }.toList()
    }
    val packs = remember {
        storePkgNames.map {
            val msg = "av.info.x" to "External store not found: $it"
            val info = MiscApp.getPackageInfo(context, packageName = it, errorMsg = msg) ?: return@map null
            AppIconPackageInfo(info)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp, horizontal = 20.dp)
    ) {
        val app = LocalApp.current
        val isInspected = LocalInspectionMode.current
        val activeStores = remember {
            storePkgNames.mapIndexedNotNull m@{ i, pkgName ->
                val p = packs[i]
                if (p == null && !isInspected) return@m null
                if (pkgName == packageSettings) {
                    if (app.isArchive && OsUtils.satisfy(OsUtils.Q)) return@m null
                }
                pkgName to p
            }
        }
        for (i in activeStores.indices) {
            if (i > 0) Spacer(modifier = Modifier.width(10.dp))
            val (pkgName, p) = activeStores[i]
            ExternalActionItem(p) {
                val owner = pkgName to appInfoOwners[pkgName]
                launchOwner(pkgName in appInfoOwners, owner, app, context)
            }
        }
    }
}

private fun launchOwner(isStandard: Boolean, owner: Pair<String, String?>, app: ApiViewingApp, context: Context) {
    val (pkgName, ownerActivity) = owner
    when {
        isStandard -> {
            val intent = Intent(AppInfo.actionShowAppInfo)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(AppInfo.extraPackageName, app.packageName)
            when (ownerActivity) {
                null -> intent.setPackage(pkgName)
                else -> intent.component = ComponentName(pkgName, ownerActivity)
            }
            safely { context.startActivity(intent) }
        }
        pkgName == packageSettings -> {
            if (app.isNotArchive) {
                safely { context.startActivity(app.settingsPage()) }
                return
            }
            safely { context.startActivity(app.apkPage()) }
                ?: CollisionDialog.infoCopyable(context, app.appPackage.basePath).show()
        }
        pkgName == packageAppManager -> {
            val intent = Intent()
                .setClassName(packageAppManager, infoAppManager)
                .putExtra("pkg", app.packageName)
            safely { context.startActivity(intent) }
        }
        else -> {
            safely { context.startActivity(app.storePage(pkgName, direct = true)) }
                ?: safely { context.startActivity(app.storePage(pkgName, direct = false)) }
        }
    }
}

inline fun safely(block: () -> Unit): Unit? {
    try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
    return Unit
}

@Composable
private fun AppDetails() {
    val app = LocalApp.current
    val context = LocalContext.current
    val service = remember { AppListService(context) }
    val pkgInfo = remember { service.getRetrievedPkgInfo(context, app) }
    var detailsContent by remember { mutableStateOf(AnnotatedString("")) }
    if (pkgInfo != null) {
        LaunchedEffect(app) {
            detailsContent = service.getAppInfoDetailsSequence(context, app, pkgInfo).annotated()
        }
    }
    var showExtended by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .let { if (showExtended) it else it.clickable { showExtended = true } }
            .padding(vertical = 12.dp, horizontal = 20.dp)
    ) {
        Text(
            text = detailsContent,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 9.sp,
            lineHeight = 12.sp,
        )
        if (showExtended) {
            var extendedContent by remember { mutableStateOf(AnnotatedString("")) }
            if (pkgInfo != null) {
                LaunchedEffect(app) {
                    extendedContent = service.getAppExtendedDetailsSequence(context, app, pkgInfo).annotated()
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = extendedContent,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 9.sp,
                lineHeight = 12.sp,
            )
        }
        if (!showExtended) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}

private fun Sequence<AppListService.AppInfoItem>.annotated() = buildAnnotatedString {
    val style = SpanStyle(fontWeight = FontWeight.SemiBold)
    forEach { item ->
        when (item) {
            is AppListService.AppInfoItem.Normal -> append(item.text)
            is AppListService.AppInfoItem.Bold ->
                withStyle(style) { append(item.text) }
        }
    }
}

@Composable
private fun ExternalActionItem(packInfo: AppIconPackageInfo?, onClick: () -> Unit) {
    if (packInfo != null) {
        Image(
            modifier = Modifier
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                )
                .size(40.dp),
            painter = rememberAsyncImagePainter(packInfo),
            contentDescription = null,
        )
    } else {
        val c = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        Box(modifier = Modifier
            .clip(CircleShape)
            .size(40.dp)
            .background(c))
    }
}

@Composable
private fun ActionItem(label: String, imageVector: ImageVector, onClick: () -> Unit) {
    RawActionItem(
        label = label,
        imageVector = imageVector,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 20.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComplexActionItem(label: String, imageVector: ImageVector, desc: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp, horizontal = 20.dp),
    ) {
        RawActionItem(label = label, imageVector = imageVector)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                modifier = Modifier.size(10.dp),
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun RawActionItem(label: String, imageVector: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

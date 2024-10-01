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

package com.madness.collision.unit.api_viewing.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madness.collision.R as MainR
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.regexOf
import com.madness.collision.util.ui.orWithRtl
import java.io.File

@Composable
internal fun AppDetailsContent(
    app: ApiViewingApp,
    verInfoList: List<VerInfo>,
    totalSize: String?,
    updateTime: String?,
    clickDetails: () -> Unit,
) {
    CompositionLocalProvider(LocalApp provides app) {
        AppDetails(verInfoList, totalSize, updateTime, clickDetails)
    }
}

// app is unlikely to change
private val LocalApp = staticCompositionLocalOf<ApiViewingApp> { error("App not provided") }

@Composable
private fun AppDetails(
    verInfoList: List<VerInfo>,
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
            val apis = remember(verInfoList) {
                verInfoList.mapIndexedNotNull { i, v ->
                    if (v.api >= OsUtils.A) i to v else null
                }
            }
            apis.forEachIndexed { i, (vi, ver) ->
                if (i > 0) {
                    Divider(
                        modifier = Modifier.size(width = 0.5.dp, height = 24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                }
                AppSdkItem(
                    modifier = Modifier.weight(1f),
                    ver = ver,
                    title = when (vi) {
                        0 -> stringResource(R.string.av_list_info_min_sdk)
                        1 -> stringResource(MainR.string.apiSdkTarget)
                        2 -> stringResource(R.string.av_list_info_compile_sdk)
                        else -> "SDK"
                    }
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
                // version name in RTL layout, CORRECT: 5.0-A01, WRONG: A01-5.0
                val appVerName = app.verName.let { v ->
                    val verRegex = regexOf("""[0-9a-zA-Z-_+.()\[\]\s]+""")
                    v.orWithRtl { if (v.matches(verRegex.toRegex())) "\u2068$v" else v }
                }
                if (updateTime != null) {
                    AppDetailsItem1(appVerName, totalSize, updateTime, Icons.Outlined.Info)
                } else {
                    val verSize = listOfNotNull(appVerName.takeIf { it.isNotEmpty() }, totalSize)
                        .joinToString(separator = " • ")
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
internal fun AppSdkItem(modifier: Modifier = Modifier, ver: VerInfo, title: String) {
    val sealIndex = ver.letterOrDev
    AppSdkItem(modifier, ver, title, sealIndex, rememberApiSeal(sealIndex))
}

@Composable
internal fun AppSdkItem(modifier: Modifier = Modifier, ver: VerInfo, title: String, sealIndex: Char, sealFile: File?) {
    AppSdkItem(
        modifier = modifier,
        label = when {
            LocalInspectionMode.current -> "$title ${ver.api}"
            else -> "$title ${ver.apiText}"
        },
        ver = ver.displaySdk,
        color = Color(SealMaker.getItemColorText(ver.api)),
        sealIndex = sealIndex,
        sealFile = sealFile,
    )
}

@Composable
fun rememberApiSeal(sealIndex: Char): File? {
    var sealFile: File? by remember(sealIndex) {
        // load initial value
        mutableStateOf(SealMaker.getBlurredCacheFile(sealIndex))
    }
    if (sealFile == null) {
        val context = LocalContext.current
        val itemWidth = with(LocalDensity.current) { 45.dp.roundToPx() }
        LaunchedEffect(sealIndex) {
            sealFile = SealMaker.getBlurredFile(context, sealIndex, itemWidth)
        }
    }
    return sealFile
}

@Composable
fun AppSdkItem(modifier: Modifier = Modifier, label: String, ver: String, color: Color, sealIndex: Char, sealFile: File?) {
    Column(
        modifier = modifier.padding(horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.rotate(if (sealIndex == 'u') 180f else 0f)) {
                if (sealFile != null) {
                    AsyncImage(
                        model = sealFile,
                        modifier = Modifier.size(30.dp),
                        contentDescription = null,
                    )
                } else {
                    val c = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    Box(modifier = Modifier.size(30.dp).background(c))
                }
                Icon(
                    modifier = Modifier.align(Alignment.BottomCenter).size(24.dp).offset(y = 10.5.dp),
                    imageVector = Icons.Outlined.Android,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.3f),
                )
            }
            Text(
                text = ver,
                color = color,
                fontSize = 14.sp,
                lineHeight = 14.sp,
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
            lineHeight = 9.sp,
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
            lineHeight = 11.sp,
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
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (label0 != null) {
            if (label.isNotEmpty()) {
                Text(
                    text = " • ",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = label0,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 11.sp,
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
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

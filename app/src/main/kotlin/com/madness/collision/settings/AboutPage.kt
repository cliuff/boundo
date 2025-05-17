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

import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.chief.graphics.AdaptiveIcon
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.dev.DarkPreview
import com.madness.collision.util.dev.LayoutDirectionPreviews
import com.madness.collision.util.ui.autoMirrored
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

class AboutOption(
    val icon: AboutOptionIcon,
    val label: String,
    val desc: String,
    val isExternalAction: Boolean = false,
    val action: (() -> Unit)? = null,
)

sealed interface AboutOptionIcon {
    @JvmInline value class Res(@DrawableRes val id: Int): AboutOptionIcon
    @JvmInline value class Vector(val value: ImageVector): AboutOptionIcon
}

@Composable
fun AboutPage(paddingValues: PaddingValues, options: List<AboutOption>) {
    val context = LocalContext.current
    val itemColor = remember { Color(ThemeUtil.getColor(context, R.attr.colorAItem)) }
    Settings(paddingValues = paddingValues, options = options, itemColor = itemColor)
}

@Composable
private fun Settings(paddingValues: PaddingValues, options: List<AboutOption>, itemColor: Color) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val cellWidth = remember {
            when {
                maxWidth >= 840.dp -> 200.dp
                maxWidth >= 600.dp -> 170.dp
                maxWidth >= 360.dp -> 170.dp
                else -> 160.dp
            }
        }
        LazyVerticalGrid(
            modifier = Modifier.widthIn(max = 900.dp).fillMaxSize(),
            columns = GridCells.Adaptive(cellWidth),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 10.dp,
                start = 10.dp,
                end = 10.dp,
            ),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.padding(top = 14.dp, bottom = 15.dp)) {
                    BuildDetails()
                }
            }
            items(options) { option ->
                SettingsItem(option = option, itemColor = itemColor)
            }
        }
    }
}

@Composable
private fun BuildDetails() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current
        val (appIcon, isRectIcon) = remember {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            drawable to (drawable?.let(AdaptiveIcon::hasRectIconMask) == true)
        }
        AsyncImage(
            model = appIcon,
            contentDescription = null,
            modifier = Modifier.size(100.dp)
                .let { if (isRectIcon) it.clip(AbsoluteSmoothCornerShape(12.dp, 80)) else it },
        )
        Spacer(modifier = Modifier.height(10.dp))
        val verText = "${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}"
        Text(
            text = stringResource(R.string.app_descriptive_name),
            fontSize = 13.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = verText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
        val buildMillis = BuildConfig.BUILD_TIMESTAMP
        val timeSpan = DateUtils.getRelativeTimeSpanString(buildMillis,
            System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        Text(
            text = timeSpan.toString(),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsItem(option: AboutOption, itemColor: Color) {
    val colorScheme = MaterialTheme.colorScheme
    val iconTint = colorScheme.onSurface.copy(alpha = 0.8f)
    val labelColor = colorScheme.onSurface.copy(alpha = 0.75f)
    val descColor = colorScheme.onSurface.copy(alpha = 0.65f)
    Column(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .let { option.action?.let { ac -> it.clickable(onClick = ac) } ?: it }
            .background(itemColor)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = option.icon
            Icon(
                modifier = Modifier.size(24.dp),
                painter = when (icon) {
                    is AboutOptionIcon.Res -> painterResource(icon.id)
                    is AboutOptionIcon.Vector -> rememberVectorPainter(icon.value)
                },
                contentDescription = null,
                tint = iconTint,
            )
            if (option.isExternalAction) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    modifier = Modifier.size(14.dp).autoMirrored(),
                    imageVector = Icons.Outlined.Launch,
                    contentDescription = null,
                    tint = iconTint,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (option.desc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = option.desc,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = descColor,
                lineHeight = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 10.sp,
            )
        }
    }
}

private val @receiver:DrawableRes Int.icon get() = AboutOptionIcon.Res(this)
private val ImageVector.icon get() = AboutOptionIcon.Vector(this)

@Composable
private fun SettingsPreview() {
    val options = remember {
        listOf(
            Triple("Open source licenses", Icons.TwoTone.Code.icon) { },
            Triple("Github", R.drawable.ic_github_24.icon) { },
            Triple("Email", Icons.TwoTone.Email.icon) { },
            Triple("Twitter", R.drawable.ic_twitter_24.icon) { },
            Triple("Telegram", R.drawable.ic_telegram_24.icon) { },
        ).map { (label, icon, action) -> AboutOption(icon, label, "$label description", true, action = action) }
    }
    val itemColor = if (isSystemInDarkTheme()) 0xff202020.toInt() else 0xfff2f2f2.toInt()
    Settings(paddingValues = PaddingValues(), options = options, itemColor = Color(itemColor))
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

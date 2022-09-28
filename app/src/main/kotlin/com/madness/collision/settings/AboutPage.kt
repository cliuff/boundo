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

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.ui.autoMirrored

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
fun AboutPage(mainViewModel: MainViewModel, options: List<AboutOption>) {
    val context = LocalContext.current
    val contentInsetTop by mainViewModel.contentWidthTop.observeAsState(0)
    val contentInsetBottom by mainViewModel.contentWidthBottom.observeAsState(0)
    val itemColor = remember { Color(ThemeUtil.getColor(context, R.attr.colorAItem)) }
    CompositionLocalProvider(
        LocalContentInsets provides (contentInsetTop to contentInsetBottom)
    ) {
        Settings(options = options, itemColor = itemColor)
    }
}

private val LocalContentInsets = compositionLocalOf { 0 to 0 }

@Composable
private fun Int.toDp() = with(LocalDensity.current) { toDp() }

@Composable
private fun Settings(options: List<AboutOption>, itemColor: Color) {
    val (insetTop, insetBottom) = LocalContentInsets.current
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = insetTop.toDp() + 8.dp,
                bottom = insetBottom.toDp() + 10.dp,
                start = 10.dp,
                end = 10.dp,
            ),
        ) {
            items(options) { option ->
                SettingsItem(option = option, itemColor = itemColor)
            }
        }
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
    Settings(options = options, itemColor = Color(itemColor))
}

@Preview(showBackground = true)
@Composable
private fun SettingsPagePreview() {
    MaterialTheme {
        SettingsPreview()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPageDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SettingsPreview()
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun SettingsPageRtlPreview() {
    MaterialTheme {
        SettingsPreview()
    }
}

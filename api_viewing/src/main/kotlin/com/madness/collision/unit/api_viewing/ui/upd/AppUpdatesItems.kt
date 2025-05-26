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

package com.madness.collision.unit.api_viewing.ui.upd

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.info.ExpIcon
import com.madness.collision.unit.api_viewing.info.ExpTag
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.unit.api_viewing.ui.comp.sealFileOf
import com.madness.collision.unit.api_viewing.ui.upd.item.ApiUpdGuiArt
import com.madness.collision.unit.api_viewing.ui.upd.item.AppApiUpdate
import com.madness.collision.unit.api_viewing.ui.upd.item.AppInstallVersion
import com.madness.collision.unit.api_viewing.ui.upd.item.AppVerUpdate
import com.madness.collision.unit.api_viewing.ui.upd.item.UpdGuiArt
import com.madness.collision.unit.api_viewing.ui.upd.item.VerUpdGuiArt
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.mainApplication
import com.madness.collision.util.ui.CompactPackageInfo
import com.madness.collision.util.ui.PackageInfo
import kotlinx.coroutines.flow.map
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

val LocalAppItemPrefs = compositionLocalOf { 0 }

@Composable
internal fun AppItem(art: UpdGuiArt, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tagGroup = remember(art) {
        art.expTags.map { tags ->
            val (ic, tx) = tags.partition { t -> t.icon !is ExpIcon.Text }
            AppTagGroup(ic, tx)
        }
    }
    val (backColor, accentColor) = remember(art) {
        SealMaker.getItemColorBack(context, art.apiInfo.api) to
                SealMaker.getItemColorAccent(context, art.apiInfo.api)
    }
    AppItem(
        modifier = modifier,
        name = art.identity.label,
        time = art.updateTime,
        apiText = art.apiInfo.displaySdk,
        sealLetter = art.apiInfo.letterOrDev,
        iconInfo = art.identity.iconPkgInfo,
        tagGroup = tagGroup.collectAsStateWithLifecycle(EmptyTagGroup).value,
        cardColor = Color(backColor),
        apiColor = Color(accentColor),
        onClick = art.onClick,
    )
}

@Composable
internal fun AppUpdateItem(art: ApiUpdGuiArt, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tagGroup = remember(art) {
        art.expTags.map { tags ->
            val (ic, tx) = tags.partition { t -> t.icon !is ExpIcon.Text }
            AppTagGroup(ic, tx)
        }
    }
    val backColor = remember(art) {
        SealMaker.getItemColorBack(context, art.newApiInfo.api)
    }
    AppUpdateItem(
        modifier = modifier,
        name = art.identity.label,
        iconInfo = art.identity.iconPkgInfo,
        tagGroup = tagGroup.collectAsStateWithLifecycle(EmptyTagGroup).value,
        cardColor = Color(backColor),
        newApi = art.newApiInfo,
        oldApi = art.oldApiInfo,
        newVer = art.newVersion,
        oldVer = art.oldVersion,
        onClick = art.onClick,
    )
}

@Composable
internal fun VerUpdateItem(art: VerUpdGuiArt, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tagGroup = remember(art) {
        art.expTags.map { tags ->
            val (ic, tx) = tags.partition { t -> t.icon !is ExpIcon.Text }
            AppTagGroup(ic, tx)
        }
    }
    val backColor = remember(art) {
        SealMaker.getItemColorBack(context, art.apiInfo.api)
    }
    VerUpdateItem(
        modifier = modifier,
        name = art.identity.label,
        iconInfo = art.identity.iconPkgInfo,
        tagGroup = tagGroup.collectAsStateWithLifecycle(EmptyTagGroup).value,
        cardColor = Color(backColor),
        newVer = art.newVersion,
        oldVer = art.oldVersion,
        onClick = art.onClick,
    )
}

@Stable
val DefaultAppItemStyle: AppItemStyle =
    AppItemStyle(
        iconSize = 48.dp,
        sealSize = 45.dp,
        nameTextStyle = TextStyle(fontSize = 14.sp, lineHeight = 16.sp),
        apiTextStyle = TextStyle(fontSize = 25.sp, lineHeight = 27.sp),
    )

@Stable
val CompactAppItemStyle: AppItemStyle =
    AppItemStyle(
        iconSize = 36.dp,
        sealSize = 36.dp,
        nameTextStyle = TextStyle(fontSize = 11.sp, lineHeight = 13.sp),
        apiTextStyle = TextStyle(fontSize = 20.sp, lineHeight = 22.sp),
    )

@Immutable
data class AppItemStyle(
    val iconSize: Dp,
    val sealSize: Dp,
    val nameTextStyle: TextStyle? = null,
    val apiTextStyle: TextStyle? = null,
)

val LocalAppItemStyle = compositionLocalOf { DefaultAppItemStyle }

@Composable
internal fun AppItem(
    modifier: Modifier = Modifier,
    name: String,
    time: String?,
    apiText: String,
    sealLetter: Char,
    iconInfo: PackageInfo,
    tagGroup: AppTagGroup,
    cardColor: Color,
    apiColor: Color,
    style: AppItemStyle = LocalAppItemStyle.current,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = AbsoluteSmoothCornerShape(18.dp, 80),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            val seal by sealFileOf(sealLetter)
            if (seal != null) {
                AsyncImage(
                    modifier = Modifier
                        .size(style.sealSize)
                        .align(BiasAlignment(0.4f, 0f)),
                    model = seal,
                    contentDescription = null,
                    alpha = 0.35f,
                )
            }
            AppItemContent(
                name = name,
                time = time,
                apiText = apiText,
                iconInfo = iconInfo,
                tagGroup = tagGroup,
                apiColor = apiColor,
                style = style,
            )
        }
    }
}

@Composable
private fun AppItemContent(
    name: String,
    time: String?,
    apiText: String,
    iconInfo: PackageInfo,
    tagGroup: AppTagGroup,
    apiColor: Color,
    style: AppItemStyle = DefaultAppItemStyle,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            modifier = Modifier.padding(2.dp).size(style.iconSize),
            iconInfo = iconInfo
        )
        Spacer(modifier = Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                // 3dp extracted from spacer to align with tag row's content
                modifier = Modifier.padding(start = 3.dp),
                text = name,
                color = MaterialTheme.colorScheme.onBackground,
                style = style.nameTextStyle ?: LocalTextStyle.current,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            if (tagGroup.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                AppTagRow(tagGroup = tagGroup)
            }
            if (time != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    modifier = Modifier.padding(start = 3.dp),
                    text = time,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
        Box(modifier = Modifier.padding(2.dp).widthIn(min = style.iconSize)) {
            Text(
                modifier = Modifier.align(BiasAlignment(-0.5f, 0f)),
                text = apiText,
                color = apiColor,
                style = style.apiTextStyle ?: LocalTextStyle.current,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun AppUpdateItem(
    modifier: Modifier = Modifier,
    name: String,
    iconInfo: PackageInfo,
    tagGroup: AppTagGroup,
    cardColor: Color,
    newApi: VerInfo,
    oldApi: VerInfo,
    newVer: AppInstallVersion,
    oldVer: AppInstallVersion,
    onClick: () -> Unit,
) {
    val containerColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFF101010) else Color.White
        false -> if (mainApplication.isDarkTheme) Color(0xFF101010) else Color.White
    }
    val cardBorderColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFF191919) else Color(0xFFF0F0F0)
        false -> if (mainApplication.isDarkTheme) Color(0xFF191919) else Color(0xFFF0F0F0)
    }
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = AbsoluteSmoothCornerShape(18.dp, 80),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        border = BorderStroke(0.5.dp, cardBorderColor),
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            AppUpdateHeader(name = name, iconInfo = iconInfo, tagGroup = tagGroup, cardColor = cardColor)
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                AppApiUpdate(newApi = newApi, oldApi = oldApi, newVer = newVer, oldVer = oldVer)
            }
        }
    }
}

@Composable
internal fun VerUpdateItem(
    modifier: Modifier = Modifier,
    name: String,
    iconInfo: PackageInfo,
    tagGroup: AppTagGroup,
    cardColor: Color,
    newVer: AppInstallVersion,
    oldVer: AppInstallVersion,
    onClick: () -> Unit,
) {
    val containerColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFF101010) else Color.White
        false -> if (mainApplication.isDarkTheme) Color(0xFF101010) else Color.White
    }
    val cardBorderColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFF191919) else Color(0xFFF0F0F0)
        false -> if (mainApplication.isDarkTheme) Color(0xFF191919) else Color(0xFFF0F0F0)
    }
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = AbsoluteSmoothCornerShape(18.dp, 80),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        border = BorderStroke(0.5.dp, cardBorderColor),
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            AppUpdateHeader(name = name, iconInfo = iconInfo, tagGroup = tagGroup, cardColor = cardColor)
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                AppVerUpdate(newVer = newVer, oldVer = oldVer)
            }
        }
    }
}

@Composable
private fun AppUpdateHeader(
    name: String,
    iconInfo: PackageInfo,
    tagGroup: AppTagGroup,
    cardColor: Color,
    modifier: Modifier = Modifier,
) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(AbsoluteSmoothCornerShape(
                        16.dp, 60, 16.dp, 60,
                        4.dp, 60, 4.dp, 60))
                    .background(cardColor)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(modifier = Modifier.size(32.dp), iconInfo = iconInfo)
                Spacer(modifier = Modifier.width(5.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        // 3dp extracted from spacer to align with tag row's content
                        modifier = Modifier.padding(start = 3.dp),
                        text = name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    if (tagGroup.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AppTagRow(tagGroup = tagGroup)
                    }
                }
            }
}

@Composable
fun AppIcon(modifier: Modifier = Modifier, iconInfo: PackageInfo) {
    if (!LocalInspectionMode.current) {
        AsyncImage(
            modifier = modifier,
            model = iconInfo,
            contentDescription = null,
        )
    } else {
        Box(modifier
            .clip(CircleShape)
            .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)))
    }
}

@Immutable
data class AppTagGroup(val icons: List<ExpTag>, val text: List<ExpTag>)

val EmptyTagGroup = AppTagGroup(icons = emptyList(), text = emptyList())

fun AppTagGroup.isNotEmpty() = !isEmpty()
fun AppTagGroup.isEmpty() = icons.isEmpty() && text.isEmpty()

@Composable
private fun AppTagRow(modifier: Modifier = Modifier, tagGroup: AppTagGroup) {
    val (iconTags, textTags) = tagGroup
    Row(
        modifier = modifier.clip(RoundedCornerShape(5.dp)).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in iconTags.indices) {
                    if (i > 0) Spacer(modifier = Modifier.width(2.5.dp))
                    key(iconTags[i].rank) { ExpIcon(icon = iconTags[i].icon) }
                }
            }
        }
        if (iconTags.isNotEmpty() && textTags.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
        }
        for (i in textTags.indices) {
            if (i > 0) Spacer(modifier = Modifier.width(4.dp))
            key(textTags[i].rank) { ExpIcon(icon = textTags[i].icon) }
        }
    }
}

@Composable
private fun ExpIcon(icon: ExpIcon) {
    when (icon) {
        is ExpIcon.Res -> ImageTag(image = icon.id)
        is ExpIcon.App -> ImageTag(image = icon.bitmap)
        is ExpIcon.Text -> TextTag(text = icon.value.toString())
    }
}

@Composable
private fun ImageTag(image: Any?, imagePadding: PaddingValues = PaddingValues()) {
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (LocalInspectionMode.current) {
            Image(
                modifier = Modifier.height(12.dp).padding(imagePadding),
                painter = painterResource(R.drawable.ic_cmp_72),
                contentDescription = null,
            )
        } else {
            AsyncImage(
                modifier = Modifier.height(12.dp).padding(imagePadding),
                model = image,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun TextTag(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 14.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 6.sp,
            lineHeight = 6.sp,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

internal fun PseudoAppIconInfo(): PackageInfo =
    object : CompactPackageInfo {
        override val handleable: Boolean = true
        override val verCode: Long = 0L
        override val uid: Int = 0
        override val packageName: String = ""
        override fun loadUnbadgedIcon(pm: PackageManager) = throw NotImplementedError()
    }

@PreviewCombinedColorLayout
@Composable
private fun AppUpdateItemPreview() {
    BoundoTheme {
        Surface() {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppUpdateItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    name = "Boundo",
                    iconInfo = PseudoAppIconInfo(),
                    tagGroup = EmptyTagGroup,
                    cardColor = Color(0xffe0ffd0),
                    newApi = VerInfo(35),
                    oldApi = VerInfo(34),
                    newVer = AppInstallVersion(135L, "3.906r", "1h ago"),
                    oldVer = AppInstallVersion(134L, "3.786r", "12h ago"),
                    onClick = {},
                )
                AppUpdateItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    name = "Boundo",
                    iconInfo = PseudoAppIconInfo(),
                    tagGroup = EmptyTagGroup,
                    cardColor = Color(0xffe0ffd0),
                    newApi = VerInfo(35),
                    oldApi = VerInfo(34),
                    newVer = AppInstallVersion(
                        135L, "3.906rfdfvhskfjsnnvsslkfjdiofusd_rel", "1h ago"),
                    oldVer = AppInstallVersion(
                        134L, "3.786rfdfvhskfjsnnvsslkfjdiofusd_rel", "12h ago"),
                    onClick = {},
                )
                AppItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    name = "Boundo",
                    time = "1 day ago",
                    apiText = "15",
                    sealLetter = 'v',
                    iconInfo = PseudoAppIconInfo(),
                    tagGroup = EmptyTagGroup,
                    cardColor = Color(0xffe0ffd0),
                    apiColor = Color(0xffbde1a4),
                    onClick = {},
                )
            }
        }
    }
}

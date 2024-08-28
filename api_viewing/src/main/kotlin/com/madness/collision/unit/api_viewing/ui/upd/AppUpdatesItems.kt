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
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import com.madness.collision.util.ui.CompactPackageInfo
import com.madness.collision.util.ui.PackageInfo

@Composable
internal fun AppItem(
    modifier: Modifier = Modifier,
    name: String,
    apiInfo: VerInfo,
    iconInfo: PackageInfo,
    timestamp: Long,
) {
    val context = LocalContext.current
    val updateTime = remember(timestamp) {
        DateUtils.getRelativeTimeSpanString(
            timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    AppItem(
        modifier = modifier,
        name = name,
        time = updateTime,
        apiText = apiInfo.displaySdk,
        iconInfo = iconInfo,
        cardColor = remember(apiInfo.api) { Color(SealMaker.getItemColorBack(context, apiInfo.api)) },
        apiColor = remember(apiInfo.api) { Color(SealMaker.getItemColorAccent(context, apiInfo.api)) },
    )
}

@Composable
fun AppItem(
    modifier: Modifier = Modifier,
    name: String,
    time: String?,
    apiText: String,
    iconInfo: PackageInfo,
    cardColor: Color,
    apiColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                modifier = Modifier.size(54.dp).padding(2.dp),
                iconInfo = iconInfo
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
                if (time != null) {
                    Text(
                        text = time,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
            Text(
                modifier = Modifier.widthIn(min = 40.dp),
                text = apiText,
                color = apiColor,
                fontSize = 25.sp,
                lineHeight = 25.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AppUpdateItem(
    modifier: Modifier = Modifier,
    name: String,
    iconInfo: PackageInfo,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(modifier = Modifier.size(32.dp), iconInfo = iconInfo)
                Spacer(modifier = Modifier.width(8.dp))
                Column() {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ImageTag(image = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        TextTag(text = "64-bit")
                        Spacer(modifier = Modifier.width(4.dp))
                        TextTag(text = "CORE")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppInstallation(135L, "3.896s", "1h ago")
            AppInstallation(134L, "3.786r", "12h ago")
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

@Composable
private fun ImageTag(image: Any?) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalInspectionMode.current) {
            Image(
                modifier = Modifier.height(12.dp),
                painter = painterResource(R.drawable.ic_cmp_72),
                contentDescription = null,
            )
        } else {
            AsyncImage(
                modifier = Modifier.height(12.dp),
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
            fontSize = 6.sp,
            lineHeight = 6.sp,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppInstallation(verCode: Long, verName: String, time: String) {
    Row() {
        Text(text = verCode.toString(), fontSize = 9.sp, lineHeight = 10.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = verName, fontSize = 9.sp, lineHeight = 10.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = time, fontSize = 9.sp, lineHeight = 10.sp)
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
        Column(modifier = Modifier.fillMaxWidth()) {
            AppUpdateItem(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                name = "Google Space",
                iconInfo = PseudoAppIconInfo(),
            )
            AppItem(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                name = "Boundo",
                time = "1 day ago",
                apiText = "15",
                iconInfo = PseudoAppIconInfo(),
                cardColor = Color(0xffe0ffd0),
                apiColor = Color(0xffbde1a4),
            )
        }
    }
}

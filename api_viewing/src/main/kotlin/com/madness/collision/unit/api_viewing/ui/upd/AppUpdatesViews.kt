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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.util.mainApplication
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
@ReadOnlyComposable
fun updateIndexLabel(index: AppUpdatesIndex): String {
    return stringResource(when (index) {
        AppUpdatesIndex.NEW -> R.string.av_upd_new
        AppUpdatesIndex.UPG -> R.string.av_upd_upg
        AppUpdatesIndex.VER -> R.string.av_upd_ver_upd
        AppUpdatesIndex.PCK -> R.string.av_upd_pck_upd
        AppUpdatesIndex.REC -> R.string.av_updates_recents
        AppUpdatesIndex.USE -> R.string.av_upd_used
    })
}

@Composable
fun UpdateNothing(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Text(
            text = stringResource(com.madness.collision.R.string.text_no_content),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            fontSize = 13.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun UpdateSectionTitle(modifier: Modifier = Modifier, text: String) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            fontSize = 14.sp,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(10.dp))
        val color = colorResource(com.madness.collision.R.color.androidRobotGreen)
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun MoreUpdatesButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(modifier = modifier, onClick = onClick) {
        Text(
            text = stringResource(R.string.av_updates_view_more),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            fontSize = 12.sp,
            lineHeight = 12.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
fun UsageAccessRequest(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                .padding(7.dp)
        ) {
            Icon(
                modifier = Modifier.size(15.dp),
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.av_upd_usage_access),
                color = MaterialTheme.colorScheme.onBackground.copy(0.85f),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.av_upd_usage_access_msg),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                fontSize = 10.sp,
                lineHeight = 10.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
    }
}

@Composable
fun QueryInstalledAppsRequest(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val contentColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else Color.White
        false -> if (mainApplication.isDarkTheme) MaterialTheme.colorScheme.onSurface else Color.White
    }
    val containerColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFF381000) else Color(0xFFFF6F00)
        false -> if (mainApplication.isDarkTheme) Color(0xFF381000) else Color(0xFFFF6F00)
    }
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = AbsoluteSmoothCornerShape(10.dp, 60),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.9f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.av_upd_installed_query),
                    color = contentColor.copy(0.95f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.av_upd_installed_query_msg),
                    color = contentColor.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.92f),
            )
        }
    }
}

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

package com.madness.collision.unit.api_viewing.ui.upd.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.ui.info.AppSdkItem

@Composable
internal fun AppApiUpdate(
    newApi: VerInfo,
    oldApi: VerInfo,
    newVer: AppInstallVersion,
    oldVer: AppInstallVersion,
) {
    Row(
        modifier = Modifier.widthIn(max = 310.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val targetTitle = stringResource(com.madness.collision.R.string.apiSdkTarget)
        Column(
            // weight(1f) for equal widths
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!LocalInspectionMode.current) {
                AppSdkItem(ver = oldApi, title = targetTitle)
            } else {
                AppSdkItem(ver = oldApi, title = targetTitle, sealIndex = 'u', sealFile = null)
            }
            Spacer(modifier = Modifier.height(6.dp))
            AppInstallationColumn(verCode = oldVer.code, verName = oldVer.name, time = oldVer.time)
        }
        Icon(
            modifier = Modifier.padding(bottom = 12.dp).size(24.dp),
            imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
        Column(
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!LocalInspectionMode.current) {
                AppSdkItem(ver = newApi, title = targetTitle)
            } else {
                AppSdkItem(ver = newApi, title = targetTitle, sealIndex = 'v', sealFile = null)
            }
            Spacer(modifier = Modifier.height(6.dp))
            AppInstallationColumn(verCode = newVer.code, verName = newVer.name, time = newVer.time)
        }
    }
}

@Composable
private fun AppInstallationColumn(verCode: Long, verName: String?, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val verNo = verCode.toString()
        if (verName == null || verName.length <= time.length) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (verName != null) {
                    AppUpdVerName(modifier = Modifier.weight(1f, fill = false), ver = verName)
                    Spacer(modifier = Modifier.width(2.dp))
                }
                AppUpdVerNo(ver = verNo)
            }
            Spacer(modifier = Modifier.height(1.dp))
            AppUpdTime(time = time)
        } else if (verNo.length >= time.length - 2) {
            AppUpdVerName(ver = verName, maxLines = 2)
            Spacer(modifier = Modifier.height(1.dp))
            AppUpdVerNo(ver = verNo)
            Spacer(modifier = Modifier.height(1.dp))
            AppUpdTime(time = time)
        } else {
            AppUpdVerName(ver = verName, maxLines = 2)
            Spacer(modifier = Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppUpdVerNo(ver = verNo)
                Spacer(modifier = Modifier.width(2.dp))
                AppUpdTime(time = time)
            }
        }
    }
}

@Composable
private fun AppUpdVerName(ver: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(
        modifier = modifier,
        text = ver,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        fontSize = 10.sp,
        lineHeight = 10.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AppUpdVerNo(ver: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // U+2116 numero sign
        AppVersionType(type = "№")
        Text(
            text = ver,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 10.sp,
            lineHeight = 10.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppUpdTime(time: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.padding(horizontal = 2.dp).size(8.8.dp),
            imageVector = Icons.Outlined.Update,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
        )
        Text(
            text = time,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppVersionType(type: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .padding(horizontal = 2.dp),
    ) {
        Text(
            text = type,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 10.sp,
            lineHeight = 10.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

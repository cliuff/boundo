/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.info.tag

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

@Composable
fun PkgArchDetails(app: ApiViewingApp) {
    val abis = listOfNotNull(
        if (app.nativeLibraries[0]) "armeabi-v7a" else null,
        if (app.nativeLibraries[1]) "arm64-v8a" else null,
        if (app.nativeLibraries[2]) "x86" else null,
        if (app.nativeLibraries[3]) "x86_64" else null,
    )
    PkgArchDetails(abiList = abis)
}

@Composable
fun PkgArchDetails(abiList: List<String>) {
    val (systemAppAbis, otherAppAbis) = remember(abiList) {
        val sysAbiSet = Build.SUPPORTED_ABIS.toSet()
        abiList.partition(sysAbiSet::contains)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
    ) {
        ArchTitle(text = stringResource(R.string.av_info_arch_abi_system))
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val abiSet64 = Build.SUPPORTED_64_BIT_ABIS.toSet()
            val abiSet32 = Build.SUPPORTED_32_BIT_ABIS.toSet()
            for (i in Build.SUPPORTED_ABIS.indices) {
                val abi = Build.SUPPORTED_ABIS[i]
                ArchAbi(
                    abi = abi,
                    checked = abi in systemAppAbis,
                    isPrimary = i == 0,
                    abiBits = if (abi in abiSet64) "64-bit" else if (abi in abiSet32) "32-bit" else null,
                )
            }
        }

        if (otherAppAbis.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ArchTitle(text = stringResource(R.string.av_info_arch_abi_other))
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (abi in otherAppAbis) {
                    ArchAbi(abi = abi, checked = true)
                }
            }
        }

        if (abiList.isEmpty()) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.av_info_arch_none),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 10.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

@Composable
private fun ArchTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        fontSize = 10.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ArchAbi(
    abi: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    abiBits: String? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checked) {
                Icon(
                    modifier = Modifier.size(11.dp),
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                )
            }
            Text(
                text = abi,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (!checked) 0.6f else 0.9f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (isPrimary && checked) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
            )
            if (checked) {
                // extra space for optical balance
                Spacer(modifier = Modifier.width(3.dp))
            }
        }
        if (abiBits != null) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = abiBits,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (!checked) 0.54f else 0.8f),
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}

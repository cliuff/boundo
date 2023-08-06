/*
 * Copyright 2023 Clifford Liu
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.util.ui.autoMirrored

interface AppSwitcherHandler {
    fun getPreviousPreview(): ApiViewingApp?
    fun getNextPreview(): ApiViewingApp?
    fun loadPrevious()
    fun loadNext()

    // used for overlay target
    fun getApp(pkgName: String): ApiViewingApp?
    fun loadApp(app: ApiViewingApp)
}

@Composable
fun AppSwitcher(modifier: Modifier = Modifier, handler: AppSwitcherHandler) {
    val context = LocalContext.current
    var previousApp: ApiViewingApp? by remember { mutableStateOf(null) }
    var nextApp: ApiViewingApp? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        previousApp = handler.getPreviousPreview()
        nextApp = handler.getNextPreview()
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = previousApp != null,
                    onClick = { handler.loadPrevious() },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                )
                .padding(vertical = 20.dp)
                .padding(start = 8.dp, end = 30.dp),
        ) {
            val previewApp = previousApp
            if (previewApp != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).autoMirrored(),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    )
                    AsyncImage(
                        modifier = Modifier.height(12.dp).widthIn(max = 12.dp),
                        model = remember { AppPackageInfo(context, previewApp) },
                        contentDescription = null,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .clickable(
                    enabled = nextApp != null,
                    onClick = { handler.loadNext() },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                )
                .padding(vertical = 20.dp)
                .padding(start = 30.dp, end = 8.dp),
        ) {
            val previewApp = nextApp
            if (previewApp != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        modifier = Modifier.height(12.dp).widthIn(max = 12.dp),
                        model = remember { AppPackageInfo(context, previewApp) },
                        contentDescription = null,
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).autoMirrored(),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    )
                }
            }
        }
    }
}


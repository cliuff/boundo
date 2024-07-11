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

package com.madness.collision.unit.api_viewing.ui.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.util.dev.PreviewCombinedColorLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListBar(
    isRefreshing: Boolean,
    windowInsets: WindowInsets = WindowInsets(0),
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    primaryAction: @Composable () -> Unit,
) {
    val refreshRotation by rememberRefreshRotation(isRefreshing)
    TopAppBar(
        title = { },
        actions = {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isRefreshing,
                    enter = fadeIn(),
                    exit = shrinkOut() + fadeOut(),
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp).rotate(refreshRotation),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        )
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isRefreshing,
                    enter = fadeIn() + expandIn(),
                    exit = fadeOut(),
                    content = { primaryAction() },
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
        },
        windowInsets = windowInsets,
        colors = colors,
    )
}

@Composable
private fun rememberRefreshRotation(isRefreshing: Boolean): State<Float> {
    if (!isRefreshing) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition("InfRefreshAnimation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "RefreshAnimation",
    )
}

@Composable
fun AppListBarAction(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}

@Composable
fun AppListBarAction(icon: ImageVector, label: String?, onClick: () -> Unit) {
    BadgedBox(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 20.dp),
            ),
        badge = {
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                content = label?.let { {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } }
            )
        },
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSrcTypeSwitcher(
    types: Map<ListSrcCat, String>,
    selType: ListSrcCat,
    onSelType: (ListSrcCat) -> Unit
) {
    val selIndex = types.keys.indexOf(selType)
    if (types.isNotEmpty() && selIndex >= 0) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selIndex,
            divider = {},
            containerColor = Color.Transparent
        ) {
            for ((srcType, typeLabel) in types) {
                Tab(
                    selected = srcType == selType,
                    onClick = { onSelType(srcType) },
                    text = { Text(text = typeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewCombinedColorLayout
@Composable
private fun AppListBarPreview() {
    val cats = remember {
        mapOf(
            ListSrcCat.Platform to "Platform",
            ListSrcCat.Temporary to "Temp",
        )
    }
    BoundoTheme {
        Column() {
            AppListBar(isRefreshing = false) {
                AppListBarAction(icon = Icons.Outlined.CheckCircle, label = "2", onClick = { })
            }
            Spacer(modifier = Modifier.height(20.dp))
            AppSrcTypeSwitcher(types = cats, selType = ListSrcCat.Platform, onSelType = { })
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

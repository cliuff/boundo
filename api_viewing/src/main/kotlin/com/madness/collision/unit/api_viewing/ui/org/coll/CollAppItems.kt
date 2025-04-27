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

package com.madness.collision.unit.api_viewing.ui.org.coll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madness.collision.chief.layout.SubcomposeTargetSize
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.madness.collision.R as MainR

internal fun <T> List<T>.getGroup(grouping: List<Int>, groupIndex: Int): List<T> {
    // end index should be greater than 0
    if (grouping[groupIndex] <= 0) return emptyList()
    // iterate backwards to find the first end index > 0
    val startIndex = (groupIndex - 1 downTo 0).find { i -> grouping[i] > 0 }
    return subList(startIndex?.let(grouping::get) ?: 0, grouping[groupIndex])
}

@Composable
@ReadOnlyComposable
fun collAppGroupHeading(groupIndex: Int): String =
    when (groupIndex) {
        0 -> "Installed launcher apps"
        1 -> "User services & components"
        2 -> "System services & components"
        3 -> "Misc system components"
        4 -> "System overlay components"
        else -> "Installed apps"
    }

@Composable
fun CollAppHeading(
    name: String,
    modifier: Modifier = Modifier,
    changeViewText: String = "",
    onChangeView: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        TextButton(onClick = onChangeView) {
            Text(
                text = changeViewText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Stable
val CompactCollAppItemStyle: CollAppItemStyle =
    CollAppItemStyle(
        showAppTypeLabel = false,
        showSecondaryText = false,
    )

@Stable
val DetailedCollAppItemStyle: CollAppItemStyle =
    CollAppItemStyle(
        showAppTypeLabel = true,
        showSecondaryText = true,
    )

@Immutable
data class CollAppItemStyle(
    val showAppTypeLabel: Boolean,
    val showSecondaryText: Boolean,
)

val LocalCollAppItemStyle = compositionLocalOf { CompactCollAppItemStyle }

@Composable
fun CollAppItem(
    name: String,
    iconModel: Any?,
    modifier: Modifier = Modifier,
    typeText: String? = null,
    typeIcon: ImageVector? = null,
    secondaryText: String? = null,
    style: CollAppItemStyle = LocalCollAppItemStyle.current,
    desc: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconModel != null) {
            AsyncImage(
                modifier = Modifier.width(36.dp).heightIn(max = 36.dp),
                model = iconModel,
                contentDescription = null,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            InlineAppName(
                modifier = Modifier.animateContentSize(),
                name = name,
                typeText = typeText.takeIf { style.showAppTypeLabel },
                typeIcon = typeIcon,
                iconTint = when (typeText) {
                    "/system", "/system_ext", "/vendor", "/odm", "/apex" ->
                        colorResource(MainR.color.androidRobotGreen).copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                },
            )
            AnimatedVisibility(visible = style.showSecondaryText) {
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                    )
                }
            }
            desc()
        }
    }
}

@Composable
private fun InlineAppName(
    name: String,
    typeText: String?,
    typeIcon: ImageVector?,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalContentColor.current,
) {
    val hasTarget = typeText != null || typeIcon != null
    val annotatedText = buildAnnotatedString {
        val nameLength = name.length
        if (nameLength > 100) {
            append(name.substring(0, 99))
            append('…')
        } else {
            append(name)
        }
        if (hasTarget) appendInlineContent("Target")
    }
    val target = @Composable {
        if (hasTarget) {
            AppType(
                modifier = Modifier.padding(start = 8.dp),
                text = typeText,
                icon = typeIcon,
                iconTint = iconTint,
            )
        }
    }
    SubcomposeTargetSize(
        modifier = modifier,
        target = target,
        content = { (tw, th) ->
            val inlineContent = if (tw > 0 && th > 0) {
                with(LocalDensity.current) {
                    val ph = Placeholder(tw.toSp(), th.toSp(), PlaceholderVerticalAlign.TextCenter)
                    mapOf("Target" to InlineTextContent(placeholder = ph, children = { target() }))
                }
            } else {
                emptyMap()
            }
            Text(
                text = annotatedText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                inlineContent = inlineContent,
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollAppGroupRow(names: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (name in names) {
            AppGroup(name = name)
        }
    }
}

@Composable
private fun AppGroup(name: String) {
    Box(
        Modifier
            .clip(AbsoluteSmoothCornerShape(3.dp, 80))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.075f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppType(
    text: String?,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalContentColor.current,
) {
    Row(
        modifier = modifier
            .border(Dp.Hairline, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape = CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(12.dp),
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
        }
        if (text != null)
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

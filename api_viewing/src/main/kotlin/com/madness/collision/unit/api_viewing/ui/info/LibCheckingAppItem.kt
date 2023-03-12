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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.info.ValueComponent

@Composable
fun AppCompLabel(
    modifier: Modifier = Modifier,
    label: String,
    labelColor: Color,
    desc: ValueComponent.AppComp,
    descColor: Color,
    descMaxLines: Int,
    enabled: Boolean = true,
    showDescEnabledTag: Boolean = false,
) {
    Column(modifier = modifier) {
        if (enabled) {
            CompLabel(label = label, color = labelColor)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompLabel(
                    modifier = Modifier.weight(1f, fill = false),
                    label = label,
                    color = labelColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                CompLabelEnabledTag(color = labelColor)
            }
        }
        if (!showDescEnabledTag) {
            Text(
                modifier = Modifier.padding(vertical = 0.5.dp),
                text = desc.comp.value,
                color = descColor,
                fontSize = 7.sp,
                lineHeight = 8.sp,
                maxLines = descMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            InlineEnabledDesc(
                modifier = Modifier.padding(vertical = 0.5.dp),
                desc = listOf(desc),
                fontSize = 7.sp,
                lineHeight = 8.sp,
                normalColor = descColor,
                disabledColor = descColor,
                maxLines = descMaxLines,
            )
        }
    }
}

@Composable
fun CompLabelEnabledTag(color: Color) {
    Text(
        modifier = Modifier
            .border(Dp.Hairline, color.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        text = stringResource(R.string.av_info_lib_comp_disabled),
        color = color,
        fontSize = 5.sp,
        lineHeight = 6.sp,
    )
}

@Composable
private fun CompDescEnabledTag(color: Color) {
    Text(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .border(Dp.Hairline, color.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        text = stringResource(R.string.av_info_lib_comp_disabled),
        color = color,
        fontSize = 4.sp,
        lineHeight = 5.sp,
    )
}

@Composable
fun CompFullDesc(
    modifier: Modifier = Modifier,
    desc: List<ValueComponent.AppComp>,
    normalColor: Color,
    disabledColor: Color,
    showDescEnabledTag: Boolean,
) {
    if (desc.isEmpty()) {
        Box(modifier = modifier)
    } else {
        // use multiple items to have different line spacing
        // between a. wrapping a line into two and b. two normal lines
        Column(modifier = modifier) {
            for (descItem in desc) {
                if (!showDescEnabledTag) {
                    Text(
                        modifier = Modifier.padding(vertical = 0.5.dp),
                        text = descItem.value,
                        color = if (desc[0].isEnabled) normalColor else disabledColor,
                        fontSize = 7.sp,
                        lineHeight = 8.sp,
                    )
                } else {
                    InlineEnabledDesc(
                        modifier = Modifier.padding(vertical = 0.5.dp),
                        desc = listOf(descItem),
                        fontSize = 7.sp,
                        lineHeight = 8.sp,
                        normalColor = normalColor,
                        disabledColor = disabledColor
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineEnabledDesc(
    modifier: Modifier = Modifier,
    desc: List<ValueComponent.AppComp>,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    normalColor: Color,
    disabledColor: Color,
    maxLines: Int = Int.MAX_VALUE,
) {
    val annotatedDesc = buildAnnotatedString {
        val disabledStyle = SpanStyle(color = disabledColor)
        desc.forEachIndexed { index, appComp ->
            if (index > 0) append('\n')
            if (appComp.isEnabled) {
                append(appComp.value)
            } else {
                withStyle(disabledStyle) { append(appComp.value) }
                appendInlineContent("disabledTag")
            }
        }
    }
    MeasureUnconstrainedSize(
        modifier = modifier,
        target = { CompDescEnabledTag(disabledColor) },
        content = { tagSize ->
            val (tagWidth, tagHeight) = with(LocalDensity.current) {
                tagSize.width.toSp() to tagSize.height.toSp()
            }
            val disabledTagTextContent = InlineTextContent(
                placeholder = Placeholder(tagWidth, tagHeight, PlaceholderVerticalAlign.TextCenter),
                children = { CompDescEnabledTag(disabledColor) },
            )
            Text(
                text = annotatedDesc,
                color = normalColor,
                fontSize = fontSize,
                lineHeight = lineHeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                inlineContent = mapOf("disabledTag" to disabledTagTextContent),
            )
        },
    )
}

private data class UnconstrainedSize(val width: Int, val height: Int)

@Composable
private fun MeasureUnconstrainedSize(
    modifier: Modifier = Modifier,
    target: @Composable () -> Unit,
    content: @Composable (size: UnconstrainedSize) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val placeable = subcompose("target", target)[0].measure(Constraints())
        val size = UnconstrainedSize(placeable.width, placeable.height)
        val contentPlaceable = subcompose("content") { content(size) }[0].measure(constraints)
        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.placeRelative(0, 0)
        }
    }
}

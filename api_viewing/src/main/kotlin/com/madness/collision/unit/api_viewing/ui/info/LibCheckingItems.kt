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

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import coil3.compose.AsyncImage
import com.madness.collision.chief.lang.runIf
import com.madness.collision.unit.api_viewing.info.*
import com.madness.collision.util.ColorUtil
import com.madness.collision.util.mainApplication
import com.madness.collision.util.ui.rememberExpandableTextLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun PlainLibItem(item: PackComponent, horizontalPadding: Dp = 20.dp) {
    when (item) {
        is ValueComponent.NativeLib -> {
            PlainNativeLibItem(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 3.dp),
                comp = item,
            )
        }
        is ValueComponent.SharedLib -> {
            PlainSharedLibItem(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 3.dp),
                comp = item,
            )
        }
        else -> {
            // apply expandable end padding, to allow for more text to display
            val textLayout = rememberExpandableTextLayout(item, horizontalPadding)
            Text(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .padding(start = horizontalPadding)
                    .runIf({ textLayout.expandLine.not() }, { padding(end = horizontalPadding) }),
                text = item.comp.value,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                onTextLayout = textLayout::onTextLayout,
            )
        }
    }
}

@Composable
fun PlainChunkPartLibItem(item: PackComponent, horizontalPadding: Dp = 20.dp) {
    Text(
        modifier = Modifier.padding(vertical = 3.dp).padding(horizontal = horizontalPadding),
        text = item.comp.value,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        fontSize = 10.sp,
        lineHeight = 14.sp,
    )
}

@Composable
private fun PlainSharedLibItem(modifier: Modifier = Modifier, comp: ValueComponent.SharedLib) {
    val libPrefs = LocalLibPrefs.current
    if (libPrefs.preferSystemLibs || comp.desc != null) {
        SharedLibItem(modifier = modifier, label = comp.value, type = comp.type, desc = comp.desc)
    }
}

@Composable
fun MarkedSimpleLibItem(item: MarkedComponent, horizontalPadding: Dp = 20.dp) {
    when (val compUiType = remember { item.getUiType() }) {
        is MarkedCompUiType.Simple ->
            MarkedSimpleLibItem(item = item, enabled = true, horizontalPadding = horizontalPadding)
        is MarkedCompUiType.AppComp ->
            MarkedSimpleLibItem(item = item, enabled = compUiType.comp.comp.isEnabled, horizontalPadding = horizontalPadding)
        is MarkedCompUiType.NativeLib ->
            MarkedSimpleNativeLibItem(item = compUiType.comp, horizontalPadding = horizontalPadding)
        else -> Unit
    }
}

@Composable
fun MarkedLibItem(item: MarkedComponent) {
    val compUiType = remember { item.getUiType() }
    when (compUiType.cat) {
        CompUiCat.Simple -> MarkedSimpleItem(item = item, compUiType = compUiType)
        CompUiCat.AppComp -> MarkedAppCompItem(item = item, compUiType = compUiType)
        CompUiCat.NativeLib -> MarkedNativeLibItem(item = item, compUiType = compUiType)
    }
}

@Composable
private fun LibIcon(resId: Int, mono: Boolean, size: Dp, alpha: Float = 1f) {
    if (mono) {
        Icon(
            modifier = Modifier.width(size).heightIn(max = size),
            painter = painterResource(resId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
    } else {
        AsyncImage(
            modifier = Modifier.width(size).heightIn(max = size),
            model = resId,
            contentDescription = null,
            alpha = alpha,
        )
    }
}

@Composable
private fun MarkedSimpleLibItem(
    modifier: Modifier = Modifier, item: MarkedComponent, enabled: Boolean = true,
    horizontalPadding: Dp = 20.dp, itemContent: @Composable () -> Unit) {
    val iconAlpha = if (enabled) 1f else 0.7f
    val textAlpha = if (enabled) 1f else 0.7f
    Column(modifier = modifier.padding(vertical = 3.dp).padding(start = horizontalPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.markedIconResId?.let { icId ->
                LibIcon(resId = icId, mono = item.isMarkedIconMono, size = 8.dp, alpha = iconAlpha)
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = item.markedLabel,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f * textAlpha),
                fontWeight = FontWeight.Medium,
                fontSize = 8.sp,
                lineHeight = 10.sp,
            )
            if (!enabled) {
                Spacer(modifier = Modifier.width(4.dp))
                CompLabelEnabledTag(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f * textAlpha),
                )
            }
        }
        itemContent()
    }
}

@Composable
fun MarkedSimpleChunkPartLibItem(item: MarkedComponent, horizontalPadding: Dp = 20.dp) {
    Column(modifier = Modifier.padding(horizontalPadding, vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.markedIconResId?.let { icId ->
                LibIcon(resId = icId, mono = item.isMarkedIconMono, size = 5.dp)
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = item.markedLabel,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium,
                fontSize = 5.sp,
                lineHeight = 5.sp,
            )
        }
        Text(
            text = item.comp.value,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            fontSize = 8.sp,
            lineHeight = 8.sp,
        )
    }
}

@Composable
private fun MarkedSimpleLibItem(item: MarkedComponent, enabled: Boolean = true, horizontalPadding: Dp = 20.dp) {
    // apply expandable end padding, to allow for more text to display
    val textLayout = rememberExpandableTextLayout(item, horizontalPadding)
    MarkedSimpleLibItem(
        modifier = (Modifier as Modifier)
            .runIf({ textLayout.expandLine.not() }, { padding(end = horizontalPadding) }),
        item = item,
        enabled = enabled,
        horizontalPadding = horizontalPadding,
    ) {
        val textAlpha = if (enabled) 1f else 0.7f
        Text(
            text = item.comp.value,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f * textAlpha),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            onTextLayout = textLayout::onTextLayout,
        )
    }
}

@Composable
private fun MarkedSimpleNativeLibItem(
    item: MarkedValueComp<ValueComponent.NativeLib>,
    horizontalPadding: Dp = 20.dp,
) {
    MarkedSimpleLibItem(item = item, enabled = true, horizontalPadding = horizontalPadding) {
        PlainNativeLibItem(comp = item.comp)
    }
}

@Composable
private fun PlainNativeLibItem(modifier: Modifier = Modifier, comp: ValueComponent.NativeLib) {
    val context = LocalContext.current
    val libPrefs = LocalLibPrefs.current
    val nativeLibValue = remember(libPrefs.preferCompressedSize) {
        comp.getDesc(context, libPrefs.preferCompressedSize)
    }
    NativeLibNormalCompEntry(modifier = modifier, label = comp.value, desc = nativeLibValue)
}

@Composable
private fun MarkedNativeLibItem(item: MarkedComponent, compUiType: MarkedCompUiType) {
    val context = LocalContext.current
    val libPrefs = LocalLibPrefs.current
    when (compUiType) {
        is MarkedCompUiType.MergingNativeLib -> {
            val compList = compUiType.comp.components
            var showFullValues by remember { mutableStateOf(false) }
            val descList = remember(libPrefs.preferCompressedSize) {
                compList.map { it.getDesc(context, libPrefs.preferCompressedSize) }
            }
            Box(
                modifier = Modifier
                    .clickable { showFullValues = !showFullValues }
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                if (showFullValues) {
                    Column {
                        Box(modifier = Modifier.padding(start = 34.dp)) {
                            CompLabel(label = item.markedLabel)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Column(modifier = Modifier.padding(start = 34.dp)) {
                            for (index in compList.indices) {
                                if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                                NativeLibCompEntry(label = compList[index].value, desc = descList[index])
                            }
                        }
                    }
                } else {
                    Column {
                        Box(modifier = Modifier.padding(start = 34.dp)) {
                            CompLabel(label = item.markedLabel)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.width(34.dp))
                            for (index in compList.indices) {
                                if (index > 0) Spacer(modifier = Modifier.width(5.dp))
                                NativeLibCompEntry(label = compList[index].value, desc = descList[index])
                            }
                            Spacer(modifier = Modifier.width(50.dp))
                        }
                    }
                }
                val markedIconResId = item.markedIconResId
                if (markedIconResId != null) {
                    val isDarkTheme = mainApplication.isDarkTheme
                    val cardColor = if (isDarkTheme) Color(0xff0c0c0c) else Color.White
                    CompIcon(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(color = cardColor),
                        iconId = markedIconResId,
                        isMono = item.isMarkedIconMono,
                    )
                }
                CompAction(
                    modifier = Modifier.align(Alignment.TopEnd),
                    label = compList.size.toString(),
                    icon = if (showFullValues) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                )
            }
        }
        is MarkedCompUiType.NativeLib -> {
            val markedComp = compUiType.comp
            val nativeLibValue = remember(libPrefs.preferCompressedSize) {
                markedComp.comp.getDesc(context, libPrefs.preferCompressedSize)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                val markedIconResId = item.markedIconResId
                if (markedIconResId != null) {
                    CompIcon(
                        modifier = Modifier.padding(end = 8.dp),
                        iconId = markedIconResId,
                        isMono = item.isMarkedIconMono,
                    )
                }
                Column {
                    CompLabel(label = item.markedLabel)
                    Spacer(modifier = Modifier.height(2.dp))
                    NativeLibCompEntry(label = markedComp.comp.value, desc = nativeLibValue)
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun MarkedAppCompItem(item: MarkedComponent, compUiType: MarkedCompUiType) {
    val enabled = when (compUiType) {
        is MarkedCompUiType.AppComp -> compUiType.comp.comp.isEnabled
        is MarkedCompUiType.MergingAppComp -> compUiType.comp.components.any { it.isEnabled }
        else -> true
    }
    MarkedSimpleItem(item, compUiType, enabled)
}

@Composable
private fun MarkedSimpleItem(item: MarkedComponent, compUiType: MarkedCompUiType, enabled: Boolean = true) {
    var showFullValues by remember { mutableStateOf(false) }
    var itemValues: List<ValueComponent.AppComp>? by remember { mutableStateOf(null) }
    var itemValuesString: String? by remember { mutableStateOf(null) }
    // constrain visibility and animate content size of ConstraintLayout instead of using AnimatedVisibility,
    // which does not work with ConstraintLayout (size change does not get recomposed)
    val constraints = remember(showFullValues) {
        ConstraintSet {
            val icon = createRefFor("icon")
            val label = createRefFor("label")
            val expandIcon = createRefFor("expandIcon")
            val fullValues = createRefFor("fullValues")
            constrain(icon) {
                centerVerticallyTo(parent, 0f)
                start.linkTo(parent.start)
            }
            constrain(label) {
                width = Dimension.preferredWrapContent
                height = Dimension.preferredWrapContent
                centerVerticallyTo(icon)
                linkTo(start = icon.end, end = expandIcon.start, bias = 0f)
            }
            constrain(expandIcon) {
                centerVerticallyTo(label)
                end.linkTo(parent.end)
            }
            constrain(fullValues) {
                width = Dimension.preferredWrapContent
                height = Dimension.preferredWrapContent
                top.linkTo(label.bottom)
                linkTo(start = icon.end, end = parent.end, bias = 0f)
                visibility = if (showFullValues) Visibility.Visible else Visibility.Gone
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    ConstraintLayout(
        modifier = Modifier
            .let click@{ modifier ->
                if (item !is MarkedMergingComp<*>) return@click modifier
                modifier.clickable {
                    showFullValues = !showFullValues
                    if (itemValues != null || itemValuesString != null) return@clickable
                    coroutineScope.launch(Dispatchers.Default) {
                        when (compUiType) {
                            is MarkedCompUiType.MergingSimple -> {
                                itemValuesString = compUiType.comp.components.drop(1)
                                    .joinToString(separator = "\n") { it.value }
                            }
                            is MarkedCompUiType.MergingAppComp -> {
                                itemValues = compUiType.comp.components.drop(1)
                            }
                            else -> Unit
                        }
                    }
                }
            }
            .fillMaxWidth()
            .let { if (item is MarkedMergingComp<*> && item.components.size < 400) it.animateContentSize() else it }
            .padding(horizontal = 20.dp, vertical = 4.dp),
        constraintSet = constraints,
    ) {
        val markedIconResId = item.markedIconResId
        if (markedIconResId != null) {
            CompIcon(
                modifier = Modifier.layoutId("icon").padding(end = 8.dp),
                iconId = markedIconResId,
                isMono = item.isMarkedIconMono,
                alpha = if (enabled) 1f else 0.4f,
            )
        }
        val (isDescEnabled, showDescEnabledTag, showFullDescEnabledTag) = when (compUiType) {
            is MarkedCompUiType.MergingAppComp -> {
                val compList = compUiType.comp.components
                val isDescEnabled = compList[0].isEnabled
                val showDescEnabledTag = enabled && compList[0].isEnabled.not()
                val showFullDescEnabledTag = enabled && compList.distinctBy { it.isEnabled }.size > 1
                Triple(isDescEnabled, showDescEnabledTag, showFullDescEnabledTag)
            }
            else -> Triple(enabled, false, false)
        }
        val compNameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.8f else 0.5f)
        val compItemColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDescEnabled) 0.9f else 0.65f)
        when (compUiType) {
            is MarkedCompUiType.AppComp, is MarkedCompUiType.MergingAppComp -> {
                AppCompLabel(
                    modifier = Modifier.layoutId("label"),
                    label = item.markedLabel,
                    labelColor = compNameColor,
                    descColor = compItemColor,
                    desc = item.comp as ValueComponent.AppComp,
                    descMaxLines = 2,
                    enabled = enabled,
                    showDescEnabledTag = showDescEnabledTag,
                )
            }
            else -> {
                CompLabel(
                    modifier = Modifier.layoutId("label"),
                    label = item.markedLabel,
                    labelColor = compNameColor,
                    descColor = compItemColor,
                    desc = item.comp.value,
                    descMaxLines = 2,
                )
            }
        }
        val mergingSize = when (compUiType) {
            is MarkedCompUiType.MergingSimple -> compUiType.comp.components.size
            is MarkedCompUiType.MergingAppComp -> compUiType.comp.components.size
            is MarkedCompUiType.MergingNativeLib -> compUiType.comp.components.size
            else -> -1
        }
        if (mergingSize >= 0) {
            CompAction(
                modifier = Modifier.layoutId("expandIcon").padding(start = 8.dp),
                label = mergingSize.toString(),
                icon = if (showFullValues) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                alpha = if (enabled) 1f else 0.5f,
            )
            when (compUiType) {
                is MarkedCompUiType.MergingSimple -> {
                    Text(
                        modifier = Modifier.layoutId("fullValues"),
                        text = itemValuesString.orEmpty(),
                        color = compItemColor,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                    )
                }
                is MarkedCompUiType.MergingAppComp -> {
                    CompFullDesc(
                        modifier = Modifier.layoutId("fullValues"),
                        desc = itemValues.orEmpty(),
                        normalColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        showDescEnabledTag = showFullDescEnabledTag,
                    )
                }
                else -> Unit
            }
        }
    }
}

// ComponentUiCategory
private enum class CompUiCat { Simple, AppComp, NativeLib }
private sealed class MarkedCompUiType(val cat: CompUiCat) {
    class Simple(val comp: MarkedValueComp<ValueComponent.Simple>) : MarkedCompUiType(CompUiCat.Simple)
    class AppComp(val comp: MarkedValueComp<ValueComponent.AppComp>) : MarkedCompUiType(CompUiCat.AppComp)
    class NativeLib(val comp: MarkedValueComp<ValueComponent.NativeLib>) : MarkedCompUiType(CompUiCat.NativeLib)
    class MergingSimple(val comp: MarkedMergingComp<ValueComponent.Simple>) : MarkedCompUiType(CompUiCat.Simple)
    class MergingAppComp(val comp: MarkedMergingComp<ValueComponent.AppComp>) : MarkedCompUiType(CompUiCat.AppComp)
    class MergingNativeLib(val comp: MarkedMergingComp<ValueComponent.NativeLib>) : MarkedCompUiType(CompUiCat.NativeLib)
}
private val MarkedCompUiType.isMerging: Boolean
    get() = when (this) {
        is MarkedCompUiType.MergingNativeLib -> true
        is MarkedCompUiType.MergingAppComp -> true
        is MarkedCompUiType.MergingSimple -> true
        is MarkedCompUiType.NativeLib -> false
        is MarkedCompUiType.AppComp -> false
        is MarkedCompUiType.Simple -> false
    }

private fun MarkedComponent.getUiType(): MarkedCompUiType {
    val item = this
    return if (item is MarkedMergingComp<*>) {
        when (item.comp) {
            is ValueComponent.NativeLib -> {
                val compList = item.castComponents<ValueComponent.NativeLib>()
                MarkedCompUiType.MergingNativeLib(MarkedMergingComp(item, compList))
            }
            is ValueComponent.SharedLib -> {
                error("unreachable")
            }
            is ValueComponent.AppComp -> {
                val compList = item.castComponents<ValueComponent.AppComp>()
                MarkedCompUiType.MergingAppComp(MarkedMergingComp(item, compList))
            }
            is ValueComponent.Simple -> {
                val compList = item.castComponents<ValueComponent.Simple>()
                MarkedCompUiType.MergingSimple(MarkedMergingComp(item, compList))
            }
        }
    } else {
        when (val valueComp = item.comp) {
            is ValueComponent.NativeLib ->
                MarkedCompUiType.NativeLib(MarkedValueComp(item, valueComp))
            is ValueComponent.SharedLib ->
                error("unreachable")
            is ValueComponent.AppComp ->
                MarkedCompUiType.AppComp(MarkedValueComp(item, valueComp))
            is ValueComponent.Simple ->
                MarkedCompUiType.Simple(MarkedValueComp(item, valueComp))
        }
    }
}

private fun ValueComponent.NativeLib.getDesc(context: Context, isCompressedSize: Boolean): String {
    return entries.joinToString(separator = "\n") { (libName, values) ->
        val (entry, compressedSize, size) = values
        val variant = entry.replace("""/?\Q$libName\E""".toRegex(), "")
        val preferredSize = if (isCompressedSize) compressedSize else size
        val s = Formatter.formatFileSize(context, preferredSize)
        "$variant • $s"
    }
}

@Composable
private fun CompIcon(modifier: Modifier = Modifier, iconId: Int, isMono: Boolean, alpha: Float = 1.0f) {
    val isDark = mainApplication.isDarkTheme
    val accentGreenColor = if (isDark) 0xFF293742.toInt() else 0xFF3DDC84.toInt()
    val borderColor = Color(ColorUtil.lightenOrDarken(accentGreenColor, 0.35f))
    Box(
        modifier = modifier
            .border(width = 0.4.dp, color = borderColor.copy(alpha = 0.25f * alpha), shape = CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isMono) {
            Icon(
                modifier = Modifier.width(18.dp).heightIn(max = 18.dp),
                painter = painterResource(iconId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f * alpha),
            )
        } else {
            AsyncImage(
                modifier = Modifier.width(18.dp).heightIn(max = 18.dp),
                model = iconId,
                contentDescription = null,
                alpha = alpha,
            )
        }
    }
}

@Composable
fun CompLabel(modifier: Modifier = Modifier, label: String, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) {
    Text(
        modifier = modifier,
        text = label,
        color = color,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
}

@Composable
private fun CompLabel(modifier: Modifier = Modifier, label: String, labelColor: Color, desc: String, descColor: Color, descMaxLines: Int) {
    Column(modifier = modifier) {
        CompLabel(label = label, color = labelColor)
        Text(
            text = desc,
            color = descColor,
            fontSize = 8.sp,
            lineHeight = 10.sp,
            maxLines = descMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NativeLibCompEntry(modifier: Modifier = Modifier, label: String, desc: String) {
    Column(
        modifier = modifier
            .clip(AbsoluteSmoothCornerShape(cornerRadius = 4.dp, smoothnessAsPercent = 60))
            .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 3.dp, vertical = 1.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            lineHeight = 8.sp,
        )
        Text(
            text = desc,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontSize = 8.sp,
            lineHeight = 9.sp,
        )
    }
}

@Composable
private fun NativeLibNormalCompEntry(modifier: Modifier = Modifier, label: String, desc: String) {
    Column(modifier = modifier.padding(vertical = 1.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            lineHeight = 8.sp,
        )
        Text(
            text = desc,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontSize = 8.sp,
            lineHeight = 9.sp,
        )
    }
}

@Composable
private fun CompAction(modifier: Modifier = Modifier, label: String, icon: ImageVector, alpha: Float = 1f) {
    val backColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * alpha)
        .compositeOver(MaterialTheme.colorScheme.surface)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color = backColor)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f * alpha),
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            lineHeight = 9.sp,
        )
        Spacer(modifier = Modifier.width(1.dp))
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f * alpha),
        )
    }
}

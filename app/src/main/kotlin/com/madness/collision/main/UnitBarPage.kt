/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.R
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.settings.SettingsFragment
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.Description
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.ceil

@Composable
fun UnitBarPage(modifier: Modifier = Modifier, mainViewModel: MainViewModel, width: Dp) {
    val context = LocalContext.current
    val descriptions = remember {
        val pinnedUnits = DescRetriever(context).includePinState().doFilter()
            .retrieveInstalled().run { mapTo(HashSet(size)) { it.unitName } }
        val (pinned, frequent) = getFrequentUnits(context).partition { it.unitName in pinnedUnits }
        // exclude api viewing, which is set as home page
        (pinned + frequent).filterNot { desc -> desc.unitName == Unit.UNIT_NAME_API_VIEWING }
    }
    val scope = rememberCoroutineScope()
    Updates(width, descriptions, modifier) { unitName ->
        if (unitName == "app_settings") {
            mainViewModel.displayFragment(SettingsFragment())
        } else {
            mainViewModel.displayUnit(unitName)
            scope.launch(Dispatchers.Default) {
                Unit.increaseFrequency(context, unitName)
            }
        }
    }
}

private fun getFrequentUnits(context: Context): List<Description> {
    val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
    val frequencies = Unit.getFrequencies(context, pref)
    val allUnits = Unit.getSortedUnitNamesByFrequency(context, frequencies = frequencies)
    val disabledUnits = Unit.getDisabledUnits(context, pref)
    return allUnits.mapNotNull {
        if (disabledUnits.contains(it)) return@mapNotNull null
        Unit.getDescription(it)?.takeIf { d -> d.isAvailable(context) }
    }
}

@Composable
private fun Updates(
    maxWidth: Dp,
    descriptions: List<Description>,
    modifier: Modifier = Modifier,
    onClick: (unitName: String) -> kotlin.Unit
) {
    // layout like LazyVerticalGrid but with fixed height
    val (minItemSize, horizontalItemMargin) = when {
        maxWidth < 316.dp -> 80.dp to 5.dp
        maxWidth < 375.dp -> 100.dp to 9.dp
        maxWidth < 600.dp -> 110.dp to 9.dp
        maxWidth < 900.dp -> 110.dp to 9.dp
        else -> 125.dp to 9.dp
    }
    val expectedBoxPadding = 17.dp
    val actualBoxPadding = expectedBoxPadding - horizontalItemMargin
    val itemsLayoutWidth = maxWidth - actualBoxPadding * 2
    val maxColumnCount = (itemsLayoutWidth / minItemSize).toInt().coerceAtLeast(1)
    val rowSize = ceil(descriptions.size / maxColumnCount.toFloat()).toInt()
    Column(modifier = modifier.padding(horizontal = actualBoxPadding)) {
        for (i in 0..<rowSize) {
            Row {
                for (j in 0..<maxColumnCount) {
                    val desc = descriptions.getOrNull(i * maxColumnCount + j)
                    if (desc != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            UnitItem(desc, horizontalItemMargin, onClick)
                        }
                    } else if (i * maxColumnCount + j == descriptions.size) {
                        // hardcoded settings item
                        Box(modifier = Modifier.weight(1f)) {
                            SettingsItem(
                                horizontalMargin = horizontalItemMargin,
                                onClick = { onClick("app_settings") }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(horizontalMargin: Dp, onClick: () -> kotlin.Unit) {
    val itemColor = when {
        LocalInspectionMode.current -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
        else -> Color(ThemeUtil.getColor(LocalContext.current, R.attr.colorAItem))
    }
    UnitItem(
        label = stringResource(R.string.Main_ToolBar_title_Settings),
        icon = {
            Icon(
                imageVector = Icons.TwoTone.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        backgroundColor = itemColor,
        horizontalMargin = horizontalMargin,
        onClick = onClick,
    )
}

@Composable
private fun UnitItem(desc: Description, horizontalMargin: Dp, onClick: (unitName: String) -> kotlin.Unit) {
    val itemColor = when {
        LocalInspectionMode.current -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
        else -> Color(ThemeUtil.getColor(LocalContext.current, R.attr.colorAItem))
    }
    UnitItem(
        label = stringResource(id = desc.nameResId),
        icon = {
            Icon(
                painter = painterResource(id = desc.iconResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        backgroundColor = itemColor,
        horizontalMargin = horizontalMargin,
        onClick = { onClick(desc.unitName) },
    )
}

@Composable
private fun UnitItem(
    label: String,
    icon: @Composable () -> kotlin.Unit,
    backgroundColor: Color,
    horizontalMargin: Dp,
    onClick: () -> kotlin.Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin)
                .clip(AbsoluteSmoothCornerShape(20.dp, 100))
                .background(backgroundColor)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
            content = { icon() },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
        )
    }
}

@Composable
@PreviewCombinedColorLayout
private fun UnitBarPreview() {
    val descriptions = remember {
        arrayOf(
            R.string.apiViewer to R.drawable.ic_android_24,
            R.string.unit_audio_timer to R.drawable.ic_timer_24,
            R.string.unit_device_manager to R.drawable.ic_devices_other_24,
            R.string.twService to R.drawable.ic_image_24,
        ).map { (nameResId, iconResId) ->
            Description("", nameResId, iconResId)
        }
    }
    BoundoTheme {
        Surface {
            BoxWithConstraints {
                Updates(maxWidth, descriptions) { }
            }
        }
    }
}

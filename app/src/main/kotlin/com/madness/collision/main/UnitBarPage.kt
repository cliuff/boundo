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
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.R
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.Description
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.ceil

class UnitBarFragment : ComposeFragment() {
    override val category: String = "UnitBar"
    override val id: String = "UnitBar"

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            UnitBarPage(mainViewModel)
        }
    }
}

@Composable
fun UnitBarPage(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val descriptions = remember {
        val pinnedUnits = DescRetriever(context).includePinState().doFilter()
            .retrieveInstalled().run { mapTo(HashSet(size)) { it.unitName } }
        val (pinned, frequent) = getFrequentUnits(context).partition { it.unitName in pinnedUnits }
        pinned + frequent
    }
    val scope = rememberCoroutineScope()
    Updates(descriptions) {
        mainViewModel.displayUnit(it)
        scope.launch(Dispatchers.Default) {
            Unit.increaseFrequency(context, it)
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
private fun Updates(descriptions: List<Description>, onClick: (unitName: String) -> kotlin.Unit) {
    val itemColor = when {
        LocalInspectionMode.current -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
        else -> Color(ThemeUtil.getColor(LocalContext.current, R.attr.colorAItem))
    }
    // layout like LazyVerticalGrid but with fixed height
    BoxWithConstraints(modifier = Modifier.padding(horizontal = 8.dp)) {
        val minItemSize = when {
            maxWidth < 360.dp -> 80.dp
            maxWidth < 600.dp -> 100.dp
            maxWidth < 900.dp -> 110.dp
            else -> 120.dp
        }
        val maxColumnCount = (maxWidth / minItemSize).toInt().coerceAtLeast(1)
        val rowSize = ceil(descriptions.size / maxColumnCount.toFloat()).toInt()
        Column {
            for (i in 0..<rowSize) {
                Row {
                    for (j in 0..<maxColumnCount) {
                        val desc = descriptions.getOrNull(i * maxColumnCount + j)
                        Box(modifier = Modifier.weight(1f)) {
                            if (desc != null) {
                                UnitItem(
                                    desc = desc,
                                    backgroundColor = itemColor,
                                    onClick = { onClick(desc.unitName) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitItem(desc: Description, backgroundColor: Color, onClick: () -> kotlin.Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp)
                .clip(AbsoluteSmoothCornerShape(20.dp, 100))
                .background(backgroundColor)
                .padding(vertical = 18.dp),
            painter = painterResource(id = desc.iconResId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = desc.nameResId),
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
private fun UpdatesPreview() {
    val descriptions = remember {
        arrayOf(
            R.string.apiViewer to R.drawable.ic_android_24,
            R.string.unit_audio_timer to R.drawable.ic_timer_24,
            R.string.unit_device_manager to R.drawable.ic_devices_other_24,
        ).map { (nameResId, iconResId) ->
            Description("", nameResId, iconResId)
        }
    }
    Updates(descriptions) { }
}

@Preview(showBackground = true)
@Composable
private fun UpdatesPagePreview() {
    MaterialTheme {
        UpdatesPreview()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpdatesPageDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        UpdatesPreview()
    }
}

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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.madness.collision.R
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.Description
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnitBarFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _composeView: ComposeView? = null
    private val composeView: ComposeView get() = _composeView!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _composeView = ComposeView(inflater.context)
        return composeView
    }

    override fun onDestroyView() {
        _composeView = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val colorScheme = if (OsUtils.satisfy(OsUtils.S)) {
            if (mainApplication.isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        composeView.setContent {
            MaterialTheme(colorScheme = colorScheme) {
                UnitBarPage(mainViewModel)
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Updates(descriptions: List<Description>, onClick: (unitName: String) -> kotlin.Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.width(8.dp))
        val context = LocalContext.current
        val itemColor = remember {
            ThemeUtil.getColor(context, R.attr.colorAItem)
        }
        descriptions.forEach { desc ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onClick(desc.unitName) }
                    .padding(horizontal = 9.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(itemColor))
                        .padding(12.dp),
                    painter = painterResource(id = desc.iconResId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.widthIn(max = 60.dp),
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

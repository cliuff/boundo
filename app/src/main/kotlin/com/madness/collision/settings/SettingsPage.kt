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

package com.madness.collision.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.main.showPage
import com.madness.collision.pref.PrefExterior
import com.madness.collision.unit.DescRetriever
import com.madness.collision.util.Page
import kotlinx.coroutines.delay

@Composable
fun SettingsPage(mainViewModel: MainViewModel, showLanguages: (context: Context) -> Unit) {
    val context = LocalContext.current
    val options = remember {
        val builtIn = listOf(
            Triple(R.string.settings_exterior, R.drawable.ic_palette_24) {
                context.showPage<Page> {
                    putString("fragmentClass", PrefExterior::class.qualifiedName)
                    putInt("titleId", R.string.settings_exterior)
                }
            },
            Triple(R.string.Settings_Button_SwitchLanguage, R.drawable.ic_language_24) {
                showLanguages(context)
            },
            Triple(R.string.Main_TextView_Advice_Text, R.drawable.ic_info_24) {
                context.showPage<AdviceFragment>()
            },
        )
        val unitOptions = getUnitOptions(mainViewModel, context)
        builtIn + unitOptions
    }
    Settings(options = options)
}

@Composable
private fun Settings(options: List<Triple<Int, Int, () -> Unit>>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            options.forEachIndexed { index, (label, icon, onClick) ->
                if (index != 0) {
                    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    Divider(color = dividerColor, thickness = 0.5.dp, startIndent = 70.dp)
                }
                SettingsItem(
                    icon = icon,
                    label = stringResource(id = label),
                    isTertiary = index == 0,
                    onClick = onClick
                )
            }
        }
    }
}

private fun getUnitOptions(mainViewModel: MainViewModel, context: Context): List<Triple<Int, Int, () -> Unit>> {
    return DescRetriever(context).retrieveInstalled().mapNotNull map@{ state ->
        val unit = state.unitName
        val unitBridge = com.madness.collision.unit.Unit.getBridge(unit) ?: return@map null  // continue
        val settingsPage = unitBridge.getSettings() ?: return@map null  // continue
        val unitDesc = state.description
        Triple(unitDesc.nameResId, unitDesc.iconResId) {
            mainViewModel.displayFragment(settingsPage)
        }
    }
}

@Composable
private fun SettingsItem(icon: Int, label: String, isTertiary: Boolean = false, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val iconTint: Color
    val iconContainerColor: Color
    if (isTertiary) {
        var iconTintA by remember(key1 = label) {
            mutableStateOf(colorScheme.primary)
        }
        var iconContainerColorA by remember(key1 = label) {
            mutableStateOf(colorScheme.primaryContainer)
        }
        LaunchedEffect(key1 = label) {
            delay(200)
            iconTintA = colorScheme.tertiary
            iconContainerColorA = colorScheme.tertiaryContainer
        }
        iconTint = animateColorAsState(targetValue = iconTintA).value
        iconContainerColor = animateColorAsState(targetValue = iconContainerColorA).value
    } else {
        iconTint = colorScheme.primary
        iconContainerColor = colorScheme.primaryContainer
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(iconContainerColor)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconTint,
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SettingsPreview() {
    val options = remember {
        listOf(
            Triple(R.string.settings_exterior, R.drawable.ic_palette_24) { },
            Triple(R.string.Settings_Button_SwitchLanguage, R.drawable.ic_language_24) { },
            Triple(R.string.Main_TextView_Advice_Text, R.drawable.ic_info_24) { },
        )
    }
    Settings(options = options)
}

@Preview(showBackground = true)
@Composable
private fun SettingsPagePreview() {
    MaterialTheme {
        SettingsPreview()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPageDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SettingsPreview()
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun SettingsPageRtlPreview() {
    MaterialTheme {
        SettingsPreview()
    }
}

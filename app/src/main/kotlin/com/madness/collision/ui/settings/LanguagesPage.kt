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

package com.madness.collision.ui.settings

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.use
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.R
import com.madness.collision.chief.layout.scaffoldWindowInsets
import com.madness.collision.ui.comp.ClassicTopAppBar
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.ui.theme.PreviewAppTheme
import com.madness.collision.util.dev.PreviewCombinedColorLayout

@Composable
fun LanguagesPage() {
    val (topBarInsets, _, contentInsets) =
        scaffoldWindowInsets(shareCutout = 12.dp, shareStatusBar = 5.dp, shareWaterfall = 8.dp)
    Scaffold(
        topBar = {
            ClassicTopAppBar(
                title = { Text(text = stringResource(R.string.Settings_Language_Dialog_Title)) },
                windowInsets = topBarInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        containerColor = MetaAppTheme.colorScheme.surfaceNeutral,
        contentWindowInsets = contentInsets,
    ) { innerPadding ->
        LanguagesContent(contentPadding = innerPadding)
    }
}

@Immutable
data class LanguagesUiState(
    val selectedLanguage: String
)

@Composable
fun LanguagesContent(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val viewModel = viewModel<LanguagesViewModel>()
    LaunchedEffect(Unit) { viewModel.init() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Languages(
        selectedValue = uiState.selectedLanguage,
        onSelectValue = viewModel::selectLanguage,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    )
}

@Composable
private fun Languages(
    selectedValue: String,
    onSelectValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val resources = LocalResources.current
    val langMap = remember(resources) { loadLanguageMap(resources) }

    LazyColumn(modifier = modifier, state = state, contentPadding = contentPadding) {
        item(key = "@lang.pad.top", contentType = "Space") {
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(
            items = langMap,
            key = { (code, _) -> code },
            contentType = { "Language" }) { (code, label) ->
            LanguageItem(
                text = label,
                selected = selectedValue == code,
                onClick = { onSelectValue(code) }
            )
        }
        item(key = "@lang.pad.bot", contentType = "Space") {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LanguageItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(modifier = Modifier.padding(start = 12.dp).padding(vertical = 5.dp), text = text)
    }
}

private fun loadLanguageMap(resources: Resources): List<Pair<String, String>> {
    val langEntries = resources.obtainTypedArray(R.array.prefSettingsLangEntries)
    val langValues = resources.obtainTypedArray(R.array.prefSettingsLangValues)
    return langEntries.use { e ->
        langValues.use { v ->
            (0..<v.length()).map { i ->
                val label = e.getString(i).orEmpty()
                val code = v.getString(i).orEmpty()
                code to label
            }
        }
    }
}

@Composable
@PreviewCombinedColorLayout
private fun LanguagesPreview() {
    PreviewAppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Languages(selectedValue = "auto", onSelectValue = {})
        }
    }
}

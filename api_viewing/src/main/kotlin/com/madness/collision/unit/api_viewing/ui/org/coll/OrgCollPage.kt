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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.main.showPage
import com.madness.collision.unit.api_viewing.ui.org.group.GroupEditorFragment
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import io.cliuff.boundo.org.model.CompColl

@Composable
fun OrgCollPage(contentPadding: PaddingValues = PaddingValues()) {
    val viewModel = viewModel<OrgCollViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (isLoading, coll) = uiState
    val context = LocalContext.current
    OrgCollScaffold(
        onClickAdd = {
            context.showPage<GroupEditorFragment>()
        },
        contentWindowInsets = WindowInsets(
            top = contentPadding.calculateTopPadding() + 10.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
            left = contentPadding.calculateLeftPadding(LocalLayoutDirection.current),
            right = contentPadding.calculateRightPadding(LocalLayoutDirection.current)),
    ) { innerPadding ->
        if (coll != null) {
            OrgCollContent(
                modifier = Modifier.fillMaxWidth(),
                coll = coll,
                contentPadding = innerPadding,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = stringResource(com.madness.collision.R.string.text_no_content),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OrgCollScaffold(
    onClickAdd: () -> Unit,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onClickAdd,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
            }
        },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

@Composable
private fun OrgCollContent(
    coll: CompColl,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item {
            CollectionName(name = coll.name)
        }
        itemsIndexed(coll.groups) { i, group ->
            CollGroup(name = group.name)
        }
    }
}

@Composable
private fun CollectionName(name: String) {
    Text(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        text = name,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 15.sp,
        lineHeight = 17.sp,
    )
}

@Composable
private fun CollGroup(name: String) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
        }
    }
}

@Composable
@PreviewCombinedColorLayout
private fun OrgCollPreview() {
    val coll = CompColl(id = 0, name = "Collection 1", groups = emptyList())
    BoundoTheme {
        OrgCollScaffold(
            onClickAdd = {},
        ) {
            OrgCollContent(
                coll = coll,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
            )
        }
    }
}

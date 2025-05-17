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

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.chief.lang.runIf
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.getFullLabel
import com.madness.collision.util.dev.PreviewCombinedColorLayout

class ListTagState(private var initTags: Map<String, Boolean> = emptyMap()) {
    private lateinit var mutStateList: SnapshotStateList<Boolean?>
    val stateList: SnapshotStateList<Boolean?> get() = mutStateList

    fun init(tagIds: List<String>) {
        mutStateList = List(tagIds.size) { i -> initTags[tagIds[i]] }.toMutableStateList()
        initTags = emptyMap()
    }
}

@Composable
fun AppListTags(tagState: ListTagState, onStateChanged: (String, Boolean?) -> Unit) {
    val context = LocalContext.current
    val tags = remember(tagState) {
        getTagEntries(context).also { tagState.init(it.map(TagsEntry::id)) }
    }
    val checkStateList = tagState.stateList
    Tags(tags = tags, checkStateList = checkStateList, onCheckChanged = { i, state ->
        checkStateList[i] = state
        onStateChanged(tags[i].id, state)
    })
}

private fun getTagEntries(context: Context): List<TagsEntry> {
    val rankedTags = AppTagManager.tags.values.sortedBy { it.rank }
    return rankedTags.map { tagInfo ->
        val label = tagInfo.getFullLabel(context)?.toString() ?: ""
        TagsEntry(tagInfo.id, label, tagInfo.icon.drawableResId)
    }
}

private class TagsEntry(val id: String, val label: String, val iconResId: Int?)

@Composable
private fun Tags(
    tags: List<TagsEntry>,
    checkStateList: SnapshotStateList<Boolean?>,
    onCheckChanged: (tagIndex: Int, newState: Boolean?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        for ((i, tag) in tags.withIndex()) {
            val checkState = checkStateList[i]
            TagItem(
                checkState = checkState,
                icon = tag.iconResId,
                label = tag.label,
                onClick = { onCheckChanged(i, if (checkState == null) true else null) },
                onLongClick = { onCheckChanged(i, checkState == false) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagItem(
    checkState: Boolean?,
    icon: Int?,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checkState != null, onCheckedChange = { onClick() })
        if (icon != null) {
            AsyncImage(
                model = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = LocalTextStyle.current
                .runIf({ checkState == false }, { copy(textDecoration = TextDecoration.LineThrough) }),
        )
    }
}

@PreviewCombinedColorLayout
@Composable
private fun TagsPreview() {
    val tags = remember {
        listOf(
            TagsEntry("", "Tag 0", null),
            TagsEntry("", "Tag 1", null),
            TagsEntry("", "Tag 2", null),
            TagsEntry("", "Tag 3", null),
        )
    }
    val checkStateList = remember { listOf(null, true, false, true).toMutableStateList() }
    BoundoTheme {
        Surface {
            Tags(tags = tags, checkStateList = checkStateList, onCheckChanged = { _, _ -> })
        }
    }
}

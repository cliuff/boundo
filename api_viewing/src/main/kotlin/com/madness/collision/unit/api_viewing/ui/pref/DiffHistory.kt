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

package com.madness.collision.unit.api_viewing.ui.pref

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.database.maintainer.DiffChange
import com.madness.collision.unit.api_viewing.database.maintainer.DiffType
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun DiffHistoryPage(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val diffRecords by produceState(mutableListOf<DiffUi>()) { value = DiffUiData.retrieve(context) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 12.dp,
            bottom = paddingValues.calculateBottomPadding() + 50.dp,
        ),
    ) {
        itemsIndexed(
            items = diffRecords,
            key = { i, item ->
                when (item) {
                    is DiffUi.Empty -> "$i.${item.record[0].id}"
                    is DiffUi.Change -> "$i.${item.record[0].id}"
                    is DiffUi.ExpandAction -> i.toString()
                }
            },
        ) { i, item ->
            when (item) {
                is DiffUi.Empty -> DiffRecord(item.record)
                is DiffUi.Change -> DiffRecord(item.record)
                is DiffUi.ExpandAction -> {
                    ExpandAction(
                        modifier = Modifier.clickable { DiffUiData.expand(diffRecords, i) },
                        action = item,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandAction(modifier: Modifier = Modifier, action: DiffUi.ExpandAction) {
    Text(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .padding(bottom = 10.dp),
        text = stringResource(R.string.av_settings_diff_minimized, action.list.size),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        fontSize = 13.sp,
        lineHeight = 15.sp,
    )
}

@Composable
private fun DiffRecord(changeList: List<DiffChange>) {
    val idChange = changeList[0]
    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Column() {
            val recordTime = DateUtils.getRelativeTimeSpanString(idChange.diff.timeMills,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
            Row() {
                Text(
                    text = idChange.diff.id,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = recordTime,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (idChange.isNotNone) {
                val pkgChanges = changeList.groupBy { it.diff.packageName }
                for ((_, list) in pkgChanges) {
                    ChangeItem(list)
                }
            }
        }
    }
}

@Composable
private fun ChangeItem(changeList: List<DiffChange>) {
    val idChange = changeList[0]
    val diffInfo = idChange.diff
    val pkg = diffInfo.packageName
    val message = when (idChange.type) {
        DiffType.None -> "="
        DiffType.Add -> "+ $pkg"
        DiffType.Remove -> "- $pkg"
        DiffType.Change -> "* $pkg"
        else -> "? $pkg"
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontSize = 12.sp,
            lineHeight = 13.sp,
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth()) {
            for (i in changeList.indices) {
                val diffChange = changeList[i]
                if (diffChange.type == DiffType.Change) {
                    if (i > 0) Spacer(modifier = Modifier.width(5.dp))
                    ColumnValueChange(diffChange)
                }
            }
        }
    }
}

@Composable
private fun ColumnValueChange(change: DiffChange) {
    Column(
        modifier = Modifier
            .clip(AbsoluteSmoothCornerShape(6.dp, 60))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 2.dp),
    ) {
        Text(
            text = change.columnName,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = change.run { "$oldValue -> $newValue" },
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}
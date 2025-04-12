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

package com.madness.collision.unit.api_viewing.ui.org.coll

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun DropdownMenuDeleteItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        modifier = modifier,
        text = {
            Text(
                // extra padding for optical balance
                modifier = Modifier.padding(end = 10.dp),
                text = text,
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
            )
        },
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error,
            leadingIconColor = MaterialTheme.colorScheme.error,
        ),
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AbsoluteSmoothCornerShape(28.dp, 70),
        title = { Text(text = title) },
        text = { Text(text = "Are you sure to delete?\nThis cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error),
                content = { Text(text = "Delete") },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(com.madness.collision.R.string.text_cancel))
            }
        },
    )
}

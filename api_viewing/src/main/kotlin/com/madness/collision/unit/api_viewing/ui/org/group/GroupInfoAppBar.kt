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

package com.madness.collision.unit.api_viewing.ui.org.group

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.madness.collision.unit.api_viewing.ui.org.coll.DeleteConfirmationDialog
import com.madness.collision.unit.api_viewing.ui.org.coll.DropdownMenuDeleteItem

@Composable
fun GroupInfoOverflowIconButton(
    onActionDelete: () -> Unit,
    iconColor: Color,
) {
    var showDelDialog by remember { mutableStateOf(false) }
    if (showDelDialog) {
        DeleteConfirmationDialog(
            title = "Delete Group",
            onConfirm = onActionDelete,
            onDismiss = { showDelDialog = false },
        )
    }

    var showOptions by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showOptions = true },
        colors = IconButtonDefaults.iconButtonColors(contentColor = iconColor),
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = null,
        )
    }

    DropdownMenu(
        modifier = Modifier.widthIn(max = 260.dp),
        expanded = showOptions,
        onDismissRequest = { showOptions = false },
        shape = RoundedCornerShape(10.dp),
    ) {
        DropdownMenuDeleteItem(
            modifier = Modifier.widthIn(min = 160.dp),
            text = "Delete group",
            onClick = {
                showDelDialog = true
                showOptions = false
            },
        )
    }
}

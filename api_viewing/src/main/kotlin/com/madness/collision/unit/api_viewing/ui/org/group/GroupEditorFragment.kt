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

package com.madness.collision.unit.api_viewing.ui.org.group

import android.os.Bundle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme

class GroupEditorFragment : ComposeFragment() {
    companion object {
        /** Group ID to modify, a string formatted as "collId:groupId". */
        const val ARG_COLL_GROUP_ID: String = "ArgCollGroupId"
    }

    private var collGroupId: Pair<Int, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val argId = arguments?.getString(ARG_COLL_GROUP_ID)
        val match = argId?.let("""(\d+):(\d+)""".toRegex()::matchEntire)
        collGroupId = match?.run { groupValues[1].toInt() to groupValues[2].toInt() }
    }

    @Composable
    override fun ComposeContent() {
        val (collId, groupId) = collGroupId ?: (-1 to -1)
        MaterialTheme(colorScheme = rememberColorScheme()) {
            GroupEditorPage(modCollId = collId, modGroupId = groupId)
        }
    }
}
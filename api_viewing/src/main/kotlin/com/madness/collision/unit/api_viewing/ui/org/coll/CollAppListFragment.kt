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

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.core.os.BundleCompat
import com.madness.collision.Democratic
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import io.cliuff.boundo.org.model.CompColl

class CollAppListFragment : ComposeFragment(), Democratic {
    companion object {
        /** An instance of [CompColl] to view. */
        const val ARG_COLL: String = "ArgColl"
        /** Group ID to modify, a string formatted as "collId:groupId". */
        const val ARG_COLL_GROUP_ID: String = "ArgCollGroupId"
    }

    private var coll: CompColl? = null
    private var collGroupId: Pair<Int, Int>? = null

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.title = coll?.name.orEmpty()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coll = arguments?.let { args -> BundleCompat.getParcelable(args, ARG_COLL, CompColl::class.java) }
        val argId = arguments?.getString(ARG_COLL_GROUP_ID)
        val match = argId?.let("""(\d+):(\d+)""".toRegex()::matchEntire)
        collGroupId = match?.run { groupValues[1].toInt() to groupValues[2].toInt() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
    }

    @Composable
    override fun ComposeContent() {
        val (collId, groupId) = collGroupId ?: (-1 to -1)
        MaterialTheme(colorScheme = rememberColorScheme()) {
            CollAppListPage(
                coll = coll,
                modCollId = collId,
                modGroupId = groupId,
                contentPadding = rememberContentPadding(),
            )
        }
    }
}
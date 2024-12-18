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

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import com.madness.collision.Democratic
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.main.showPage
import com.madness.collision.unit.api_viewing.R
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class GroupInfoFragment : ComposeFragment(), Democratic {
    companion object {
        /** An instance of [OrgGroup] to view. */
        const val ARG_COLL_GROUP: String = "ArgCollGroup"
        /** Group ID to modify, a string formatted as "collId:groupId". */
        const val ARG_COLL_GROUP_ID: String = "ArgCollGroupId"
    }

    private var collGroup: OrgGroup? = null
    private var collGroupId: Pair<Int, Int>? = null
    private val viewModel: GroupEditorViewModel by viewModels()

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.title = collGroup?.name.orEmpty()
        inflateAndTint(R.menu.toolbar_org_group, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.orgGroupActionUpdate -> {
                context?.showPage<GroupEditorFragment> {
                    val (collId, groupId) = collGroupId ?: (-1 to -1)
                    putString(GroupEditorFragment.ARG_COLL_GROUP_ID, "$collId:$groupId")
                }
                return true
            }
            R.id.orgGroupActionDelete -> {
                collGroup?.let(viewModel::remove)
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collGroup = arguments?.let { args -> BundleCompat.getParcelable(args, ARG_COLL_GROUP, OrgGroup::class.java) }
        val argId = arguments?.getString(ARG_COLL_GROUP_ID)
        val match = argId?.let("""(\d+):(\d+)""".toRegex()::matchEntire)
        collGroupId = match?.run { groupValues[1].toInt() to groupValues[2].toInt() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)

        val viewLifecycle = viewLifecycleOwner.lifecycle
        viewModel.uiState
            .flowWithLifecycle(viewLifecycle)
            .filter { state -> state.isSubmitOk }
            .onEach { activity?.onBackPressedDispatcher?.onBackPressed() }
            .launchIn(viewLifecycle.coroutineScope)
    }

    @Composable
    override fun ComposeContent() {
        val (collId, groupId) = collGroupId ?: (-1 to -1)
        MaterialTheme(colorScheme = rememberColorScheme()) {
            GroupInfoPage(
                group = collGroup,
                modCollId = collId,
                modGroupId = groupId,
                contentPadding = rememberContentPadding(),
            )
        }
    }
}
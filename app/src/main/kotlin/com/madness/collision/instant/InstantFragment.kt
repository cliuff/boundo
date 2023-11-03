/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.instant

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.ActivityInstantManagerBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Unit
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.alterPadding
import com.madness.collision.util.os.OsUtils

internal class InstantFragment: TaggedFragment(), Democratic {

    override val category: String = "Instant"
    override val id: String = "Instant"
    
    companion object {
        @JvmStatic
        fun newInstance() = InstantFragment()
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var viewBinding: ActivityInstantManagerBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.Main_TextView_Launcher)
        inflateAndTint(R.menu.toolbar_instant, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.instantTBManual -> {
                val context = context ?: return false
                CollisionDialog.alert(context, R.string.instantManual).show()
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = ActivityInstantManagerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.instantContainer.alterPadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            viewBinding.instantContainer.alterPadding(bottom = asBottomMargin(it))
        }

        val installedUnits = Unit.getInstalledUnits(context)
        val predicate: (InstantItem) -> Boolean = {
            (!it.hasRequiredUnit || installedUnits.contains(it.requiredUnitName)) && it.isAvailable(context)
        }

        kotlin.run {
            val items = if (OsUtils.satisfy(OsUtils.N)) BuiltInItems.Tiles.filter(predicate) else emptyList()
            val views = viewBinding.instantRecyclerTile to viewBinding.instantIntroTile
            views.load(InstantAdapter.TYPE_TILE, items, context)
        }
        kotlin.run {
            val items = BuiltInItems.Shortcuts.filter(predicate)
            val views = viewBinding.instantRecyclerShortcut to viewBinding.instantIntroShortcut
            views.load(InstantAdapter.TYPE_SHORTCUT, items, context)
        }
        kotlin.run {
            val items = BuiltInItems.Others
                .mapNotNull { (api, comp) -> if (OsUtils.satisfy(api)) comp() else null }
                .filter(predicate)
            val views = viewBinding.instantRecyclerOther to viewBinding.instantIntroOther
            views.load(InstantAdapter.TYPE_OTHER, items, context)
        }
    }

    private fun Pair<RecyclerView, View>.load(type: Int, items: List<InstantItem>, context: Context) {
        val (recyclerView, titleView) = this
        if (items.isNotEmpty()) {
            recyclerView.bind(InstantAdapter(context, mainViewModel, type, items))
        } else {
            recyclerView.visibility = View.GONE
            titleView.visibility = View.GONE
        }
    }

    private fun RecyclerView.bind(instantAdapter: InstantAdapter<*>) {
        instantAdapter.resolveSpanCount(this@InstantFragment, 290f)
        setHasFixedSize(true)
        setItemViewCacheSize(instantAdapter.itemCount)
        layoutManager = instantAdapter.suggestLayoutManager()
        adapter = instantAdapter
    }
}

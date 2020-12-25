/*
 * Copyright 2020 Clifford Liu
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
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.ActivityInstantManagerBinding
import com.madness.collision.instant.other.InstantOthers
import com.madness.collision.instant.shortcut.InstantShortcuts
import com.madness.collision.instant.tile.InstantTiles
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding

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
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        viewBinding = ActivityInstantManagerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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
        // qs tiles
        val availableTiles by lazy { InstantTiles.TILES.filter(predicate) }
        if (X.aboveOn(X.N) && availableTiles.isNotEmpty()) {
            val adapterTile = InstantAdapter(context,
                    mainViewModel, InstantAdapter.TYPE_TILE, availableTiles)
            adapterTile.resolveSpanCount(this, 400f)
            viewBinding.instantRecyclerTile.run {
                setHasFixedSize(true)
                setItemViewCacheSize(availableTiles.size)
                layoutManager = adapterTile.suggestLayoutManager()
                adapter = adapterTile
            }
        } else {
            viewBinding.instantRecyclerTile.visibility = View.GONE
            viewBinding.instantIntroTile.visibility = View.GONE
        }
        // shortcuts
        val availableShortcuts = InstantShortcuts.SHORTCUTS.filter(predicate)
        if (availableShortcuts.isNotEmpty()) {
            val adapterShortcut = InstantAdapter(context, mainViewModel,
                    InstantAdapter.TYPE_SHORTCUT, availableShortcuts)
            adapterShortcut.resolveSpanCount(this, 400f)
            viewBinding.instantRecyclerShortcut.run {
                setHasFixedSize(true)
                setItemViewCacheSize(availableShortcuts.size)
                layoutManager = adapterShortcut.suggestLayoutManager()
                adapter = adapterShortcut
            }
        } else {
            viewBinding.instantRecyclerShortcut.visibility = View.GONE
            viewBinding.instantIntroShortcut.visibility = View.GONE
        }
        // other
        val availableOther = InstantOthers.OTHERS.filter {
            X.aboveOn(it.first)
        }.map { it.second.invoke() }.filter(predicate)
        if (availableOther.isNotEmpty()) {
            val adapterOther = InstantAdapter(context,
                    mainViewModel, InstantAdapter.TYPE_OTHER, availableOther)
            adapterOther.resolveSpanCount(this, 400f)
            viewBinding.instantRecyclerOther.run {
                setHasFixedSize(true)
                setItemViewCacheSize(availableOther.size)
                layoutManager = adapterOther.suggestLayoutManager()
                adapter = adapterOther
            }
        } else {
            viewBinding.instantRecyclerOther.visibility = View.GONE
            viewBinding.instantIntroOther.visibility = View.GONE
        }
    }

}

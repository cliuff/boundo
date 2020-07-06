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

import android.annotation.TargetApi
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.instant.other.InstantOthers
import com.madness.collision.instant.shortcut.InstantShortcuts
import com.madness.collision.instant.tile.InstantTiles
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.activity_instant_manager.*
import kotlin.math.roundToInt

@TargetApi(X.N)
internal class InstantFragment: TaggedFragment(), Democratic {

    override val category: String = "Instant"
    override val id: String = "Instant"
    
    companion object {
        @JvmStatic
        fun newInstance() = InstantFragment()
    }

    private val mainViewModel: MainViewModel by activityViewModels()

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(R.layout.activity_instant_manager, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            instantContainer.alterPadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            instantContainer.alterPadding(bottom = it)
        }

        val itemWidth = X.size(context, 400f, X.DP)
        val spanCount = (availableWidth / itemWidth).roundToInt().run {
            if (this < 2) 1 else this
        }

        val installedUnits = SplitInstallManagerFactory.create(context).installedModules
        val predicate: (InstantItem) -> Boolean = {
            (!it.hasRequiredUnit || installedUnits.contains(it.requiredUnitName)) && it.isAvailable(context)
        }
        // qs tiles
        val availableTiles = InstantTiles.TILES.filter(predicate)
        val adapterTile = InstantAdapter(context, mainViewModel, InstantAdapter.TYPE_TILE, availableTiles)
        instantRecyclerTile.run {
            setHasFixedSize(true)
            setItemViewCacheSize(availableTiles.size)
            layoutManager = if (spanCount == 1) LinearLayoutManager(context) else GridLayoutManager(context, spanCount)
            adapter = adapterTile
        }
        // shortcuts
        if (X.belowOff(X.N_MR1)) {
            instantRecyclerShortcut.visibility = View.GONE
            instantIntroShortcut.visibility = View.GONE
            return
        }
        val availableShortcuts = InstantShortcuts.SHORTCUTS.filter(predicate)
        val adapterShortcut = InstantAdapter(context, mainViewModel, InstantAdapter.TYPE_SHORTCUT, availableShortcuts)
        instantRecyclerShortcut.run {
            setHasFixedSize(true)
            setItemViewCacheSize(availableShortcuts.size)
            layoutManager = if (spanCount == 1) LinearLayoutManager(context) else GridLayoutManager(context, spanCount)
            adapter = adapterShortcut
        }
        // other
        val availableOther = InstantOthers.OTHERS.filter(predicate)
        val adapterOther = InstantAdapter(context, mainViewModel, InstantAdapter.TYPE_OTHER, availableOther)
        instantRecyclerOther.run {
            setHasFixedSize(true)
            setItemViewCacheSize(availableOther.size)
            layoutManager = if (spanCount == 1) LinearLayoutManager(context) else GridLayoutManager(context, spanCount)
            adapter = adapterOther
        }
    }

}

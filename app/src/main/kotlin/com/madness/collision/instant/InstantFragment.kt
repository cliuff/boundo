package com.madness.collision.instant

import android.annotation.TargetApi
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.instant.shortcut.InstantShortcuts
import com.madness.collision.instant.tile.InstantTiles
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import kotlinx.android.synthetic.main.activity_instant_manager.*

@TargetApi(X.N)
internal class InstantFragment: Fragment(), Democratic {
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
        val installedUnits = SplitInstallManagerFactory.create(context).installedModules
        val predicate: (InstantItem) -> Boolean = {
            (!it.hasRequiredUnit || installedUnits.contains(it.requiredUnitName)) && it.isAvailable(context)
        }
        val availableTiles = InstantTiles.TILES.filter(predicate)
        val adapterTile = InstantAdapter(context, mainViewModel, InstantAdapter.TYPE_TILE, availableTiles)
        instantRecyclerTile.setHasFixedSize(true)
        instantRecyclerTile.setItemViewCacheSize(availableTiles.size)
        instantRecyclerTile.layoutManager = LinearLayoutManager(context)
        instantRecyclerTile.adapter = adapterTile
        if (X.belowOff(X.N_MR1)) {
            instantRecyclerShortcut.visibility = View.GONE
            instantIntroShortcut.visibility = View.GONE
            return
        }
        val availableShortcuts = InstantShortcuts.SHORTCUTS.filter(predicate)
        val adapterShortcut = InstantAdapter(context, mainViewModel, InstantAdapter.TYPE_SHORTCUT, availableShortcuts)
        instantRecyclerShortcut.setHasFixedSize(true)
        instantRecyclerShortcut.setItemViewCacheSize(availableShortcuts.size)
        instantRecyclerShortcut.layoutManager = LinearLayoutManager(context)
        instantRecyclerShortcut.adapter = adapterShortcut
    }

}

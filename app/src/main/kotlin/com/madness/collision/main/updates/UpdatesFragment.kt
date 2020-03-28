package com.madness.collision.main.updates

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.ensureAdded
import kotlinx.android.synthetic.main.fragment_updates.*
import kotlin.math.roundToInt

internal class UpdatesFragment : Fragment(), Democratic {

    private lateinit var mContext: Context
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var updatesProviders: List<Pair<String, UpdatesProvider>>
    private lateinit var fragments: List<Pair<String, Fragment>>

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.main_updates)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        updatesProviders = Unit.getPinnedUnits(mContext).mapNotNull {
            Unit.getUpdates(it)?.run { it to this }
        }
        fragments = updatesProviders.mapNotNull {
            it.second.getFragment()?.run { it.first to this }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_updates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        for ((unitName, f) in fragments) {
            ensureAdded(R.id.updatesContainer, f)
            val description = Unit.getDescription(unitName) ?: continue
            updatesPinned.setCompoundDrawablesRelativeWithIntrinsicBounds(description.getIcon(mContext), null, null, null)
            updatesPinned.text = description.getName(mContext)
            updatesPinnedContainer.setOnClickListener {
                mainViewModel.displayUnit(unitName, shouldShowNavAfterBack = true)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize(mainViewModel)
        val extra = X.size(mContext, 5f, X.DP).roundToInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            updatesContainer.alterPadding(top = it + extra)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            updatesContainer.alterPadding(bottom = it + extra)
        }
    }

}

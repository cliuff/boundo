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

package com.madness.collision.main.updates

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.MainUpdatesHeaderBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.ensureAdded
import kotlinx.android.synthetic.main.fragment_updates.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal class UpdatesFragment : TaggedFragment(), Democratic {

    companion object {
        const val ARG_MODE = "mode"
        const val MODE_NORMAL = 0
        const val MODE_NO_UPDATES = 1

        @JvmStatic
        fun newInstance(mode: Int) : UpdatesFragment {
            val b = Bundle().apply {
                putInt(ARG_MODE, mode)
            }
            return UpdatesFragment().apply { arguments = b }
        }
    }

    override val category: String = "MainUpdates"
    override val id: String = "Updates"

    private lateinit var mContext: Context
    private val mainViewModel: MainViewModel by activityViewModels()
    private var updatesProviders: MutableList<Pair<String, UpdatesProvider>> = mutableListOf()
    private var fragments: MutableList<Pair<String, Fragment>> = mutableListOf()
    private var _fixedChildCount: Int = 0
    private val fixedChildCount: Int
    get() = _fixedChildCount
    private var mode = MODE_NORMAL
    private val isNoUpdatesMode: Boolean
        get() = mode == MODE_NO_UPDATES

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.main_updates)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        mode = arguments?.getInt(ARG_MODE) ?: MODE_NORMAL
        if (isNoUpdatesMode) return
        updatesProviders = Unit.getPinnedUnits(mContext).mapNotNull {
            Unit.getUpdates(it)?.run { it to this }
        }.toMutableList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_updates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _fixedChildCount = mainUpdatesContainer.childCount
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize(mainViewModel)
        val extra = X.size(mContext, 5f, X.DP).roundToInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            mainUpdatesContainer.alterPadding(top = it + extra)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            mainUpdatesContainer.alterPadding(bottom = it + extra)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        loadUpdates()
    }

    /**
     * Remove all the updates, especially the fragments from fragment manager
     */
    private fun clearUpdates() {
        if (fragments.isEmpty()) return
        val transaction = childFragmentManager.beginTransaction()
        fragments.forEach {
            transaction.remove(it.second)
        }
        transaction.commitNowAllowingStateLoss()
        // clear headers
        if (mainUpdatesContainer.childCount > fixedChildCount) {
            mainUpdatesContainer.removeViews(fixedChildCount, mainUpdatesContainer.childCount - fixedChildCount)
        }
    }

    private fun loadUpdates() {
        // clear old updates
        clearUpdates()
        // load latest updates
        fragments = updatesProviders.mapNotNull {
            if (it.second.hasUpdates(this)) it.second.getFragment()?.run {
                it.first to this
            } else null
        }.toMutableList()
        mainUpdatesSecUpdates.visibility = if (fragments.isEmpty()) View.GONE else View.VISIBLE
        if (fragments.isEmpty()) return
        for ((_, f) in fragments) {
            ensureAdded(R.id.mainUpdatesContainer, f, true)
        }
        lifecycleScope.launch {
            delay(100)
            val inflater = LayoutInflater.from(context)
            launch(Dispatchers.Main) {
                for (i in fragments.indices) {
                    val (unitName, _) = fragments[i]
                    val header = MainUpdatesHeaderBinding.inflate(inflater, mainUpdatesContainer, false)
                    val description = Unit.getDescription(unitName) ?: continue
                    mainUpdatesContainer.addView(header.root, fixedChildCount + i * 2)
                    header.mainUpdatesHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(description.getIcon(mContext), null, null, null)
                    header.mainUpdatesHeader.text = description.getName(mContext)
                    header.mainUpdatesHeader.setOnClickListener {
                        mainViewModel.displayUnit(unitName, shouldShowNavAfterBack = true)
                    }
                }
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadUpdates()
        } else {
            fragments.forEach { (_, f) ->
                f.onHiddenChanged(hidden)
            }
        }
    }

}

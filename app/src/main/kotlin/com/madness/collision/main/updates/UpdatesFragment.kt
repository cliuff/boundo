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

package com.madness.collision.main.updates

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isEmpty
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.FragmentUpdatesBinding
import com.madness.collision.databinding.MainUpdatesHeaderBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.*
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitDescViewModel
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var mode = MODE_NORMAL
    private val isNoUpdatesMode: Boolean
        get() = mode == MODE_NO_UPDATES
    private lateinit var viewBinding: FragmentUpdatesBinding
    private lateinit var inflater: LayoutInflater

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.main_updates)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        inflater = LayoutInflater.from(mContext)
        mode = arguments?.getInt(ARG_MODE) ?: MODE_NORMAL
        if (isNoUpdatesMode) return
        updatesProviders = DescRetriever(mContext).includePinState().doFilter()
                .retrieveInstalled().mapNotNull {
                    Unit.getUpdates(it.unitName)?.run { it.unitName to this }
                }.toMutableList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentUpdatesBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUpdates()

        democratize(mainViewModel)
        val extra = X.size(mContext, 5f, X.DP).roundToInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.mainUpdatesContainer.alterPadding(top = it + extra)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            viewBinding.mainUpdatesContainer.alterPadding(bottom = asBottomMargin(it + extra))
        }

        val descViewModel: UnitDescViewModel by activityViewModels()
        descViewModel.updated.observe(viewLifecycleOwner) {
            updateItem(it)
        }
    }

    override fun onDestroyView() {
        // returning from other bottom nav destination would restore added fragments
        // but fragment containers need to be restored manually
        // so remove the added fragments before the state is saved
        clearUpdates()
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        fragments.forEach { (_, f) ->
            f.onHiddenChanged(hidden)
        }
        if (!hidden) updateUpdates()
    }

    private fun retrieveUpdateFragments(): MutableList<Pair<String, Fragment>> {
        return updatesProviders.mapNotNull {
            if (it.second.hasUpdates(this)) it.second.fragment?.run {
                it.first to this
            } else null
        }.toMutableList()
    }

    /**
     * Remove all the updates, especially the fragments from fragment manager
     */
    private fun clearUpdates() {
        if (fragments.isEmpty()) return
        if (viewBinding.mainUpdatesUpdateContainer.isEmpty()) return
        for (i in fragments.indices.reversed()) {
            removeUpdateFragment(fragments[i], i)
        }
    }

    private fun loadUpdates() = lifecycleScope.launch(Dispatchers.Default) {
        fragments = retrieveUpdateFragments()
        if (fragments.isEmpty()) return@launch
        // Use launchWhenStarted to avoid IllegalArgumentException:
        // No view found for id 0x1 (unknown) for fragment MyUpdatesFragment
        lifecycleScope.launchWhenStarted {
            fragments.forEach {
                addUpdateFragment(it)
            }
        }
    }

    private fun addUpdateFragment(updateFragment: Pair<String, Fragment>, index: Int = -1) {
        val (unitName: String, fragment: Fragment) = updateFragment
        if (fragment.isAdded) return
        val container = addUpdateFragmentContainer(unitName, index) ?: return
        childFragmentManager.beginTransaction().add(container.id, fragment).commitNowAllowingStateLoss()
    }

    private fun addUpdateFragmentContainer(unitName: String, index: Int) : ViewGroup? {
        val container = getUpdateFragmentContainer(unitName) ?: return null
        viewBinding.mainUpdatesUpdateContainer.run {
            if (index < 0) addView(container) else addView(container, index)
        }
        return container
    }

    private fun getUpdateFragmentContainer(unitName: String) : ViewGroup? {
        val description = Unit.getDescription(unitName) ?: return null

        val container = LinearLayout(mContext).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        val header = MainUpdatesHeaderBinding.inflate(inflater, container, false)
        val icon = description.getIcon(mContext)
        header.mainUpdatesHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
        header.mainUpdatesHeader.text = description.getName(mContext)
        header.mainUpdatesHeader.setOnClickListener {
            mainViewModel.displayUnit(unitName, shouldShowNavAfterBack = true)
        }

        container.addView(header.root)
        return container
    }

    private fun removeUpdateFragment(updateFragment: Pair<String, Fragment>, index: Int) {
        if (index > viewBinding.mainUpdatesUpdateContainer.size - 1) return
        val (_, fragment: Fragment) = updateFragment
        childFragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
        viewBinding.mainUpdatesUpdateContainer.removeViewAt(index)
    }

    private fun updateUpdates() = lifecycleScope.launch(Dispatchers.Default) {
        val newFragments = retrieveUpdateFragments()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return fragments.size
            }

            override fun getNewListSize(): Int {
                return newFragments.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return fragments[oldItemPosition].first == newFragments[newItemPosition].first
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return fragments[oldItemPosition].first == newFragments[newItemPosition].first
            }

        }, false)
        withContext(Dispatchers.Main) {
            diff.dispatchUpdatesTo(object : ListUpdateCallback {

                override fun onInserted(position: Int, count: Int) {
                    for (i in position until position + count) {
                        addUpdateFragment(newFragments[i])
                    }
                }

                override fun onRemoved(position: Int, count: Int) {
                    for (i in (position until position + count).reversed()) {
                        removeUpdateFragment(fragments[i], i)
                    }
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                }

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                }
            })
            fragments = newFragments
        }
    }

    /**
     * List changes include addition and deletion but no update
     */
    private fun updateItem(stateful: StatefulDescription) {
        val isAddition = stateful.isPinned
        if (isAddition) {
            if (updatesProviders.any { it.first == stateful.unitName }) return
            val provider = Unit.getUpdates(stateful.unitName) ?: return
            updatesProviders.add(stateful.unitName to provider)
            if (!provider.hasUpdates(this)) return
            val fragment = provider.fragment ?: return
            val updateFragment = stateful.unitName to fragment
            fragments.add(updateFragment)
            addUpdateFragment(updateFragment)
        } else {
            for (i in updatesProviders.indices) {
                val provider = updatesProviders[i]
                if (provider.first != stateful.unitName) continue
                updatesProviders.removeAt(i)
                break
            }
            for (i in fragments.indices) {
                val fragment = fragments[i]
                if (fragment.first != stateful.unitName) continue
                fragments.removeAt(i)
                removeUpdateFragment(fragment, i)
                break
            }
        }
    }

}

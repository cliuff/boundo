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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.madness.collision.databinding.FragmentUpdatesBinding
import com.madness.collision.databinding.MainUpdatesHeaderBinding
import com.madness.collision.main.MainFragment
import com.madness.collision.main.MainPageViewModel
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.*
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitDescViewModel
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.ElapsingTime
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.controller.saveFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal class UpdatesFragment : TaggedFragment() {

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
    // used for assignment and thread-safe modification
    private var _updatesProviders: MutableList<Pair<String, UpdatesProvider>> = mutableListOf()
    private val updatesProviders: List<Pair<String, UpdatesProvider>>
        get() = _updatesProviders
    // used for assignment and thread-safe modification
    private var _fragments: MutableList<Pair<String, Fragment>> = mutableListOf()
    private val fragments: List<Pair<String, Fragment>>
        get() = _fragments
    private var mode = MODE_NORMAL
    private val isNoUpdatesMode: Boolean
        get() = mode == MODE_NO_UPDATES
    // last time view created
    private val elapsingViewCreated = ElapsingTime()
    private lateinit var viewBinding: FragmentUpdatesBinding
    private lateinit var inflater: LayoutInflater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeUpdates", "onCreate() hasState=${savedInstanceState != null}")
        mContext = context ?: return
        inflater = LayoutInflater.from(mContext)
        mode = arguments?.getInt(ARG_MODE) ?: MODE_NORMAL
        if (isNoUpdatesMode) return
        val descList = DescRetriever(mContext).includePinState().doFilter().retrieveInstalled()
        _updatesProviders = descList.mapNotNullTo(ArrayList(descList.size)) {
            Unit.getUpdates(it.unitName)?.run { it.unitName to this }
        }
        kotlin.run r@{
            val savedState = savedInstanceState ?: return@r
            val units = savedState.getStringArrayList("UFS").orEmpty()
            if (units.isEmpty()) return@r
            val fMan = childFragmentManager
            val fList = units.mapNotNullTo(ArrayList(units.size)) m@{ u ->
                fMan.getFragment(savedState, "UF-$u")?.let { u to it }
            }
            _fragments = fList
            Log.d("HomeUpdates", fList.joinToString(prefix = "Restored: ") { (u, f) -> "$u-$f" })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("HomeUpdates", "onCreateView() hasState=${savedInstanceState != null}")
        viewBinding = FragmentUpdatesBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeUpdates", "onViewCreated() hasState=${savedInstanceState != null}")
        fragments.forEach { restoreUpdateFragment(it) }

        updateUpdates()
        elapsingViewCreated.reset()

        val extra = X.size(mContext, 5f, X.DP).roundToInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.mainUpdatesContainer.alterPadding(top = it + extra)
        }
        val parent = parentFragment
        if (parent is MainFragment) {
            val mainPageViewModel: MainPageViewModel by parent.viewModels()
            mainPageViewModel.bottomContentWidth.observe(viewLifecycleOwner) {
                viewBinding.mainUpdatesContainer.alterPadding(bottom = asBottomMargin(it + extra))
            }
        }

        val descViewModel: UnitDescViewModel by activityViewModels()
        descViewModel.updated.observe(viewLifecycleOwner) {
            updateItem(it)
        }
    }

    override fun onResume() {
        super.onResume()
        // reload updates if it has been over 2 minutes
        // since the last time updates were loaded
        if (elapsingViewCreated.interval(30_000)) {
            updateUpdates()
        }
    }

    fun refreshUpdates() {
        updateUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("HomeUpdates", "onSaveInstanceState()")
        fragments.let s@{ fList ->
            if (fList.isEmpty()) return@s
            outState.putStringArrayList("UFS", ArrayList(fList.map { it.first }))
            fList.forEach { (unit, f) -> childFragmentManager.saveFragment(outState, "UF-$unit", f) }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        // returning from other bottom nav destination would restore added fragments
        // but fragment containers need to be restored manually
        // so remove the added fragments before the state is saved
//        clearUpdates()
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        fragments.forEach { (_, f) ->
            f.onHiddenChanged(hidden)
        }
        if (!hidden) updateUpdates()
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

    private fun restoreUpdateFragment(updateFragment: Pair<String, Fragment>) {
        val (unitName: String, fragment: Fragment) = updateFragment
        val container = addUpdateFragmentContainer(unitName, -1) ?: return
        val fMan = childFragmentManager
        if (fragment.isAdded) {
            // avoid IllegalStateException: Can't change container ID of fragment
            fMan.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
            fMan.executePendingTransactions()
        }
        fMan.beginTransaction().replace(container.id, fragment).commitNowAllowingStateLoss()
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
        header.mainUpdatesHeaderContainer.setOnClickListener {
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
        Log.d("HomeUpdates", "---------  Checking  -------------")
        val host = this@UpdatesFragment
        val oldFragments = fragments
        val oldFMap = oldFragments.toMap()
        val updatesInfo = updatesProviders.mapNotNull m@{ (unitName, provider) ->
            val newUpdate = provider.hasNewUpdate(host) ?: return@m null
            // query old list first in case fragments were restored
            val f = oldFMap[unitName] ?: provider.fragment ?: return@m null
            Triple(unitName, f, newUpdate)
        }
        val newFragments = updatesInfo.mapTo(ArrayList(updatesInfo.size)) { it.first to it.second }
        val logOld = oldFragments.joinToString { it.first }
        val logNew = updatesInfo.joinToString { it.first + "-${it.third}" }
        Log.d("HomeUpdates", "Applying u $logOld -> $logNew")
        if (oldFragments.isEmpty() || updatesInfo.isEmpty()) {
            // Use whenStarted() to avoid IllegalArgumentException:
            // No view found for id 0x1 (unknown) for fragment MyUpdatesFragment
            whenStarted {
                if (oldFragments.isEmpty()) {
                    if (updatesInfo.isNotEmpty()) {
                        updatesInfo.forEach {
                            addUpdateFragment(it.first to it.second)
                        }
                    }
                } else if (updatesInfo.isEmpty()) {
                    for (i in oldFragments.indices.reversed()) {
                        removeUpdateFragment(oldFragments[i], i)
                    }
                }
                _fragments = newFragments
            }
            return@launch
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldFragments.size
            }

            override fun getNewListSize(): Int {
                return updatesInfo.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldFragments[oldItemPosition].first == updatesInfo[newItemPosition].first
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return updatesInfo[newItemPosition].third.not()
            }

        }, false)
        withContext(Dispatchers.Main) {
            val diffFragments = oldFragments.toMutableList()
            diff.dispatchUpdatesTo(object : ListUpdateCallback {

                override fun onInserted(position: Int, count: Int) {
                    // Use launchWhenStarted to avoid IllegalArgumentException:
                    // No view found for id 0x1 (unknown) for fragment MyUpdatesFragment
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED).not()) return
                    val newPos = kotlin.run {
                        if (position <= 0) return@run 0
                        val oldItem = diffFragments[position - 1]
                        updatesInfo.indexOfFirst { it.first == oldItem.first } + 1
                    }
                    Log.d("HomeUpdates", "Diff insert: pos=$position, count=$count, newPos=$newPos")
                    for (offset in 0 until count) {
                        val index = position + offset
                        val f = updatesInfo[newPos + offset].run { first to second }
                        Log.d("HomeUpdates", "Diff insert: ${f.first} at $index")
                        addUpdateFragment(f, index)
                        diffFragments.add(index, f)
                    }
                }

                override fun onRemoved(position: Int, count: Int) {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED).not()) return
                    Log.d("HomeUpdates", "Diff remove: pos=$position, count=$count")
                    for (offset in (0 until count).reversed()) {
                        val index = position + offset
                        Log.d("HomeUpdates", "Diff remove: ${diffFragments[index].first} at $index")
                        removeUpdateFragment(diffFragments[index], index)
                        diffFragments.removeAt(index)
                    }
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    Log.d("HomeUpdates", "Diff move: from=$fromPosition, to=$toPosition")
                }

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED).not()) return
                    Log.d("HomeUpdates", "Diff change: pos=$position, count=$count")
                    for (offset in 0 until count) {
                        val index = position + offset
                        Log.d("HomeUpdates", "Diff change: ${diffFragments[index].first} at $index")
                        val u = diffFragments[index].second as? Updatable ?: continue
                        u.updateState()
                    }
                }
            })
            _fragments = newFragments
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
            synchronized(_updatesProviders) {
                _updatesProviders.add(stateful.unitName to provider)
            }
            if (!provider.hasUpdates(this)) return
            val fragment = provider.fragment ?: return
            val updateFragment = stateful.unitName to fragment
            synchronized(_fragments) {
                _fragments.add(updateFragment)
            }
            addUpdateFragment(updateFragment)
        } else {
            for (i in updatesProviders.indices) {
                val provider = updatesProviders[i]
                if (provider.first != stateful.unitName) continue
                synchronized(_updatesProviders) {
                    _updatesProviders.removeAt(i)
                }
                break
            }
            for (i in fragments.indices) {
                val fragment = fragments[i]
                if (fragment.first != stateful.unitName) continue
                synchronized(_fragments) {
                    _fragments.removeAt(i)
                }
                removeUpdateFragment(fragment, i)
                break
            }
        }
    }

}

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
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.madness.collision.databinding.FragmentUpdatesBinding
import com.madness.collision.databinding.MainUpdatesHeaderBinding
import com.madness.collision.main.MainFragment
import com.madness.collision.main.MainPageViewModel
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Description
import com.madness.collision.unit.StatefulDescription
import com.madness.collision.unit.UnitDescViewModel
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.ElapsingTime
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.dev.idString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

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

    interface Listener {
        fun onRefreshState(isRefreshing: Boolean)
    }

    override val category: String = "MainUpdates"
    override val id: String = "Updates"

    private val dID = "@" + idString.takeLast(2)
    private lateinit var mContext: Context
    private val updatesViewModel: UpdatesViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
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
    private val updateProviderMan = UpdateProviderMan()
    private val updateFragmentHost = object : UpdateFragmentHost {
        override val context: Context by ::mContext
        override val fragmentManager: FragmentManager by ::childFragmentManager
        override val updateContainerView: ViewGroup get() = viewBinding.mainUpdatesUpdateContainer
        override fun createUpdateHeader(desc: Description, parent: ViewGroup) = createUpdateHeaderView(desc, parent)
    }
    private val updateFragmentMan = UpdateFragmentMan(updateFragmentHost)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeUpdates", "$dID onCreate() hasState=${savedInstanceState != null}")
        mContext = context ?: return
        inflater = LayoutInflater.from(mContext)
        mode = arguments?.getInt(ARG_MODE) ?: MODE_NORMAL
        if (isNoUpdatesMode) return
        updateProviderMan.init(mContext)
        kotlin.run r@{
            val savedState = savedInstanceState ?: return@r
            val units = savedState.getStringArrayList("UFS").orEmpty()
            if (units.isEmpty()) return@r
            val fMan = childFragmentManager
            val fList = units.mapNotNullTo(ArrayList(units.size)) m@{ u ->
                fMan.getFragment(savedState, "UF-$u")?.let { u to it }
            }
            _fragments = fList
            Log.d("HomeUpdates", fList.joinToString(prefix = "$dID Restored: ") { (u, f) -> "$u-$f" })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("HomeUpdates", "$dID onCreateView() hasState=${savedInstanceState != null}")
        viewBinding = FragmentUpdatesBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeUpdates", "$dID onViewCreated() hasState=${savedInstanceState != null}")
        fragments.forEach { updateFragmentMan.restoreUpdateFragment(it) }

        updatesViewModel.uiStateFlow
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach(::resolveUiState)
            .launchIn(viewLifecycleOwner.lifecycleScope)

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
        Log.d("HomeUpdates", "$dID onSaveInstanceState()")
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
            updateFragmentMan.removeUpdateFragment(fragments[i], i)
        }
    }

    private fun createUpdateHeaderView(description: Description, parent: ViewGroup): View {
        val header = MainUpdatesHeaderBinding.inflate(inflater, parent, false)
        val icon = description.getIcon(mContext)
        header.mainUpdatesHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
        header.mainUpdatesHeader.text = description.getName(mContext)
        header.mainUpdatesHeaderContainer.setOnClickListener {
            mainViewModel.displayUnit(description.unitName, shouldShowNavAfterBack = true)
        }
        return header.root
    }

    private val updateCheckMutex = Mutex()

    private fun updateUpdates() {
        if (updatesViewModel.uiStateFlow.value == UpdatesUiState.CheckingUpdate) return
        lifecycleScope.launch(Dispatchers.Default) {
            updateCheckMutex.withLock {
                withTimeoutOrNull(2.minutes) {
                    updatesViewModel.updateUiState(UpdatesUiState.CheckingUpdate)
                    Log.d("HomeUpdates", "---------  $dID Checking  -------------")
                    val host = this@UpdatesFragment
                    val oldFragments = fragments
                    val updatesInfo = updateProviderMan.getUpdateInfo(host, oldFragments.toMap())
                    val state = UpdatesUiState.UpdateAvailable(updatesInfo, oldFragments)
                    updatesViewModel.updateUiState(state)
                }
            }
        }
    }

    private fun resolveUiState(state: UpdatesUiState) {
        (parentFragment as? Listener)?.run {
            onRefreshState(state == UpdatesUiState.CheckingUpdate)
            Log.d("HomeUpdates", "$dID/$state")
        }
        when (state) {
            UpdatesUiState.None -> kotlin.Unit
            UpdatesUiState.CheckingUpdate -> kotlin.Unit
            UpdatesUiState.NoUpdate ->
                resolveUiState(UpdatesUiState.UpdateAvailable(emptyList(), fragments))
            is UpdatesUiState.UpdateAvailable -> {
                val (updatesInfo, oldFragments) = state
                val newFragments = updatesInfo.mapTo(ArrayList(updatesInfo.size)) { it.unitName to it.fragment }
                val logOld = oldFragments.joinToString { it.first }
                val logNew = updatesInfo.joinToString { it.unitName + "-${it.hasNewUpdate}" }
                Log.d("HomeUpdates", "$dID Applying u $logOld -> $logNew")

                val diffHelper = UpdateDiffHelper(updateFragmentMan, oldFragments, updatesInfo)
                if (oldFragments.isEmpty() || updatesInfo.isEmpty()) {
                    diffHelper.applySimpleUpdate()
                } else {
                    val callback = diffHelper.getCallback(lifecycle, dID)
                    diffHelper.getDiffResult().dispatchUpdatesTo(callback)
                }
                _fragments = newFragments
            }
        }
    }

    /**
     * List changes include addition and deletion but no update
     */
    private fun updateItem(stateful: StatefulDescription) {
        updateProviderMan.updateItem(
            host = this,
            stateful = stateful,
            add = { fragment ->
                val updateFragment = stateful.unitName to fragment
                synchronized(_fragments) { _fragments.add(updateFragment) }
                updateFragmentMan.addUpdateFragment(updateFragment)
            },
            remove = {
                for (i in fragments.indices) {
                    val fragment = fragments[i]
                    if (fragment.first != stateful.unitName) continue
                    synchronized(_fragments) { _fragments.removeAt(i) }
                    updateFragmentMan.removeUpdateFragment(fragment, i)
                    break
                }
            }
        )
    }

}

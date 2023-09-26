/*
 * Copyright 2023 Clifford Liu
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

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.madness.collision.unit.Updatable

class UpdateDiffHelper(
    private val updateFragmentMan: UpdateFragmentMan,
    private val oldFragments: List<Pair<String, Fragment>>,
    private val updatesInfo: List<UnitUpdateInfo>,
) {
    fun applySimpleUpdate() {
        if (oldFragments.isEmpty()) {
            if (updatesInfo.isNotEmpty()) {
                updatesInfo.forEach {
                    updateFragmentMan.addUpdateFragment(it.unitName to it.fragment)
                }
            }
        } else if (updatesInfo.isEmpty()) {
            for (i in oldFragments.indices.reversed()) {
                updateFragmentMan.removeUpdateFragment(oldFragments[i], i)
            }
        }
    }

    fun getDiffResult() = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldFragments.size
        }

        override fun getNewListSize(): Int {
            return updatesInfo.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldFragments[oldItemPosition].first == updatesInfo[newItemPosition].unitName
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return updatesInfo[newItemPosition].hasNewUpdate.not()
        }

    }, false)

    fun getCallback(lifecycle: Lifecycle, dID: String) = object : ListUpdateCallback {
        private val diffFragments = oldFragments.toMutableList()

        override fun onInserted(position: Int, count: Int) {
            // Use launchWhenStarted to avoid IllegalArgumentException:
            // No view found for id 0x1 (unknown) for fragment MyUpdatesFragment
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED).not()) return
            val newPos = kotlin.run {
                if (position <= 0) return@run 0
                val oldItem = diffFragments[position - 1]
                updatesInfo.indexOfFirst { it.unitName == oldItem.first } + 1
            }
            Log.d("HomeUpdates", "$dID Diff insert: pos=$position, count=$count, newPos=$newPos")
            for (offset in 0 until count) {
                val index = position + offset
                val f = updatesInfo[newPos + offset].run { unitName to fragment }
                Log.d("HomeUpdates", "$dID Diff insert: ${f.first} at $index")
                updateFragmentMan.addUpdateFragment(f, index)
                diffFragments.add(index, f)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED).not()) return
            Log.d("HomeUpdates", "$dID Diff remove: pos=$position, count=$count")
            for (offset in (0 until count).reversed()) {
                val index = position + offset
                Log.d("HomeUpdates", "$dID Diff remove: ${diffFragments[index].first} at $index")
                updateFragmentMan.removeUpdateFragment(diffFragments[index], index)
                diffFragments.removeAt(index)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            Log.d("HomeUpdates", "$dID Diff move: from=$fromPosition, to=$toPosition")
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED).not()) return
            Log.d("HomeUpdates", "$dID Diff change: pos=$position, count=$count")
            for (offset in 0 until count) {
                val index = position + offset
                Log.d("HomeUpdates", "$dID Diff change: ${diffFragments[index].first} at $index")
                val u = diffFragments[index].second as? Updatable ?: continue
                u.updateState()
            }
        }
    }
}
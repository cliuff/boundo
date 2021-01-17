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
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.madness.collision.databinding.AdapterFrequentUnitsBinding
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.StatefulDescription
import com.madness.collision.util.sortedWithUtilsBy

internal class PinnedUnitsAdapter(context: Context, private val listener: Listener)
    : SandwichAdapter<PinnedUnitsAdapter.UnitsHolder>(context) {

    class UnitsHolder(binding: AdapterFrequentUnitsBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.frequentUnitsAdapterCard
        val name: AppCompatTextView = binding.frequentUnitsAdapterName as AppCompatTextView
        val icon: ImageView = binding.frequentUnitsAdapterIcon
    }

    interface Listener {
        val click: (StatefulDescription) -> Unit
        val longClick: (StatefulDescription) -> Boolean
    }

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private var descriptions = DescRetriever(mContext).includePinState().doFilter()
            .retrieveInstalled().dataSorted()

    override var spanCount: Int = 1
    override val listCount: Int
        get() = descriptions.size

    private fun List<StatefulDescription>.dataSorted(): MutableList<StatefulDescription> {
        return sortedWithUtilsBy { it.description.getName(context) }.toMutableList()
    }

    /**
     * List changes include addition and deletion but no update
     */
    fun updateItem(stateful: StatefulDescription) {
        val isAddition = stateful.isPinned
        if (isAddition) {
            if (descriptions.any { it.unitName == stateful.unitName }) return
            descriptions.add(stateful)
            descriptions = descriptions.dataSorted()
            for (i in descriptions.indices) {
                if (descriptions[i].unitName != stateful.unitName) continue
                notifyItemInserted(i + frontCount)
                break
            }
        } else {
            for (i in descriptions.indices) {
                if (descriptions[i].unitName != stateful.unitName) continue
                descriptions.removeAt(i)
                // when the only item is removed, adapter size changes to 0
                if (listCount == 0) notifyDataSetChanged()
                else notifyItemRemoved(i + frontCount)
                break
            }
        }
    }

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): UnitsHolder {
        return UnitsHolder(AdapterFrequentUnitsBinding.inflate(mInflater, parent, false))
    }

    override fun onMakeBody(holder: UnitsHolder, index: Int) {
        val stateful = descriptions[index]
        val description = stateful.description
        holder.name.text = description.getName(mContext)
        holder.icon.setImageDrawable(description.getIcon(mContext))
        holder.card.setOnClickListener {
            listener.click.invoke(stateful)
        }
        holder.card.setOnLongClickListener {
            listener.longClick.invoke(stateful)
        }
    }
}

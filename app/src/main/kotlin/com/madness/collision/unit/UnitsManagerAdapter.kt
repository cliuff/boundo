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

package com.madness.collision.unit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.madness.collision.R
import com.madness.collision.databinding.AdapterUnitsManagerBinding
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.sortedWithUtilsBy
import kotlin.Unit

internal class UnitsManagerAdapter(context: Context, private val listener: Listener)
    : SandwichAdapter<UnitsManagerAdapter.UnitViewHolder>(context) {

    class UnitViewHolder(binding: AdapterUnitsManagerBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.unitsManagerAdapterCard
        val name: AppCompatTextView = binding.unitsManagerAdapterName
        val status: ImageView = binding.unitManagerAdapterStatus
        val container: View = binding.unitManagerContainer
    }

    interface Listener {
        val click: (StatefulDescription) -> Unit
    }

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val descriptions = DescRetriever(mContext).includePinState()
            .retrieveAll().sortedWithUtilsBy { it.description.getName(context) }
    private val colorPass: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionPass) }

    override var spanCount: Int = 1
    override val listCount: Int = descriptions.size

    /**
     * Update is the only possible list change, no addition or deletion
     */
    fun updateItem(stateful: StatefulDescription) {
        for (i in descriptions.indices) {
            val desc = descriptions[i]
            if (desc.unitName != stateful.unitName) continue
            // update
            desc.updateState(stateful)
            notifyListItemChanged(i)
            break
        }
    }

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        return UnitViewHolder(AdapterUnitsManagerBinding.inflate(mInflater, parent, false))
    }

    override fun onMakeBody(holder: UnitViewHolder, index: Int) {
        val stateful = descriptions[index]
        val description = stateful.description
        optimizeSideMargin(index, 30f, 7f, holder.card)
        holder.name.text = description.getName(mContext)
        holder.name.setCompoundDrawablesRelativeWithIntrinsicBounds(description.getIcon(mContext), null, null, null)
        holder.container.setOnClickListener {
            listener.click.invoke(stateful)
        }
        if (stateful.isAvailable && stateful.isEnabled) {
            holder.status.visibility = View.VISIBLE
            holder.status.drawable.setTint(colorPass)
        } else {
            holder.status.visibility = View.GONE
        }
    }
}

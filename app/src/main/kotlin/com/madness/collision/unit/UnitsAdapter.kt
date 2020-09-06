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
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.madness.collision.databinding.AdapterUnitsBinding
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.StringUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class UnitsAdapter(context: Context, private val mainViewModel: MainViewModel)
    : SandwichAdapter<UnitsAdapter.UnitsHolder>(context) {

    class UnitsHolder(binding: AdapterUnitsBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.unitsAdapterCard
        val name: AppCompatTextView = binding.unitsAdapterName as AppCompatTextView
    }

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val mAvailableDescriptions = Unit.getInstalledUnits(mContext).mapNotNull {
        Unit.getDescription(it)
    }.filter { it.isAvailable(mContext) }.sortedWith { o1, o2 ->
        StringUtils.compareName(o1.getName(context), o2.getName(context))
    }

    override var spanCount: Int = 1
    override val listCount: Int = mAvailableDescriptions.size

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): UnitsHolder {
        return UnitsHolder(AdapterUnitsBinding.inflate(mInflater, parent, false))
    }

    override fun onMakeBody(holder: UnitsHolder, index: Int) {
        val description = mAvailableDescriptions[index]
        holder.name.text = description.getName(mContext)
        holder.name.setCompoundDrawablesRelativeWithIntrinsicBounds(description.getIcon(mContext), null, null, null)
        holder.card.setOnClickListener {
            mainViewModel.displayUnit(description.unitName, shouldShowNavAfterBack = true)
            GlobalScope.launch { Unit.increaseFrequency(mContext, description.unitName) }
        }
        holder.card.setOnLongClickListener {
            description.descriptionPage?.let { mainViewModel.displayFragment(it, shouldShowNavAfterBack = true) }
            true
        }
    }
}

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
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.madness.collision.databinding.AdapterFrequentUnitsBinding
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Unit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.*
import kotlin.Comparator

internal class PinnedUnitsAdapter(context: Context, private val mainViewModel: MainViewModel)
    : SandwichAdapter<PinnedUnitsAdapter.UnitsHolder>(context) {

    class UnitsHolder(binding: AdapterFrequentUnitsBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.frequentUnitsAdapterCard
        val name: AppCompatTextView = binding.frequentUnitsAdapterName as AppCompatTextView
        val icon: ImageView = binding.frequentUnitsAdapterIcon
    }

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val pinnedUnitsDescriptions = Unit.getPinnedUnits(mContext).mapNotNull {
        Unit.getDescription(it)
    }.filter { it.isAvailable(mContext) }.sortedWith(Comparator { o1, o2 -> Collator.getInstance(Locale.CHINESE).compare(o1.getName(context), o2.getName(context)) })

    override var spanCount: Int = 1
    override val listCount: Int = pinnedUnitsDescriptions.size

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): UnitsHolder {
        return UnitsHolder(AdapterFrequentUnitsBinding.inflate(mInflater, parent, false))
    }

    override fun onMakeBody(holder: UnitsHolder, index: Int) {
        val description = pinnedUnitsDescriptions[index]
        holder.name.text = description.getName(mContext)
        holder.icon.setImageDrawable(description.getIcon(mContext))
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

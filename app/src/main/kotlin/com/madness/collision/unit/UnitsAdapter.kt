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
    private val mAvailableDescriptions = Unit.getSortedUnitNamesByFrequency(mContext).mapNotNull {
        Unit.getDescription(it)
    }.filter { it.isAvailable(mContext) }

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

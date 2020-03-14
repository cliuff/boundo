package com.madness.collision.unit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.madness.collision.R
import com.madness.collision.databinding.AdapterUnitsManagerBinding
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.ThemeUtil

internal class UnitsManagerAdapter(context: Context, splitInstallManager: SplitInstallManager, private val mainViewModel: MainViewModel)
    : SandwichAdapter<UnitsManagerAdapter.UnitViewHolder>(context) {

    class UnitViewHolder(binding: AdapterUnitsManagerBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.unitsManagerAdapterCard
        val name: AppCompatTextView = binding.unitsManagerAdapterName
        val status: ImageView = binding.unitManagerAdapterStatus
        val container: View = binding.unitManagerContainer
    }

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val installedUnits = Unit.getInstalledUnits(splitInstallManager)
    private val mDescriptions: List<Description>
    init {
        val frequencies = Unit.getFrequencies(mContext)
        mDescriptions = frequencies.toList().plus(Unit.UNITS.filter { !frequencies.containsKey(it) }.map { it to 0 })
                .sortedByDescending { it.second }.mapNotNull { Unit.getDescription(it.first) }
    }
    private val colorPass: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionPass) }

    override var spanCount: Int = 1
    override val listCount: Int = mDescriptions.size

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        return UnitViewHolder(AdapterUnitsManagerBinding.inflate(mInflater, parent, false))
    }

    override fun onMakeBody(holder: UnitViewHolder, index: Int) {
        val description = mDescriptions[index]
        holder.name.text = description.getName(mContext)
        holder.name.setCompoundDrawablesRelativeWithIntrinsicBounds(description.getIcon(mContext), null, null, null)
        holder.container.setOnClickListener {
            description.descriptionPage?.let { mainViewModel.displayFragment(it) }
        }
        val isInstalled = installedUnits.contains(description.unitName)
        if (isInstalled) {
            holder.status.visibility = View.VISIBLE
            holder.status.drawable.setTint(colorPass)
        } else {
            holder.status.visibility = View.GONE
        }
    }
}

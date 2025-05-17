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

package com.madness.collision.settings.unitmgr

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.card.MaterialCardView
import com.madness.collision.R
import com.madness.collision.databinding.AdapterUnitsManagerBinding
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.StatefulDescription
import com.madness.collision.util.ColorUtil
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.mainApplication
import com.madness.collision.util.sortedWithUtilsBy

internal class UnitsManagerAdapter(context: Context, private val listener: Listener)
    : SandwichAdapter<UnitsManagerAdapter.UnitViewHolder>(context) {

    class UnitViewHolder(binding: AdapterUnitsManagerBinding): RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.unitsManagerAdapterCard
        val icon = binding.unitManIcon
        val name: AppCompatTextView = binding.unitsManagerAdapterName
        val status: ImageView = binding.unitManagerAdapterStatus
        val container: View = binding.unitManagerContainer
        val dynamic = binding.unitManDynamic
        val dynamicState = binding.unitManDynamicState
        val disabled = binding.unitManDisabled
    }

    interface Listener {
        val click: (StatefulDescription) -> Unit
    }

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val descriptions = DescRetriever(mContext).includePinState()
            .retrieveAll().sortedWithUtilsBy { it.description.getName(context) }
    private val colorPass: Int by lazy { ThemeUtil.getColor(context, R.attr.colorActionPass) }
    private val colorSubText: Int by lazy { ThemeUtil.getColor(context, R.attr.colorTextSub) }
    private val colorAlert: ColorStateList by lazy {
        val c = ThemeUtil.getColor(context, R.attr.colorActionAlert)
        ColorStateList.valueOf(c)
    }
    private val colorPinned: ColorStateList by lazy {
        val c = Color.parseColor("#A0FFC030")
        ColorStateList.valueOf(c)
    }
    private val colorPassStateList: ColorStateList by lazy {
        ColorStateList.valueOf(colorPass)
    }
    // red theme is neither pale nor dark, the best way is to select color for each respective theme
    private val cardColorDynamic = if (mainApplication.isPaleTheme) Color.parseColor("#FFFFF5F0")
    else ColorUtil.darkenAs(Color.parseColor("#FFFF7030"), if (mainApplication.isDarkTheme) 0.15f else 0.55f)
    private val cardColorStatic = ThemeUtil.getColor(context, R.attr.colorAItem)
    private val colorOnItem = ThemeUtil.getColor(context, R.attr.colorAOnItem)
    // 0..255
    private val cardAlphaComp: Int = context.resources.getInteger(R.integer.surfaceAlphaComp)

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
        holder.name.run {
            text = description.getName(mContext)
            setTextColor(if (stateful.isAvailable) colorOnItem else colorSubText)
            paintFlags = if (stateful.isUnavailable) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        holder.icon.setImageDrawable(description.getIcon(mContext))
        holder.icon.imageTintList = ColorStateList.valueOf(if (stateful.isAvailable) colorOnItem else colorSubText)
        val showDynamicState = stateful.isDynamic && description.isRemovable
        val opaqueCardColor = if (showDynamicState) cardColorDynamic else cardColorStatic
        // adjust card color alpha manually since card background color overrides view alpha
        val semitransparentCardColor = ColorUtils.setAlphaComponent(opaqueCardColor, cardAlphaComp)
        holder.card.setCardBackgroundColor(semitransparentCardColor)
        if (showDynamicState) {
            holder.dynamicState.setText(if (stateful.isInstalled) R.string.unit_desc_installed else R.string.unit_desc_not_installed)
            holder.dynamicState.setTextColor(if (stateful.isInstalled) colorPass else colorSubText)
        }
        holder.dynamic.isVisible = showDynamicState
        holder.dynamicState.isVisible = showDynamicState
        val showDisableMsg = stateful.isDisabled && stateful.isInstalled && stateful.isAvailable
        holder.disabled.isVisible = showDisableMsg
        holder.container.setOnClickListener {
            listener.click.invoke(stateful)
        }
        when {
            // installed, available and pinned
            stateful.isPinned -> {
                holder.status.load(R.drawable.ic_star_24)
                holder.status.imageTintList = colorPinned
                holder.status.visibility = View.VISIBLE
            }
            // disabled but installed and available
            showDisableMsg -> {
                holder.status.load(R.drawable.ic_block_24)
                holder.status.imageTintList = colorAlert
                holder.status.visibility = View.VISIBLE
            }
            // not installed and available
            stateful.isUninstalled && stateful.isAvailable -> {
                holder.status.load(R.drawable.ic_download_24)
                holder.status.imageTintList = colorPassStateList
                holder.status.visibility = View.VISIBLE
            }
            else -> holder.status.visibility = View.GONE
        }
    }
}

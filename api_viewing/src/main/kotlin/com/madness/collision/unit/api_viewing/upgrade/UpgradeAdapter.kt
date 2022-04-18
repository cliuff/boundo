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

package com.madness.collision.unit.api_viewing.upgrade

import android.content.Context
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AvUpdUpgItemBinding
import com.madness.collision.unit.api_viewing.list.APIAdapter
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.util.X
import kotlinx.coroutines.CoroutineScope

internal class UpgradeAdapter(context: Context, listener: Listener, scope: CoroutineScope)
    : APIAdapter(context, listener, scope) {
    override val shouldShowTime: Boolean = true
    var upgrades: List<Upgrade> = emptyList()
        set(value) {
            field = value
            apps = upgrades.map { it.new }
        }
    @Suppress("unchecked_cast")
    override var appList: List<*>
        get() = upgrades
        set(value) {
            upgrades = if (value.isNotEmpty() && value[0] is Upgrade) value as List<Upgrade> else emptyList()
        }

    class Holder(binding: AvUpdUpgItemBinding) : APIAdapter.Holder(binding.root) {
        val preVer: AppCompatTextView = binding.avUpdUpgItemPreVer as AppCompatTextView
        val preTime: AppCompatTextView = binding.avUpdUpgItemPreTime as AppCompatTextView
        val preApi: AppCompatTextView = binding.avUpdUpgItemPreApi as AppCompatTextView
        val preSeal: ImageView = binding.avUpdUpgItemPreSeal
        val preBack: View = binding.avUpdUpgItemPreBack
        val newVer: AppCompatTextView = binding.avUpdUpgItemNewVer as AppCompatTextView
        init {
            logo = binding.avUpdUpgItemNewIcon
            name = binding.avUpdUpgItemNewName as AppCompatTextView
            updateTime = binding.avUpdUpgItemNewTime as AppCompatTextView
            tags = binding.avUpdUpgItemNewTags
            api = binding.avUpdUpgItemNewApi as AppCompatTextView
            seal = binding.avUpdUpgItemNewSeal
            card = binding.avUpdUpgItemNewCard
        }
    }

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): APIAdapter.Holder {
        val binding = AvUpdUpgItemBinding.inflate(inflater, parent, false)
        return Holder(binding)
    }

    override fun onMakeBody(holder: APIAdapter.Holder, index: Int) {
        if (holder !is Holder) return
        super.onMakeBody(holder, index)
        val upgrade = upgrades[index]
        holder.newVer.text = upgrade.versionName.second

        val verInfo = VerInfo(upgrade.targetApi.first)
        holder.preApi.text = verInfo.displaySdk

        SealManager.populate4Seal(context, verInfo.letter, itemLength)
        if (loadPref.shouldShowDesserts) {
            holder.preApi.setTextColor(SealManager.getItemColorAccent(context, verInfo.api))
            val seal: Bitmap? = SealManager.seals[verInfo.letter]
            if (seal == null) {
                holder.preSeal.visibility = View.GONE
            } else {
                holder.preSeal.visibility = View.VISIBLE
                holder.preSeal.setImageBitmap(X.toMin(seal, itemLength))
            }
            val itemBack = SealManager.getItemColorBack(context, verInfo.api)
            holder.preBack.setBackgroundColor(itemBack)
        } else {
            holder.preApi.setTextColor(holder.name.textColors)
            holder.preSeal.visibility = View.GONE
            holder.preBack.setBackgroundColor(colorSurface)
        }

        holder.preVer.text = upgrade.versionName.first
        val updateTime = DateUtils.getRelativeTimeSpanString(upgrade.updateTime.first,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        holder.preTime.text = updateTime
    }
}
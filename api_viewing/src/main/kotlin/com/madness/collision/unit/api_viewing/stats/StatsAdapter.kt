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

package com.madness.collision.unit.api_viewing.stats

import android.content.Context
import android.graphics.Bitmap
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.madness.collision.diy.SandwichAdapter
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AdapterAvStatsBinding
import com.madness.collision.unit.api_viewing.seal.SealManager
import com.madness.collision.util.X
import com.madness.collision.util.adapted
import com.madness.collision.util.alterMargin
import com.madness.collision.util.dartFuture
import com.madness.collision.util.os.OsUtils
import kotlin.math.roundToInt

internal class StatsAdapter(context: Context) : SandwichAdapter<StatsAdapter.Holder>(context) {

    class Holder(binding: AdapterAvStatsBinding) : RecyclerView.ViewHolder(binding.root) {
        val card: MaterialCardView = binding.avStatsAdapterCard
        val logoBack: ImageView = binding.avStatsAdapterLogoBack
        val logoText: AppCompatTextView = binding.avStatsAdapterLogoText as AppCompatTextView
        val version: AppCompatTextView = binding.avStatsAdapterVersion as AppCompatTextView
        val codeName: AppCompatTextView = binding.avStatsAdapterName as AppCompatTextView
        val count: AppCompatTextView = binding.avStatsAdapterCount as AppCompatTextView
        val seal: ImageView = binding.avStatsAdapterSeal

        constructor(binding: AdapterAvStatsBinding, sweetMargin: Int): this(binding) {
//            mViews.avStatsAdapterCard.cardElevation = sweetElevation
//            mViews.avStatsAdapterCard.stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.res_lift_card_on_touch)
            card.alterMargin(top = sweetMargin, bottom = sweetMargin)
        }
    }

    var stats: SparseIntArray = SparseIntArray(0)
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    override var spanCount: Int = 1
        set(value) {
            if (value > 0) field = value
        }
    override val listCount: Int
        get() = stats.size()
    val indexOffset: Int
        get() = spanCount

    private val isSweet = EasyAccess.isSweet
    private val shouldShowDesserts = isSweet
//    private val sweetElevation = if (shouldShowDesserts) X.size(context, 1f, X.DP) else 0f
    private val sweetMargin = if (shouldShowDesserts) X.size(context, 5f, X.DP).roundToInt() else 0
    private val innerMargin: Float
        get() = if (EasyAccess.shouldShowDesserts) 5f else 2f
    private val itemLength: Int = X.size(context, 45f, X.DP).roundToInt()

    private val inflater = LayoutInflater.from(context)

    override fun onCreateBodyItemViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = AdapterAvStatsBinding.inflate(inflater, parent, false)
        return if (shouldShowDesserts) Holder(binding, sweetMargin) else Holder(binding)
    }

    override fun onMakeBody(holder: Holder, index: Int) {
        val statsCount = stats.valueAt(index)
        val verInfo = VerInfo(stats.keyAt(index), true)

        optimizeSideMargin(index, 15f, innerMargin, holder.card)

        val ver = when {
            verInfo.api == OsUtils.DEV -> "Developer Preview"
            verInfo.sdk.isEmpty() -> "API ${verInfo.api}"
            else -> verInfo.sdk
        }
        holder.version.dartFuture("Android $ver")
        holder.count.dartFuture(statsCount.adapted)

        // display a dot when API bigger than 99 (longer than 2 digits)
        val logoText = if (verInfo.api > 99) "•" else verInfo.apiText
        holder.logoText.dartFuture(logoText)
        disposeAPIInfo(verInfo, holder.logoText, holder.logoBack)

        SealManager.populate4Seal(context, verInfo.letter, itemLength)
        if (shouldShowDesserts){
            holder.count.setTextColor(SealManager.getItemColorAccent(context, verInfo.api))
            val seal: Bitmap? = SealManager.seals[verInfo.letter]
            if (seal == null) {
                holder.seal.visibility = View.GONE
            } else {
                holder.seal.visibility = View.VISIBLE
                holder.seal.setImageBitmap(X.toMin(seal, itemLength))
            }
            val itemBack = SealManager.getItemColorBack(context, verInfo.api)
            holder.card.setCardBackgroundColor(itemBack)
        }

        if (isSweet && verInfo.codeName(context) != verInfo.sdk) {
            holder.codeName.dartFuture(verInfo.codeName(context))
            holder.codeName.visibility = View.VISIBLE
        } else {
            holder.codeName.visibility = View.GONE
        }
    }

    private fun disposeAPIInfo(ver: VerInfo, logoText: TextView, logoBack: ImageView) {
        logoText.text = ver.sdk
        if (EasyAccess.isSweet){
            val colorText = SealManager.getItemColorText(ver.api)
            logoText.setTextColor(colorText)
        }

        val bitmap: Bitmap = SealManager.disposeSealBack(context, ver.letter, itemLength)
        logoBack.setImageBitmap(X.circularBitmap(bitmap))
    }
}

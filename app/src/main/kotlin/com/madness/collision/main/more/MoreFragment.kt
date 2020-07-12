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

package com.madness.collision.main.more

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.google.android.material.card.MaterialCardView
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.ImmortalActivity
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.*

class MoreFragment : TaggedFragment(), Democratic, View.OnClickListener, NavNode {

    override val category: String = "More"
    override val id: String = "More"
    
    companion object {
        @JvmStatic
        fun newInstance() = MoreFragment()
    }

    override val nodeDestinationId: Int = R.id.moreFragment
    private val viewModel: MainViewModel by activityViewModels()
    private val exterior = mainApplication.exterior
    private var exteriorTransparency: Int = 0xFF

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.app_name)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        var res = R.layout.fragment_more
        if (context != null) {
            SettingsFunc.updateLanguage(context)
            val dp480 = X.size(context, 500f, X.DP)
            val dimension = X.getCurrentAppResolution(context)
            if (context.spanJustMore){
                val occupiedWidth = viewModel.sideNavWidth
                if (dimension.x - occupiedWidth >= dp480) res = R.layout.fragment_more_lm
            } else if (dimension.x >= dp480) res = R.layout.fragment_more_lm
        }
        return inflater.inflate(res, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mViews = view ?: return
        initData()
        bindData(mViews)
    }

    private fun initData(){
        if (exterior) exteriorTransparency = resources.getInteger(R.integer.exteriorTransparency)
        democratize(viewModel)
    }

    private fun bindData(mViews: View){
        viewModel.contentWidthTop.observe(viewLifecycleOwner){
            mViews.findViewById<LinearLayout>(R.id.moreContainer)?.alterPadding(top = it)
        }
        viewModel.contentWidthBottom.observe(viewLifecycleOwner){
            mViews.findViewById<LinearLayout>(R.id.moreContainer)?.alterPadding(bottom = it)
        }
        val cardInstant = mViews.findViewById<MaterialCardView>(R.id.moreInstant)
        val isInstantUnavailable = X.belowOff(X.M)
        if (isInstantUnavailable) cardInstant.visibility = View.GONE
        val cardSettings = mViews.findViewById<MaterialCardView>(R.id.moreSettings)
        val cardUnitManager = mViews.findViewById<MaterialCardView>(R.id.moreUnitsManager)
        prepareCards(if (isInstantUnavailable) null else cardInstant, cardSettings, cardUnitManager)
        cardSettings.setOnLongClickListener {
            Intent(context, ImmortalActivity::class.java).run {
                putExtra(P.IMMORTAL_EXTRA_LAUNCH_MODE, P.IMMORTAL_EXTRA_LAUNCH_MODE_MORTAL)
                startActivity(this)
            }
            true
        }
    }

    private fun prepareCards(vararg cards: CardView?){
        for (it in cards) {
            it ?: continue
            it.setOnClickListener(this)
            if (exterior) {
                val c = it.cardBackgroundColor
                it.setCardBackgroundColor(c.withAlpha(exteriorTransparency))
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.moreInstant -> MoreFragmentDirections.actionMoreFragmentToInstantFragment()
            R.id.moreUnitsManager -> MoreFragmentDirections.actionMoreFragmentToUnitsManagerFragment()
            R.id.moreSettings -> MoreFragmentDirections.actionMoreFragmentToSettingsFragment()
            else -> null
        }?.let { navigate(it) }
    }
}

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

package com.madness.collision.main.more

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.ImmortalActivity
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.*
import com.madness.collision.util.AppUtils.asBottomMargin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
        toolbar[0].isVisible = true
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        var res = R.layout.fragment_more
        if (context != null) {
            SettingsFunc.updateLanguage(context)
            val dp480 = X.size(context, 500f, X.DP)
            val dimension = SystemUtil.getRuntimeWindowSize(context)
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

        // run test
        val context = context ?: return
        if (TestPlayground.hasTest) TestPlayground.start(context)
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
            mViews.findViewById<LinearLayout>(R.id.moreContainer)?.alterPadding(bottom = asBottomMargin(it))
        }
        val cardInstant = mViews.findViewById<MaterialCardView>(R.id.moreInstant)
        val cardSettings = mViews.findViewById<MaterialCardView>(R.id.moreSettings)
        val cardUnitManager = mViews.findViewById<MaterialCardView>(R.id.moreUnitsManager)
        prepareCards(cardInstant, cardSettings, cardUnitManager)
        cardSettings.setOnLongClickListener {
            val context = context ?: return@setOnLongClickListener true
            showDevOptions(context)
            true
        }
    }

    private val devOptions: List<Pair<String, (Context) -> Unit>> = listOf(
        "Immortal" to { context ->
            val intent = Intent(context, ImmortalActivity::class.java).apply {
                putExtra(P.IMMORTAL_EXTRA_LAUNCH_MODE, P.IMMORTAL_EXTRA_LAUNCH_MODE_MORTAL)
            }
            startActivity(intent)
        },
        "Display info" to { context ->
            lifecycleScope.launch(Dispatchers.Default) {
                val displayInfo = DisplayInfo.getDisplaysAndInfo(context)
                withContext(Dispatchers.Main) {
                    showInfoDialog(context, displayInfo)
                }
            }
        },
        "App Room info" to { context ->
            lifecycleScope.launch(Dispatchers.Default) {
                val roomInfo = AccessAV.getRoomInfo(context)
                withContext(Dispatchers.Main) {
                    showInfoDialog(context, roomInfo)
                }
            }
        },
        "Clean App Room" to { context ->
            lifecycleScope.launch(Dispatchers.Default) {
                AccessAV.clearRoom(context)
                withContext(Dispatchers.Main) {
                    notifyBriefly(R.string.text_done)
                }
            }
        },
        "Nuke App Room" to { context ->
            lifecycleScope.launch(Dispatchers.Default) {
                val re = AccessAV.nukeAppRoom(context)
                withContext(Dispatchers.Main) {
                    notifyBriefly(if (re) R.string.text_done else R.string.text_error)
                }
            }
        },
    )

    private fun showDevOptions(context: Context) {
        val customContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val pop = CollisionDialog(context, R.string.text_OK).apply {
            setContent(0)
            setTitleCollision(0, 0, 0)
            setCustomContentMere(customContent)
            setListener { dismiss() }
        }
        val marginHor = X.size(context, 20f, X.DP).roundToInt()
        val marginVer = X.size(context, 4f, X.DP).roundToInt()
        val btnTint = ThemeUtil.getColor(context, R.attr.colorAItem).let { ColorStateList.valueOf(it) }
        val btnTextColor = ThemeUtil.getColor(context, R.attr.colorAOnItem).let { ColorStateList.valueOf(it) }
        devOptions.forEach { (title, click) ->
            val btn = MaterialButton(context).apply {
                text = title
                setTextColor(btnTextColor)
                backgroundTintList = btnTint
                isAllCaps = false
                setOnClickListener {
                    pop.dismiss()
                    click(context)
                }
            }
            customContent.addView(btn)
            btn.alterMargin(start = marginHor, top = marginVer, end = marginHor, bottom = marginVer)
        }
        customContent.alterPadding(top = marginHor)
        pop.decentHeight()
        pop.show()
    }

    private fun showInfoDialog(context: Context, content: CharSequence) {
        val view = TextView(context).apply {
            text = content
            textSize = 8f
            val padding = X.size(context, 6f, X.DP).roundToInt()
            alterPadding(start = padding, top = padding * 3, end = padding)
        }
        CollisionDialog(context, R.string.text_OK).apply {
            setContent(0)
            setTitleCollision(0, 0, 0)
            setCustomContent(view)
            decentHeight()
            setListener { dismiss() }
        }.show()
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

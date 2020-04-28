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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.MainUpdatesHeaderBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import com.madness.collision.util.ensureAdded
import kotlinx.android.synthetic.main.fragment_updates.*
import kotlin.math.roundToInt

internal class UpdatesFragment : Fragment(), Democratic {

    private lateinit var mContext: Context
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var updatesProviders: List<Pair<String, UpdatesProvider>>
    private lateinit var fragments: List<Pair<String, Fragment>>

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.main_updates)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
        updatesProviders = Unit.getPinnedUnits(mContext).mapNotNull {
            Unit.getUpdates(it)?.run { it to this }
        }
        fragments = updatesProviders.mapNotNull {
            it.second.getFragment()?.run { it.first to this }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_updates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (fragments.isEmpty()) {
            mainUpdatesSecUpdates.visibility = View.GONE
        }
        val inflater = LayoutInflater.from(context)
        for ((unitName, f) in fragments) {
            val header = MainUpdatesHeaderBinding.inflate(inflater, mainUpdatesContainer, true)
            ensureAdded(R.id.mainUpdatesContainer, f)
            val description = Unit.getDescription(unitName) ?: continue
            header.mainUpdatesHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(description.getIcon(mContext), null, null, null)
            header.mainUpdatesHeader.text = description.getName(mContext)
            header.mainUpdatesHeader.setOnClickListener {
                mainViewModel.displayUnit(unitName, shouldShowNavAfterBack = true)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize(mainViewModel)
        val extra = X.size(mContext, 5f, X.DP).roundToInt()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            mainUpdatesContainer.alterPadding(top = it + extra)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            mainUpdatesContainer.alterPadding(bottom = it + extra)
        }
    }

}

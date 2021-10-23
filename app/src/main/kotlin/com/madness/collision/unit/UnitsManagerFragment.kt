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

package com.madness.collision.unit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.FragmentUnitsManagerBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.TaggedFragment

internal class UnitsManagerFragment : TaggedFragment(), Democratic {

    override val category: String = "UnitManager"
    override val id: String = "UnitManager"

    companion object {
        @JvmStatic
        fun newInstance(): UnitsManagerFragment {
            return UnitsManagerFragment()
        }
    }

    private lateinit var mViews: FragmentUnitsManagerBinding
    private lateinit var mRecyclerView: RecyclerView
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.unitsManager)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        mViews = FragmentUnitsManagerBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context: Context = context ?: return
        democratize(mainViewModel)
        val descViewModel: UnitDescViewModel by activityViewModels()

        mRecyclerView = mViews.unitsManagerRecyclerView

        val mAdapter = UnitsManagerAdapter(context, object : UnitsManagerAdapter.Listener {
            override val click: (StatefulDescription) -> kotlin.Unit = {
                it.description.descriptionPage?.run {
                    mainViewModel.displayFragment(this)
                }
            }
        })
        mAdapter.resolveSpanCount(this, 400f)
        mRecyclerView.layoutManager = mAdapter.suggestLayoutManager()
        mRecyclerView.adapter = mAdapter

        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            mAdapter.topCover = it
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            mAdapter.bottomCover = asBottomMargin(it)
        }
        descViewModel.updated.observe(viewLifecycleOwner) {
            mAdapter.updateItem(it)
        }
    }

}

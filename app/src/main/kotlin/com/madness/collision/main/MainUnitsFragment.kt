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

package com.madness.collision.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.madness.collision.databinding.FragmentMainUnitsBinding
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.alterPadding

internal class MainUnitsFragment : TaggedFragment() {
    override val category: String = "MainUnits"
    override val id: String = "MainUnits"

    companion object {

        @JvmStatic
        fun newInstance() : MainUnitsFragment {
            return MainUnitsFragment()
        }
    }

    private lateinit var mViews: FragmentMainUnitsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mViews = FragmentMainUnitsBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainViewModel: MainViewModel by activityViewModels()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            mViews.mainUnitsContainer.alterPadding(top = it)
        }
        val parent = parentFragment
        if (parent is MainFragment) {
            val mainPageViewModel: MainPageViewModel by parent.viewModels()
            mainPageViewModel.bottomContentWidth.observe(viewLifecycleOwner) {
                mViews.mainUnitsContainer.alterPadding(bottom = asBottomMargin(it))
            }
        }
    }
}

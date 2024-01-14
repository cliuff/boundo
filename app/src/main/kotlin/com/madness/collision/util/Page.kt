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

package com.madness.collision.util

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.FragmentPageBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment

inline fun <reified T: Fragment> Page(titleId: Int = 0, democratic: Democratic? = null): Page {
    val f = try {
        T::class.java.getDeclaredConstructor().newInstance()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
    return Page(f, titleId, democratic)
}

/**
 * Wrap a fragment
 */
class Page(fragment: Fragment? = null, private var titleId: Int = 0, private val democratic: Democratic? = null) : TaggedFragment(), Democratic {
    companion object {
        const val STATE_KEY_FRA = "MyFragment"
    }

    override val category: String = "Page"
    override val id: String = "Page"

    private val mainViewModel: MainViewModel by activityViewModels()
    private var mFragment: Fragment? = null
    init {
        if (fragment != null) {
            mFragment = fragment
        }
    }
    private lateinit var viewBinding: FragmentPageBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        if (democratic != null) {
            return democratic.createOptions(context, toolbar, iconColor)
        }
        mainViewModel.configNavigation(toolbar, iconColor)
        if (titleId != 0) {
            toolbar.setTitle(titleId)
        }
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        democratic ?: return false
        return democratic.selectOption(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // nav args
        val args = arguments ?: return
        mFragment = childFragmentManager.getSavedFragment(savedInstanceState, STATE_KEY_FRA)
        if (mFragment == null) {
            args.getString("fragmentClass")?.let {
                mFragment = try {
                    Class.forName(it).declaredConstructors[0].newInstance() as Fragment
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }
            }
        }
        titleId = args.getInt("titleId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentPageBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val f = mFragment ?: return
        ensureAdded(R.id.pageContainer, f)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.pageContainer.alterPadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            viewBinding.pageContainer.alterPadding(bottom = asBottomMargin(it))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mFragment?.let {
            childFragmentManager.saveFragment(outState, STATE_KEY_FRA, it)
        }
        super.onSaveInstanceState(outState)
    }

}

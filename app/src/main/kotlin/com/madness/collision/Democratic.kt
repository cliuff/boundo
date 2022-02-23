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

package com.madness.collision

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.madness.collision.main.MainViewModel

/**
 * Used for fragments to implement their own tool bar interface.
 *
 * &nbsp;
 *
 * This is the equivalent of [Activity.onCreateOptionsMenu][android.app.Activity.onCreateOptionsMenu]
 * and [Activity.onOptionsItemSelected][android.app.Activity.onOptionsItemSelected].
 */
interface Democratic{
    fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean{
        return true
    }

    fun selectOption(item: MenuItem): Boolean {
        return false
    }

    fun tintOptionIcons(toolbar: Toolbar, iconColor: Int) {
        val menu =  toolbar.menu
        for (i in 0 until menu.size()) menu.getItem(i).icon?.setTint(iconColor)
    }

    fun inflateAndTint(menuResId: Int, toolbar: Toolbar, iconColor: Int) {
        toolbar.inflateMenu(menuResId)
        tintOptionIcons(toolbar, iconColor)
    }

    fun configNavigation(toolbar: Toolbar, iconColor: Int, mainViewModel: MainViewModel) {
        val context = toolbar.context
        ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_24)?.let {
            it.setTint(iconColor)
            toolbar.navigationIcon = it
        }
        toolbar.setNavigationOnClickListener { mainViewModel.popUpBackStack() }
    }

    fun MainViewModel.configNavigation(toolbar: Toolbar, iconColor: Int) {
        configNavigation(toolbar, iconColor, this)
    }

    fun democratize(mainViewModel: MainViewModel) {
        mainViewModel.democratize(this)
    }
}
